/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.service;

import de.hybris.platform.storelocator.model.PointOfServiceModel;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.dcx.demo.slot.model.DeliverySlotModel;


public interface DeliverySlotService
{
	List<DeliverySlotModel> getAvailableDeliverySlots(PointOfServiceModel store);

	List<DeliverySlotModel> getDeliverySlotsBetweenDates(Date startDate, Date endDate);

	List<DeliverySlotModel> getDeliverySlotsBeforeToday();

	DeliverySlotModel getDeliverySlotforPK(String selectedDeliverySlot);

	Optional<DeliverySlotModel> getExistingDeliverySlots(DeliverySlotModel deliverySlotModel, PointOfServiceModel currentStore);

	List<DeliverySlotModel> getDeliverySlotsForDate(PointOfServiceModel store, Date date);
}
