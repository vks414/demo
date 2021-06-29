package org.sap.commercemigration.strategy.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.MapUtils;
import org.sap.commercemigration.concurrent.DataPipe;
import org.sap.commercemigration.concurrent.DataWorkerExecutor;
import org.sap.commercemigration.concurrent.DataWorkerPoolFactory;
import org.sap.commercemigration.concurrent.MaybeFinished;
import org.sap.commercemigration.concurrent.RetriableTask;
import org.sap.commercemigration.concurrent.impl.DefaultDataWorkerExecutor;
import org.sap.commercemigration.constants.CommercemigrationConstants;
import org.sap.commercemigration.context.CopyContext;
import org.sap.commercemigration.context.MigrationContext;
import org.sap.commercemigration.dataset.DataSet;
import org.sap.commercemigration.performance.PerformanceCategory;
import org.sap.commercemigration.performance.PerformanceRecorder;
import org.sap.commercemigration.performance.PerformanceUnit;
import org.sap.commercemigration.service.DatabaseCopyTaskRepository;
import org.sap.commercemigration.service.DatabaseMigrationDataTypeMapperService;
import org.sap.commercemigration.strategy.PipeWriterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopyOptions;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;


public class CopyPipeWriterStrategy implements PipeWriterStrategy<DataSet>
{
	private static final Logger LOG = LoggerFactory.getLogger(CopyPipeWriterStrategy.class);

	private final DatabaseMigrationDataTypeMapperService databaseMigrationDataTypeMapperService;

	private final DatabaseCopyTaskRepository taskRepository;

	private final DataWorkerPoolFactory dataWriteWorkerPoolFactory;

	public CopyPipeWriterStrategy(final DatabaseMigrationDataTypeMapperService databaseMigrationDataTypeMapperService,
			final DatabaseCopyTaskRepository taskRepository, final DataWorkerPoolFactory dataWriteWorkerPoolFactory)
	{
		this.databaseMigrationDataTypeMapperService = databaseMigrationDataTypeMapperService;
		this.taskRepository = taskRepository;
		this.dataWriteWorkerPoolFactory = dataWriteWorkerPoolFactory;
	}

