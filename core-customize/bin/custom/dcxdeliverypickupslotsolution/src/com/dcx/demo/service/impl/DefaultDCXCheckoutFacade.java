package com.dcx.demo.service.impl;

import de.hybris.platform.acceleratorfacades.order.impl.DefaultAcceleratorCheckoutFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import com.dcx.demo.service.DeliverySlotService;
import com.dcx.demo.slot.model.DeliverySlotModel;


/**
 * Default implementation of {@link CheckoutFacade}
 */
public class DefaultDCXCheckoutFacade extends DefaultAcceleratorCheckoutFacade
{
	private static final Logger LOG = Logger.getLogger(DefaultDCXCheckoutFacade.class);

	@Resource(name = "deliverySlotService")
	private DeliverySlotService deliverySlotService;

	@Override
	protected void afterPlaceOrder(@SuppressWarnings("unused")
	final CartModel cartModel, final OrderModel orderModel) //NOSONAR
	{
		if (orderModel != null)
		{
			getCartService().removeSessionCart();
			try
			{
				final Set<String> deliverySlots = new HashSet<>();
				orderModel.getEntries().stream().forEach(i -> {
					if (i.getDeliverySlotPK() != null)
					{
						deliverySlots.add(i.getDeliverySlotPK());
					}
				});
				deliverySlots.stream().forEach(e -> {
					final DeliverySlotModel slot = deliverySlotService.getDeliverySlotforPK(e);
					int cap = slot.getRemainingCapacity();
					slot.setRemainingCapacity(cap--);
					getModelService().save(slot);
				});

			}
			catch (final Exception e2)
			{
				LOG.error("Error in reducing slot capacity.", e2);
			}
			getModelService().refresh(orderModel);
		}
	}

}
