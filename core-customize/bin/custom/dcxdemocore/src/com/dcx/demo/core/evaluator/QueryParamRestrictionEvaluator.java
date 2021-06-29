/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.core.evaluator;

import de.hybris.platform.acceleratorservices.uiexperience.UiExperienceService;
import de.hybris.platform.cms2.servicelayer.data.RestrictionData;
import de.hybris.platform.cms2.servicelayer.services.evaluator.CMSRestrictionEvaluator;
import de.hybris.platform.dcxdemocore.model.restrictions.CMSQueryParamRestrictionModel;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class QueryParamRestrictionEvaluator implements CMSRestrictionEvaluator<CMSQueryParamRestrictionModel>
{
	private static final Logger LOG = Logger.getLogger(QueryParamRestrictionEvaluator.class);

	private UiExperienceService uiExperienceService;

	protected UiExperienceService getUiExperienceService()
	{
		return uiExperienceService;
	}

	@Required
	public void setUiExperienceService(final UiExperienceService uiExperienceService)
	{
		this.uiExperienceService = uiExperienceService;
	}

	@Override
	public boolean evaluate(final CMSQueryParamRestrictionModel restriction, final RestrictionData context)
	{
		try
		{

			final HttpServletRequest request = getCurrentRequest();
			if (request != null && request.getQueryString() != null)
			{
				final String[] params = request.getQueryString().split("&");
				for (final String param : params)
				{
					final String[] pair = param.split("=");
					if (pair.length > 1 && pair[0].equalsIgnoreCase("view") && pair[1].equals(restriction.getQueryparam()))
					{
						return true;
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("Could not evaluate CMSQueryParamRestriction. Error in evaluation");
		}

		LOG.warn("Could not evaluate CMSQueryParamRestriction. noQueryParam set. Returning false.");
		return false;
	}

	private HttpServletRequest getCurrentRequest()
	{
		return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
	}
}