	@Override
	public void write(final CopyContext context, final DataPipe<DataSet> pipe, final CopyContext.DataCopyItem item)
			throws Exception
	{
		final String targetTableName = item.getTargetItem();
		final PerformanceRecorder performanceRecorder = context.getPerformanceProfiler()
				.createRecorder(PerformanceCategory.DB_WRITE, targetTableName);
		performanceRecorder.start();
		final Set<String> excludedColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (context.getMigrationContext().getExcludedColumns().containsKey(targetTableName))
		{
			excludedColumns.addAll(context.getMigrationContext().getExcludedColumns().get(targetTableName));
			LOG.info("Ignoring excluded column(s): {}", excludedColumns);
		}
		final Set<String> nullifyColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (context.getMigrationContext().getNullifyColumns().containsKey(targetTableName))
		{
			nullifyColumns.addAll(context.getMigrationContext().getNullifyColumns().get(targetTableName));
			LOG.info("Nullify column(s): {}", nullifyColumns);
		}

		final List<String> columnsToCopy = new ArrayList<>();
		try (Connection sourceConnection = context.getMigrationContext().getDataSourceRepository().getConnection();
				Statement stmt = sourceConnection.createStatement();
				ResultSet metaResult = stmt.executeQuery(String.format("select * from %s where 0 = 1", item.getSourceItem()));)
		{
			final ResultSetMetaData sourceMeta = metaResult.getMetaData();
			final int columnCount = sourceMeta.getColumnCount();
			for (int i = 1; i <= columnCount; i++)
			{
				final String column = sourceMeta.getColumnName(i);
				if (!excludedColumns.contains(column))
				{
					columnsToCopy.add(column);
				}
			}
		}

		if (columnsToCopy.isEmpty())
		{
			throw new IllegalStateException(
					String.format("%s: source has no columns or all columns excluded", item.getPipelineName()));
		}
		final ThreadPoolTaskExecutor taskExecutor = dataWriteWorkerPoolFactory.create(context);
		final DataWorkerExecutor<Boolean> workerExecutor = new DefaultDataWorkerExecutor<>(taskExecutor);
		Connection targetConnection = null;
		final AtomicLong totalCount = new AtomicLong(0);
		Optional<String> upsertId = Optional.empty();
		try
		{
			targetConnection = context.getMigrationContext().getDataTargetRepository().getConnection();
			final boolean requiresIdentityInsert = requiresIdentityInsert(item.getTargetItem(), targetConnection);
			MaybeFinished<DataSet> sourcePage;
			boolean firstPage = true;
			do
			{
				sourcePage = pipe.get();
				if (sourcePage.isPoison())
				{
					throw new IllegalStateException("Poison received; dying. Check the logs for further insights.");
				}
				final DataSet dataSet = sourcePage.getValue();
				if (firstPage)
				{
					doTruncateIfNecessary(context, item.getTargetItem());
					doTurnOnOffIndicesIfNecessary(context, item.getTargetItem(), false);
					if (context.getMigrationContext().isIncrementalModeEnabled())
					{
						upsertId = determineUpsertId(dataSet);
					}
					firstPage = false;
				}
				if (dataSet.isNotEmpty())
				{
					final DataWriterContext dataWriterContext = new DataWriterContext(context, item, dataSet, columnsToCopy,
							nullifyColumns, performanceRecorder, totalCount, upsertId, requiresIdentityInsert);
					final RetriableTask writerTask = createWriterTask(dataWriterContext);
					workerExecutor.safelyExecute(writerTask);
				}
			}
			while (!sourcePage.isDone());
			workerExecutor.waitAndRethrowUncaughtExceptions();
			if (taskExecutor != null)
			{
				taskExecutor.shutdown();
			}
		}
		catch (final Exception e)
		{
			pipe.requestAbort(e);
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			throw e;
		}
		finally
		{
			if (targetConnection != null)
			{
				doTurnOnOffIndicesIfNecessary(context, item.getTargetItem(), true);
				targetConnection.close();
			}
			updateProgress(context, item, totalCount.get());
		}
	}

	private void switchIdentityInsert(final Connection connection, final String tableName, final boolean on)
	{
		try (Statement stmt = connection.createStatement())
		{
			final String onOff = on ? "ON" : "OFF";
			stmt.executeUpdate(String.format("SET IDENTITY_INSERT %s %s", tableName, onOff));
		}
		catch (final Exception e)
		{
			//TODO using brute force FIX
		}
	}

	protected void executeBatch(final CopyContext.DataCopyItem item, final PreparedStatement preparedStatement,
			final long batchCount, final PerformanceRecorder recorder) throws SQLException
	{
		final Stopwatch timer = Stopwatch.createStarted();
		preparedStatement.executeBatch();
		preparedStatement.clearBatch();
		LOG.debug("Batch written ({} items) for table '{}' in {}", batchCount, item.getTargetItem(), timer.stop().toString());
		recorder.record(PerformanceUnit.ROWS, batchCount);
	}

	private void updateProgress(final CopyContext context, final CopyContext.DataCopyItem item, final long totalCount)
	{
		try
		{
			taskRepository.updateTaskProgress(context, item, totalCount);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not update progress", e);
		}
	}

	protected void doTruncateIfNecessary(final CopyContext context, final String targetTableName) throws Exception
	{
		if (context.getMigrationContext().isTruncateEnabled())
		{
			if (!context.getMigrationContext().getTruncateExcludedTables().contains(targetTableName))
			{
				assertTruncateAllowed(context, targetTableName);
				context.getMigrationContext().getDataTargetRepository().truncateTable(targetTableName);
			}
		}
	}

