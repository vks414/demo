package com.dcx.demo.core.model;

import de.hybris.platform.dcxdemocore.model.restrictions.CMSQueryParamRestrictionModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import de.hybris.platform.servicelayer.type.TypeService;
import de.hybris.platform.util.localization.Localization;

import org.springframework.beans.factory.annotation.Required;


public class QueryParamRestrictionDynamicDescription implements DynamicAttributeHandler<String, CMSQueryParamRestrictionModel>
{
	private TypeService typeService;

	protected TypeService getTypeService()
	{
		return typeService;
	}

	@Required
	public void setTypeService(final TypeService typeService)
	{
		this.typeService = typeService;
	}

	@Override
	public String get(final CMSQueryParamRestrictionModel model)
	{
		final String queryParam = model.getQueryparam();

		final StringBuilder result = new StringBuilder();
		if (queryParam != null)
		{
			final String localizedString = Localization.getLocalizedString("type.CMSQueryParamRestriction.description.text");
			result.append(localizedString == null ? "Page only applies on experience level:" : localizedString);
			result.append(' ').append(queryParam);
		}

		return result.toString();
	}

	@Override
	public void set(final CMSQueryParamRestrictionModel model, final String value)
	{
		throw new UnsupportedOperationException();
	}
}
