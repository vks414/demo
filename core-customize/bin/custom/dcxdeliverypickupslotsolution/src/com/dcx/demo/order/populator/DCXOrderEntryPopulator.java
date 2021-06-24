
package com.dcx.demo.order.populator;

import de.hybris.platform.commercefacades.order.converters.populator.OrderEntryPopulator;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dcx.demo.slot.DeliverySlotData;


/**
 * Converter for converting order / cart entries
 */
public class DCXOrderEntryPopulator extends OrderEntryPopulator
{
	private static final Logger LOG = LoggerFactory.getLogger(DCXOrderEntryPopulator.class);


	@Override
	public void populate(final AbstractOrderEntryModel source, final OrderEntryData target)
	{
		super.populate(source, target);
		if (source.getDeliverySlotDate() != null && source.getDeliverySlotTime() != null)
		{
			final DeliverySlotData slotData = new DeliverySlotData();
			slotData.setDate(source.getDeliverySlotDate());
			final String[] times = source.getDeliverySlotTime().split("-");
			slotData.setFromTime(times[0]);
			slotData.setToTime(times[1]);
			target.setDeliverySlot(slotData);
		}
	}

}