	protected void doTurnOnOffIndicesIfNecessary(final CopyContext context, final String targetTableName, final boolean on)
			throws Exception
	{
		if (context.getMigrationContext().isDropAllIndexesEnabled())
		{
			if (!on)
			{
				LOG.debug("{} indexes for table '{}'", "Dropping", targetTableName);
				context.getMigrationContext().getDataTargetRepository().dropIndexesOfTable(targetTableName);
			}
		}
		else
		{
			if (context.getMigrationContext().isDisableAllIndexesEnabled())
			{
				if (!context.getMigrationContext().getDisableAllIndexesIncludedTables().isEmpty())
				{
					if (!context.getMigrationContext().getDisableAllIndexesIncludedTables().contains(targetTableName))
					{
						return;
					}
				}
				LOG.debug("{} indexes for table '{}'", on ? "Rebuilding" : "Disabling", targetTableName);
				if (on)
				{
					context.getMigrationContext().getDataTargetRepository().enableIndexesOfTable(targetTableName);
				}
				else
				{
					context.getMigrationContext().getDataTargetRepository().disableIndexesOfTable(targetTableName);
				}
			}
		}
	}

	protected void assertTruncateAllowed(final CopyContext context, final String targetTableName) throws Exception
	{
		if (context.getMigrationContext().isIncrementalModeEnabled())
		{
			throw new IllegalStateException("Truncating tables in incremental mode is illegal. Change the property "
					+ CommercemigrationConstants.MIGRATION_DATA_TRUNCATE_ENABLED + " to false");
		}
	}

	protected boolean isColumnOverride(final CopyContext context, final CopyContext.DataCopyItem item,
			final String sourceColumnName)
	{
		return MapUtils.isNotEmpty(item.getColumnMap()) && item.getColumnMap().containsKey(sourceColumnName);
	}

	protected boolean isColumnOverride(final CopyContext context, final CopyContext.DataCopyItem item)
	{
		return MapUtils.isNotEmpty(item.getColumnMap());
	}

	private PreparedStatement createPreparedStatement(final CopyContext context, final String targetTableName,
			final List<String> columnsToCopy, final Optional<String> upsertId, final Connection targetConnection) throws Exception
	{
		if (context.getMigrationContext().isIncrementalModeEnabled())
		{
			if (upsertId.isPresent())
			{
				return targetConnection.prepareStatement(getBulkUpsertStatement(targetTableName, columnsToCopy, upsertId.get()));
			}
			else
			{
				throw new RuntimeException(
						"The incremental approach can only be used on tables that have a valid identifier like PK or ID");
			}
		}
		else
		{
			return targetConnection.prepareStatement(getBulkInsertStatement(targetTableName, columnsToCopy,
					columnsToCopy.stream().map(column -> "?").collect(Collectors.toList())));
		}
	}

	private String getBulkInsertStatement(final String targetTableName, final List<String> columnsToCopy,
			final List<String> columnsToCopyValues)
	{
		return "INSERT INTO " + targetTableName + " " + getBulkInsertStatementParamList(columnsToCopy, columnsToCopyValues);
	}

	private String getBulkInsertStatementParamList(final List<String> columnsToCopy, final List<String> columnsToCopyValues)
	{
		return "(" + String.join(", ", columnsToCopy) + ") VALUES ("
				+ columnsToCopyValues.stream().collect(Collectors.joining(", ")) + ")";
	}

	private String getBulkUpdateStatementParamList(final List<String> columnsToCopy, final List<String> columnsToCopyValues)
	{
		return "SET " + IntStream.range(0, columnsToCopy.size())
				.mapToObj(idx -> String.format("%s = %s", columnsToCopy.get(idx), columnsToCopyValues.get(idx)))
				.collect(Collectors.joining(", "));
	}

	private Optional<String> determineUpsertId(final DataSet dataSet)
	{
		if (dataSet.hasColumn("PK"))
		{
			return Optional.of("PK");
		}
		else if (dataSet.hasColumn("ID"))
		{
			return Optional.of("ID");
		}
		else
		{
			//should we support more IDs? In the hybris context there is hardly any other with regards to transactional data.
			return Optional.empty();
		}
	}

