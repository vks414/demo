package com.dcx.demo.service.impl;

import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.storelocator.model.PointOfServiceModel;
import de.hybris.platform.storelocator.pos.PointOfServiceService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcx.demo.service.DeliverySlotFacade;
import com.dcx.demo.service.DeliverySlotService;
import com.dcx.demo.slot.DeliverySlotData;
import com.dcx.demo.slot.model.DeliverySlotModel;
import com.dcx.demo.slot.populator.DeliverySlotPopulator;


public class DefaultDeliverySlotFacade implements DeliverySlotFacade
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultDeliverySlotFacade.class);

	@Resource(name = "deliverySlotService")
	private DeliverySlotService deliverySlotService;

	@Resource(name = "deliverySlotPopulator")
	private DeliverySlotPopulator deliverySlotPopulator;

	@Resource(name = "pointOfServiceService")
	private PointOfServiceService pointOfServiceService;

	@Resource(name = "cartService")
	private CartService cartService;

	@Resource(name = "modelService")
	private ModelService modelService;

	private DeliverySlotModel getEarliestDeliverySlot(final PointOfServiceModel pos)
	{
		DeliverySlotModel avaialbleSlot = null;
		final Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(new Date()); // Use Given Date
		int i = 0;
		while (avaialbleSlot == null && i < 5)
		{
			final List<DeliverySlotModel> result = deliverySlotService.getDeliverySlotsForDate(pos, startCalendar.getTime());
			final Optional<DeliverySlotModel> slotModel = result.stream().filter(e -> e.getRemainingCapacity() > 0).findFirst();
			if (slotModel.isPresent())
			{
				//final DeliverySlotData slotData = new DeliverySlotData();
				//deliverySlotPopulator.populate(slotModel.get(), slotData);
				avaialbleSlot = slotModel.get();
			}
			startCalendar.add(Calendar.DATE, 1);
			i++;
		}
		return avaialbleSlot;

	}

	@Override
	public void setDefaultdeliverySlots()
	{
		final CartModel cart = cartService.getSessionCart();
		cart.getEntries().stream().filter(e -> e.getDeliveryPointOfService() != null && e.getDeliverySlotDate() == null)
				.forEach(i -> {
					final PointOfServiceModel pos = i.getDeliveryPointOfService();
					final DeliverySlotModel slotModel = getEarliestDeliverySlot(pos);
					i.setDeliverySlotDate(slotModel.getDate());
					i.setDeliverySlotTime(slotModel.getFromTime() + " - " + slotModel.getToTime());
					i.setDeliverySlotPK(slotModel.getPk().toString());
					modelService.save(i);
					;
				});
	}

	@Override
	public List<DeliverySlotData> getAvailableDeliverySlots(final String pointOfServiceName)
	{
		final PointOfServiceModel pos = pointOfServiceService.getPointOfServiceForName(pointOfServiceName);
		final List<DeliverySlotModel> slotList = deliverySlotService.getAvailableDeliverySlots(pos);
		final List<DeliverySlotData> slotDataList = new ArrayList<>();

		slotList.forEach((final DeliverySlotModel slot) -> {
			final DeliverySlotData slotData = new DeliverySlotData();
			deliverySlotPopulator.populate(slot, slotData);
			slotDataList.add(slotData);
		});

		return slotDataList;

	}

	@Override
	public List<DeliverySlotData> getDeliverySlotsForCodeAndDate(final String code, final Date date)
	{
		// XXX Auto-generated method stub
		return null;

	}

	@Override
	public List<DeliverySlotData> getUnusedDeliverySlotsBetweenDates(final Date startDate, final Date endDate)
	{
		// XXX Auto-generated method stub
		return null;

	}

	@Override
	public List<DeliverySlotData> getUnusedDeliverySlotsBeforeToday()
	{
		// XXX Auto-generated method stub
		return null;

	}

	@Override
	public void setDeliverySlot(final String selectedDeliverySlot, final List<String> entries)
	{
		final DeliverySlotModel slotModel = deliverySlotService.getDeliverySlotforPK(selectedDeliverySlot);
		final CartModel cart = cartService.getSessionCart();
		cart.getEntries().stream().filter(e -> entries.contains(e.getEntryNumber().toString())).forEach(i -> {
			i.setDeliverySlotDate(slotModel.getDate());
			i.setDeliverySlotTime(slotModel.getFromTime() + " - " + slotModel.getToTime());
			i.setDeliverySlotPK(slotModel.getPk().toString());
			modelService.save(i);
			;
		});


	}
}
