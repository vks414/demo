/*
 *
 * [y] hybris Platform
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.dcxdemocore.jalo.restrictions;

import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloBusinessException;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.type.ComposedType;
import de.hybris.platform.util.localization.Localization;

import org.apache.log4j.Logger;


public class CMSQueryParamRestriction extends GeneratedCMSQueryParamRestriction
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(CMSQueryParamRestriction.class.getName());

	@Override
	protected Item createItem(final SessionContext ctx, final ComposedType type, final ItemAttributeMap allAttributes)
			throws JaloBusinessException
	{
		// business code placed here will be executed before the item is created
		// then create the item
		final Item item = super.createItem(ctx, type, allAttributes);
		// business code placed here will be executed after the item was created
		// and return the item
		return item;
	}

	@Override
	public String getDescription(final SessionContext ctx)
	{
		final String queryParam = getQueryparam();

		final StringBuilder result = new StringBuilder();
		if (queryParam != null)
		{
			final String localizedString = Localization.getLocalizedString("type.CMSQueryParamRestriction.description.text");
			result.append(localizedString == null ? "Page only applies on experience level:" : localizedString);
			result.append(' ').append(queryParam);
		}

		return result.toString();
	}

}