	private String getBulkUpsertStatement(final String targetTableName, final List<String> columnsToCopy, final String columnId)
	{
		/*
		 * https://michaeljswart.com/2017/07/sql-server-upsert-patterns-and-antipatterns/ We are not using a stored
		 * procedure here as CCv2 does not grant sp exec permission to the default db user
		 */
		final StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(String.format("MERGE %s WITH (HOLDLOCK) AS t", targetTableName));
		sqlBuilder.append("\n");
		sqlBuilder.append(String.format("USING (SELECT %s) AS s ON t.%s = s.%s",
				Joiner.on(',').join(columnsToCopy.stream().map(column -> "? " + column).collect(Collectors.toList())), columnId,
				columnId));
		sqlBuilder.append("\n");
		sqlBuilder.append("WHEN MATCHED THEN UPDATE"); //update
		sqlBuilder.append("\n");
		sqlBuilder.append(getBulkUpdateStatementParamList(columnsToCopy,
				columnsToCopy.stream().map(column -> "s." + column).collect(Collectors.toList())));
		sqlBuilder.append("\n");
		sqlBuilder.append("WHEN NOT MATCHED THEN INSERT"); //insert
		sqlBuilder.append("\n");
		sqlBuilder.append(getBulkInsertStatementParamList(columnsToCopy,
				columnsToCopy.stream().map(column -> "s." + column).collect(Collectors.toList())));
		sqlBuilder.append(";");
		return sqlBuilder.toString();
	}

