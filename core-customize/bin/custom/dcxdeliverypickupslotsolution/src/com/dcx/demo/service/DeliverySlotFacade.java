/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.service;

import java.util.Date;
import java.util.List;

import com.dcx.demo.slot.DeliverySlotData;


public interface DeliverySlotFacade
{
	List<DeliverySlotData> getAvailableDeliverySlots(String pointOfServiceName);

	List<DeliverySlotData> getDeliverySlotsForCodeAndDate(String code, Date date);

	List<DeliverySlotData> getUnusedDeliverySlotsBetweenDates(Date startDate, Date endDate);

	List<DeliverySlotData> getUnusedDeliverySlotsBeforeToday();

	void setDeliverySlot(String selectedDeliverySlot, List<String> entries);

	void setDefaultdeliverySlots();

}
