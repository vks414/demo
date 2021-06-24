package com.dcx.demo.slot.job;


import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.storelocator.model.PointOfServiceModel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import com.dcx.demo.service.DeliverySlotService;
import com.dcx.demo.slot.model.DeliverySlotModel;
import com.dcx.demo.slot.model.DeliverySlotTimingModel;
import com.dcx.demo.slot.model.SlotCronJobModel;


public class DeliverySlotCreationCronJob extends AbstractJobPerformable<SlotCronJobModel>
{

	private static final Logger LOG = Logger.getLogger(DeliverySlotCreationCronJob.class);

	@Resource(name = "flexibleSearchService")
	private FlexibleSearchService flexibleSearchService;

	@Resource(name = "deliverySlotService")
	private DeliverySlotService deliverySlotService;

	@Override
	public PerformResult perform(final SlotCronJobModel deliverySlotCronJobModel)
	{
		final BaseStoreModel currentBaseStore = deliverySlotCronJobModel.getStore();
		final Date jobStartDate = getFormattedDate(deliverySlotCronJobModel.getFromDate());
		final Date jobEndDate = getFormattedDate(deliverySlotCronJobModel.getToDate());
		final List<DeliverySlotModel> removableDeliverySlots = deliverySlotService.getDeliverySlotsBetweenDates(jobStartDate,
				jobEndDate);
		final Collection<PointOfServiceModel> stores = getAllPos(currentBaseStore);
		if (!removableDeliverySlots.isEmpty())
		{
			modelService.removeAll(removableDeliverySlots);
		}
		final List<Date> dateList = getDaysBetweenDates(jobStartDate, jobEndDate);
		for (final PointOfServiceModel store : stores)
		{
			final List<DeliverySlotTimingModel> slotTimingsList = store.getSlotTimings();
			for (final Date date : dateList)
			{
				for (final DeliverySlotTimingModel dfgDeliverySlotTimingsModel : slotTimingsList)
				{
					final DeliverySlotModel deliverySlotModel = modelService.create(DeliverySlotModel.class);
					deliverySlotModel.setCode(dfgDeliverySlotTimingsModel.getCode() + "-" + store.getPk().toString());
					deliverySlotModel.setDate(getFormattedDate(date));

					final Optional<DeliverySlotModel> existingDeliverySlot = deliverySlotService
							.getExistingDeliverySlots(deliverySlotModel, store);
					if (!existingDeliverySlot.isPresent())
					{
						deliverySlotModel.setFromTime(dfgDeliverySlotTimingsModel.getFromTime());
						deliverySlotModel.setToTime(dfgDeliverySlotTimingsModel.getToTime());
						deliverySlotModel.setRemainingCapacity(dfgDeliverySlotTimingsModel.getSlotCapacity());
						deliverySlotModel.setStore(store);
						modelService.save(deliverySlotModel);
					}
				}
			}
		}

		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	private List<Date> getDaysBetweenDates(final Date startdate, final Date enddate)
	{
		final List<Date> dates = new ArrayList<Date>();
		final Calendar calendar = new GregorianCalendar();
		calendar.setTime(startdate);

		while (calendar.getTime().before(enddate))
		{
			final Date result = calendar.getTime();
			dates.add(result);
			calendar.add(Calendar.DATE, 1);
		}
		return dates;
	}

	private Date getFormattedDate(final Date givenDate)
	{
		Date formattedDate = null;
		final DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		try
		{
			formattedDate = formatter.parse(formatter.format(givenDate));
		}
		catch (final ParseException parseException)
		{
			LOG.error("Unexpexted error occured" + parseException);

		}
		return formattedDate;
	}

	private List<Date> getFormattedDateList(final List<Date> givenDateList)
	{
		final List<Date> formattedDateList = new ArrayList<Date>();
		final DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		try
		{
			for (Date givenDate : givenDateList)
			{
				givenDate = formatter.parse(formatter.format(givenDate));
				formattedDateList.add(givenDate);
			}
		}
		catch (final ParseException parseException)
		{
			LOG.error("Unexpexted error occured" + parseException);
		}
		return formattedDateList;
	}

	public Collection<PointOfServiceModel> getAllPos(final BaseStoreModel baseStore)
	{
		final FlexibleSearchQuery fQuery = new FlexibleSearchQuery(
				"SELECT {PK} FROM {PointOfService} where {baseStore}=?basestore");
		fQuery.addQueryParameter("basestore", baseStore);
		final SearchResult<PointOfServiceModel> result = flexibleSearchService.search(fQuery);
		return result.getResult();
	}

}
