/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.service.impl;

import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.storelocator.model.PointOfServiceModel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcx.demo.service.DeliverySlotService;
import com.dcx.demo.slot.model.DeliverySlotModel;


public class DefaultDeliverySlotService implements DeliverySlotService
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultDeliverySlotService.class);

	@Resource(name = "flexibleSearchService")
	private FlexibleSearchService flexibleSearchService;

	private static final String DELIVERY_LIST_QUERY = "select {ds.pk} from {DeliverySlot as ds} where "
			+ "{ds.date} >= ?todaysDate and {ds.date} <= ?lastDateOfMonth and {ds.store} =?currentStore ORDER BY {ds.date} ASC, {ds.code} ASC";

	private static final String DELIVERY_SLOT_QUERY = "select {ds.pk} from {DeliverySlot as ds} where " + "{ds.pk} = ?pk";


	private static final String EXISTING_DELIVERY_SLOT_LIST = "select {ds.pk} from {DeliverySlot as ds} where "
			+ "{ds.date} =?date and {ds.Code} =?slotCode and {ds.store} =?currentStore";

	private static final String DELIVERY_SLOT_BETWEEN_DATES = "select {ds.pk} from {DeliverySlot as ds} where "
			+ "{ds.date} >=?startdate and {ds.date} <= ?enddate";

	private static final String DELIVERY_SLOT_FOR_DATE = "select {ds.pk} from {DeliverySlot as ds} where "
			+ "{ds.date} =?currentdate and {ds.store} =?currentStore";

	@Override
	public List<DeliverySlotModel> getDeliverySlotsForDate(final PointOfServiceModel store, final Date date)
	{
		final int cutoffTime = store.getPickupOffset();//1PM
		final int deliveryLeadTime = store.getPickupLeadTime();
		final int holidayCount = 0;
		final int noOfDaysToShowDeliverySlot = 5;
		final List<Date> holidayList = getFormattedDateList(store.getSlotHolidaysList());
		Date actualStartDate = null;
		final Calendar calender = Calendar.getInstance();
		calender.setTime(date); // Use Given Date
		final int deliveryOffset = getPickupOffsetDays(cutoffTime);
		int totalDays = deliveryLeadTime + deliveryOffset;
		while (totalDays > 0)
		{
			final Date currentDay = getFormattedDate(calender.getTime());
			if (!isCurrentDayHoliday(holidayList, currentDay))
			{
				totalDays--;
			}
			calender.add(Calendar.DATE, 1);
		}
		actualStartDate = getFormattedDate(calender.getTime());
		final String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(actualStartDate);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(DELIVERY_SLOT_FOR_DATE);
		query.addQueryParameter("currentdate", currentDate);
		query.addQueryParameter("currentStore", store);
		final SearchResult<DeliverySlotModel> searchResult = flexibleSearchService.search(query);
		return searchResult.getResult();

	}

	@Override
	public List<DeliverySlotModel> getAvailableDeliverySlots(final PointOfServiceModel store)

	{
		final int cutoffTime = store.getPickupOffset();//1PM
		final int deliveryLeadTime = store.getPickupLeadTime();
		int holidayCount = 0;
		final int noOfDaysToShowDeliverySlot = 5;
		final List<Date> holidayList = getFormattedDateList(store.getSlotHolidaysList());
		Date actualStartDate = null, endDate = null, actualEndDate = null;
		final Calendar calender = Calendar.getInstance();
		calender.setTime(new Date()); // Now use today date.
		final int deliveryOffset = getPickupOffsetDays(cutoffTime);
		int totalDays = deliveryLeadTime + deliveryOffset;
		while (totalDays > 0)
		{
			final Date currentDay = getFormattedDate(calender.getTime());
			if (!isCurrentDayHoliday(holidayList, currentDay))
			{
				totalDays--;
			}
			calender.add(Calendar.DATE, 1);
		}
		actualStartDate = getFormattedDate(calender.getTime());
		calender.setTime(actualStartDate);
		calender.add(Calendar.DATE, noOfDaysToShowDeliverySlot);
		endDate = getFormattedDate(calender.getTime());
		holidayCount = getHolidaysCount(holidayList, actualStartDate, endDate);
		calender.add(Calendar.DATE, holidayCount);
		actualEndDate = getFormattedDate(calender.getTime());
		final List<Date> dateList = getDaysBetweenDates(actualStartDate, actualEndDate, holidayList);
		final List<DeliverySlotModel> filteredDeliverySlotList = new ArrayList<DeliverySlotModel>();
		final String actualStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(actualStartDate);
		final String actualEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(actualEndDate);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(DELIVERY_LIST_QUERY);
		query.addQueryParameter("todaysDate", actualStart);
		query.addQueryParameter("lastDateOfMonth", actualEnd);
		query.addQueryParameter("currentStore", store);
		final SearchResult<DeliverySlotModel> searchResult = flexibleSearchService.search(query);
		if (!searchResult.getResult().stream().anyMatch(e -> e.getRemainingCapacity() > 0))
		{
			//
		}
		for (final Date date : dateList)
		{
			for (final DeliverySlotModel deliverySlotModel : searchResult.getResult())
			{
				if (deliverySlotModel.getDate().compareTo(date) == 0)
				{
					filteredDeliverySlotList.add(deliverySlotModel);
				}
			}
		}

		return filteredDeliverySlotList;

	}

	@Override
	public List<DeliverySlotModel> getDeliverySlotsBetweenDates(final Date startDate, final Date endDate)
	{
		final String actualStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startDate);
		final String actualEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endDate);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(DELIVERY_SLOT_BETWEEN_DATES);
		query.addQueryParameter("startdate", actualStart);
		query.addQueryParameter("enddate", actualEnd);
		final SearchResult<DeliverySlotModel> searchResult = flexibleSearchService.search(query);
		return searchResult.getResult();

	}

	@Override
	public List<DeliverySlotModel> getDeliverySlotsBeforeToday()
	{
		// XXX Auto-generated method stub
		return null;

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
			LOG.error("Unexpexted error ocurred" + parseException);
		}
		return formattedDate;
	}

	private int getPickupOffsetDays(final int cutoffTime)
	{
		int deliveryOffset = 0;
		String hourCutOff, minCutOff;
		final Calendar cal = Calendar.getInstance();
		final Date currentDate = cal.getTime();
		final String cutOff = String.valueOf(cutoffTime);

		if (!(cutOff.length() < 4))
		{
			hourCutOff = cutOff.substring(0, cutOff.length() / 2);
			minCutOff = cutOff.substring(cutOff.length() / 2);
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourCutOff));
			cal.set(Calendar.MINUTE, Integer.parseInt(minCutOff));
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
		}

		if (currentDate.after(cal.getTime()))
		{
			deliveryOffset = 1;
		}
		return deliveryOffset;
	}

	private int getHolidaysCount(final List<Date> holidayList, final Date actualStartDate, final Date actualEndDate)
	{
		final Calendar calendar = new GregorianCalendar();
		int holidayCount = 0;
		calendar.setTime(actualStartDate);
		while (calendar.getTime().before(actualEndDate) || calendar.getTime().compareTo(actualEndDate) == 0)
		{
			final Date result = calendar.getTime();
			if (isCurrentDayHoliday(holidayList, result))
			{
				holidayCount++;
			}
			calendar.add(Calendar.DATE, 1);
		}
		return holidayCount;
	}

	private Boolean isCurrentDayHoliday(final List<Date> holidayList, Date currentDate)
	{
		Boolean isHoliday = false;
		currentDate = getFormattedDate(currentDate);
		final List<Date> listOfDates = new ArrayList<Date>(holidayList);
		Collections.sort(listOfDates);
		if (listOfDates.contains(currentDate))
		{
			isHoliday = Boolean.valueOf(true);
		}
		else
		{
			isHoliday = Boolean.valueOf(false);
		}
		return isHoliday;
	}

	private List<Date> getDaysBetweenDates(final Date startdate, final Date enddate, final List<Date> holidayList)
	{
		final List<Date> dates = new ArrayList<Date>();
		final Calendar calendar = new GregorianCalendar();
		calendar.setTime(startdate);
		while (calendar.getTime().before(enddate))
		{
			final Date result = calendar.getTime();
			if (!isCurrentDayHoliday(holidayList, result))
			{
				dates.add(result);
			}
			calendar.add(Calendar.DATE, 1);
		}
		return dates;
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

	@Override
	public DeliverySlotModel getDeliverySlotforPK(final String selectedDeliverySlot)
	{
		final FlexibleSearchQuery query = new FlexibleSearchQuery(DELIVERY_SLOT_QUERY);
		query.addQueryParameter("pk", selectedDeliverySlot);
		final DeliverySlotModel searchResult = flexibleSearchService.searchUnique(query);
		if (searchResult != null)
		{
			return searchResult;
		}
		else
		{
			return null;
		}
	}

	@Override
	public Optional<DeliverySlotModel> getExistingDeliverySlots(final DeliverySlotModel deliverySlotModel,
			final PointOfServiceModel currentStore)
	{
		final FlexibleSearchQuery query = new FlexibleSearchQuery(EXISTING_DELIVERY_SLOT_LIST);
		query.addQueryParameter("slotCode", deliverySlotModel.getCode());
		query.addQueryParameter("date", deliverySlotModel.getDate());
		query.addQueryParameter("currentStore", currentStore);
		try
		{
			return Optional.ofNullable(flexibleSearchService.searchUnique(query));
		}
		catch (final ModelNotFoundException e)
		{
			return Optional.empty();
		}
	}

}
