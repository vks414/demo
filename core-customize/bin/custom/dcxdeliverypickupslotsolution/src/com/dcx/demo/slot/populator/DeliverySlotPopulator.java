package com.dcx.demo.slot.populator;



import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.dcx.demo.slot.DeliverySlotData;
import com.dcx.demo.slot.model.DeliverySlotModel;


public class DeliverySlotPopulator implements Populator<DeliverySlotModel, DeliverySlotData>
{
	@Override
	public void populate(final DeliverySlotModel source, final DeliverySlotData target) throws ConversionException
	{
		target.setPK(source.getPk().toString());
		if (source.getDate() != null)
		{
			target.setDate(source.getDate());
		}
		if (source.getFromTime() != null)
		{
			target.setFromTime(source.getFromTime());
		}
		if (source.getToTime() != null)
		{
			target.setToTime(source.getToTime());
		}
		if (source.getCode() != null)
		{
			target.setCode(source.getCode());
		}
		target.setRemainingCapacity(source.getRemainingCapacity());

	}
}