	private boolean requiresIdentityInsert(final String targetTableName, final Connection targetConnection)
	{
		final StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT \n");
		sqlBuilder.append("count(*)\n");
		sqlBuilder.append("FROM sys.columns\n");
		sqlBuilder.append("WHERE\n");
		sqlBuilder.append(String.format("object_id = object_id('%s')\n", targetTableName));
		sqlBuilder.append("AND\n");
		sqlBuilder.append("is_identity = 1\n");
		sqlBuilder.append(";\n");
		try
		{
			final Statement statement = targetConnection.createStatement();
			final ResultSet resultSet = statement.executeQuery(sqlBuilder.toString());
			boolean requiresIdentityInsert = false;
			if (resultSet.next())
			{
				requiresIdentityInsert = resultSet.getInt(1) > 0;
			}
			resultSet.close();
			return requiresIdentityInsert;
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private RetriableTask createWriterTask(final DataWriterContext dwc)
	{
		final MigrationContext ctx = dwc.getContext().getMigrationContext();
		if (!ctx.isBulkCopyEnabled())
		{
			return new DataWriterTask(dwc);
		}
		else
		{
			final boolean noNullification = dwc.getNullifyColumns().isEmpty();
			final boolean noIncremental = !ctx.isIncrementalModeEnabled();
			final boolean noColumnOverride = !isColumnOverride(dwc.getContext(), dwc.getCopyItem());
			if (noNullification && noIncremental && noColumnOverride)
			{
				LOG.warn("EXPERIMENTAL: Using bulk copy for {}", dwc.getCopyItem().getTargetItem());
				return new DataBulkWriterTask(dwc);
			}
			else
			{
				return new DataWriterTask(dwc);
			}
		}
	}

	private static class DataWriterContext
	{
		private final CopyContext context;
		private final CopyContext.DataCopyItem copyItem;
		private final DataSet dataSet;
		private final List<String> columnsToCopy;
		private final Set<String> nullifyColumns;
		private final PerformanceRecorder performanceRecorder;
		private final AtomicLong totalCount;
		private final Optional<String> upsertId;
		private final boolean requiresIdentityInsert;

		public DataWriterContext(final CopyContext context, final CopyContext.DataCopyItem copyItem, final DataSet dataSet,
				final List<String> columnsToCopy, final Set<String> nullifyColumns, final PerformanceRecorder performanceRecorder,
				final AtomicLong totalCount, final Optional<String> upsertId, final boolean requiresIdentityInsert)
		{
			this.context = context;
			this.copyItem = copyItem;
			this.dataSet = dataSet;
			this.columnsToCopy = columnsToCopy;
			this.nullifyColumns = nullifyColumns;
			this.performanceRecorder = performanceRecorder;
			this.totalCount = totalCount;
			this.upsertId = upsertId;
			this.requiresIdentityInsert = requiresIdentityInsert;
		}

		public CopyContext getContext()
		{
			return context;
		}

		public CopyContext.DataCopyItem getCopyItem()
		{
			return copyItem;
		}

		public DataSet getDataSet()
		{
			return dataSet;
		}

		public List<String> getColumnsToCopy()
		{
			return columnsToCopy;
		}

		public Set<String> getNullifyColumns()
		{
			return nullifyColumns;
		}

		public PerformanceRecorder getPerformanceRecorder()
		{
			return performanceRecorder;
		}

		public AtomicLong getTotalCount()
		{
			return totalCount;
		}

		public Optional<String> getUpsertId()
		{
			return upsertId;
		}

		public boolean isRequiresIdentityInsert()
		{
			return requiresIdentityInsert;
		}
	}

	private class DataWriterTask extends RetriableTask
	{

		private final DataWriterContext ctx;

		public DataWriterTask(final DataWriterContext ctx)
		{
			super(ctx.getContext(), ctx.getCopyItem().getTargetItem());
			this.ctx = ctx;
		}

		@Override
		protected Boolean internalRun()
		{
			try
			{
				if (!ctx.getDataSet().getAllResults().isEmpty())
				{
					process();
				}
				return Boolean.TRUE;
			}
			catch (final Exception e)
			{
				//LOG.error("Error while executing table task " + ctx.getCopyItem().getTargetItem(),e);
				throw new RuntimeException("Error processing writer task for " + ctx.getCopyItem().getTargetItem(), e);
			}
		}

		private void process() throws Exception
		{
			Connection connection = null;
			Boolean originalAutoCommit = null;
			final boolean requiresIdentityInsert = ctx.isRequiresIdentityInsert();
			try
			{
				connection = ctx.getContext().getMigrationContext().getDataTargetRepository().getConnection();
				originalAutoCommit = connection.getAutoCommit();
				try (PreparedStatement bulkWriterStatement = createPreparedStatement(ctx.getContext(),
						ctx.getCopyItem().getTargetItem(), ctx.getColumnsToCopy(), ctx.getUpsertId(), connection);
						Statement tempStmt = connection.createStatement();
						ResultSet tempTargetRs = tempStmt
								.executeQuery(String.format("select * from %s where 0 = 1", ctx.getCopyItem().getTargetItem())))
				{
					connection.setAutoCommit(false);
					if (requiresIdentityInsert)
					{
						switchIdentityInsert(connection, ctx.getCopyItem().getTargetItem(), true);
					}
					for (final List<Object> row : ctx.getDataSet().getAllResults())
					{
						int paramIdx = 1;
						for (final String sourceColumnName : ctx.getColumnsToCopy())
						{
							final int targetColumnIdx = tempTargetRs.findColumn(sourceColumnName);
							final int targetColumnType = tempTargetRs.getMetaData().getColumnType(targetColumnIdx);
							if (ctx.getNullifyColumns().contains(sourceColumnName))
							{
								bulkWriterStatement.setNull(paramIdx, targetColumnType);
								LOG.trace("Column {} is nullified. Setting NULL value...", sourceColumnName);
							}
							else
							{
								if (isColumnOverride(ctx.getContext(), ctx.getCopyItem(), sourceColumnName))
								{
									bulkWriterStatement.setObject(paramIdx, ctx.getCopyItem().getColumnMap().get(sourceColumnName),
											targetColumnType);
								}
								else
								{
									final Object sourceColumnValue = ctx.getDataSet().getColumnValue(sourceColumnName, row);
									if (sourceColumnValue != null)
									{
										bulkWriterStatement.setObject(paramIdx, sourceColumnValue, targetColumnType);
									}
									else
									{
										bulkWriterStatement.setNull(paramIdx, targetColumnType);
									}
								}
							}
							paramIdx += 1;
						}
						bulkWriterStatement.addBatch();
					}
					final int batchCount = ctx.getDataSet().getAllResults().size();
					executeBatch(ctx.getCopyItem(), bulkWriterStatement, batchCount, ctx.getPerformanceRecorder());
					bulkWriterStatement.clearParameters();
					bulkWriterStatement.clearBatch();
					connection.commit();
					final long totalCount = ctx.getTotalCount().addAndGet(batchCount);
					updateProgress(ctx.getContext(), ctx.getCopyItem(), totalCount);
				}
			}
			catch (final Exception e)
			{
				if (connection != null)
				{
					connection.rollback();
				}
				throw e;
			}
			finally
			{
				if (connection != null && originalAutoCommit != null)
				{
					connection.setAutoCommit(originalAutoCommit);
				}
				if (connection != null && ctx != null)
				{
					if (requiresIdentityInsert)
					{
						switchIdentityInsert(connection, ctx.getCopyItem().getTargetItem(), false);
					}
					connection.close();
				}
			}
		}
	}

	private class DataBulkWriterTask extends RetriableTask
	{

		private final DataWriterContext ctx;

		public DataBulkWriterTask(final DataWriterContext ctx)
		{
			super(ctx.getContext(), ctx.getCopyItem().getTargetItem());
			this.ctx = ctx;
		}

		@Override
		protected Boolean internalRun()
		{
			try
			{
				if (!ctx.getDataSet().getAllResults().isEmpty())
				{
					process();
				}
				return Boolean.TRUE;
			}
			catch (final Exception e)
			{
				//LOG.error("Error while executing table task " + ctx.getCopyItem().getTargetItem(),e);
				throw new RuntimeException("Error processing writer task for " + ctx.getCopyItem().getTargetItem(), e);
			}
		}

		private void process() throws Exception
		{
			Connection connection = null;
			Boolean originalAutoCommit = null;
			try
			{
				connection = ctx.getContext().getMigrationContext().getDataTargetRepository().getConnection();
				originalAutoCommit = connection.getAutoCommit();
				connection.setAutoCommit(false);
				final SQLServerBulkCopy bulkCopy = new SQLServerBulkCopy(connection.unwrap(SQLServerConnection.class));
				final SQLServerBulkCopyOptions copyOptions = new SQLServerBulkCopyOptions();
				copyOptions.setBulkCopyTimeout(0);
				copyOptions.setBatchSize(ctx.getContext().getMigrationContext().getReaderBatchSize());
				bulkCopy.setBulkCopyOptions(copyOptions);
				bulkCopy.setDestinationTableName(ctx.getCopyItem().getTargetItem());

				try (Statement tempStmt = connection.createStatement();
						ResultSet tempTargetRs = tempStmt
								.executeQuery(String.format("select * from %s where 0 = 1", ctx.getCopyItem().getTargetItem())))
				{
					for (final String column : ctx.getColumnsToCopy())
					{
						final int targetColumnIdx = tempTargetRs.findColumn(column);
						bulkCopy.addColumnMapping(column, targetColumnIdx);
					}
				}
				bulkCopy.writeToServer(ctx.getDataSet().toSQLServerBulkData());
				connection.commit();
				final Stopwatch timer = Stopwatch.createStarted();
				final int bulkCount = ctx.getDataSet().getAllResults().size();
				LOG.debug("Bulk written ({} items) for table '{}' in {}", bulkCount, ctx.getCopyItem().getTargetItem(),
						timer.stop().toString());
				ctx.getPerformanceRecorder().record(PerformanceUnit.ROWS, bulkCount);
				final long totalCount = ctx.getTotalCount().addAndGet(bulkCount);
				updateProgress(ctx.getContext(), ctx.getCopyItem(), totalCount);
			}
			catch (final Exception e)
			{
				if (connection != null)
				{
					connection.rollback();
				}
				throw e;
			}
			finally
			{
				if (connection != null && originalAutoCommit != null)
				{
					connection.setAutoCommit(originalAutoCommit);
				}
				if (connection != null && ctx != null)
				{
					connection.close();
				}
			}
		}
	}

}
