/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.facades.process.action;

import de.hybris.platform.basecommerce.enums.ConsignmentStatus;
import de.hybris.platform.orderprocessing.events.SendReadyForPickupMessageEvent;
import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import de.hybris.platform.ordersplitting.model.ConsignmentProcessModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.servicelayer.event.EventService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;


public class UpdateReadyForPickupAndSendMessageAction extends AbstractProceduralAction<ConsignmentProcessModel>
{
	private static final Logger LOG = Logger.getLogger(UpdateReadyForPickupAndSendMessageAction.class);

	private EventService eventService;

	@Override
	public void executeAction(final ConsignmentProcessModel process)
	{
		LOG.info("Process: " + process.getCode() + " in step " + getClass().getSimpleName());
		final ConsignmentModel consignment = process.getConsignment();
		consignment.setStatus(ConsignmentStatus.READY_FOR_PICKUP);
		modelService.save(consignment);
		getEventService().publishEvent(getEvent(process));
		if (LOG.isInfoEnabled())
		{
			LOG.info("Process: " + process.getCode() + " in step " + getClass());
		}

	}

	protected EventService getEventService()
	{
		return eventService;
	}

	@Required
	public void setEventService(final EventService eventService)
	{
		this.eventService = eventService;
	}

	protected SendReadyForPickupMessageEvent getEvent(final ConsignmentProcessModel process)
	{
		return new SendReadyForPickupMessageEvent(process);
	}
}
