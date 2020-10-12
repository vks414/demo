/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.search.solrfacetsearch.provider.impl;

import de.hybris.platform.customercouponservices.daos.CustomerCouponDao;
import de.hybris.platform.promotionengineservices.model.PromotionSourceRuleModel;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.solrfacetsearch.config.IndexedProperty;
import de.hybris.platform.solrfacetsearch.provider.impl.AbstractFacetValueDisplayNameProvider;
import de.hybris.platform.solrfacetsearch.search.SearchQuery;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Required;

/**
 *
 */
public class PromotionFacetNameDisplayProvider extends AbstractFacetValueDisplayNameProvider {
	private CustomerCouponDao customerCouponDao;
	@Resource
	FlexibleSearchService flexibleSearchService;

	@Resource(name = "i18nService")
	private I18NService i18nService;

	/* (non-Javadoc)
	 * @see de.hybris.platform.solrfacetsearch.provider.impl.AbstractFacetValueDisplayNameProvider#getDisplayName(de.hybris.platform.solrfacetsearch.search.SearchQuery, de.hybris.platform.solrfacetsearch.config.IndexedProperty, java.lang.String)
	 */
	@Override
	public String getDisplayName(final SearchQuery query, final IndexedProperty property, final String facetValue) {

		final String SEARCH_PROMOTION_RULE_QUERY = "select {pr." + PromotionSourceRuleModel.PK + "} from {"
				+ PromotionSourceRuleModel._TYPECODE + " as pr} where  {pr." + PromotionSourceRuleModel.PROMOTIONFACETCODE
				+ "} = ?facetCode";
		final FlexibleSearchQuery promoQuery = new FlexibleSearchQuery(SEARCH_PROMOTION_RULE_QUERY);
		promoQuery.addQueryParameter("facetCode", facetValue);
		final SearchResult<PromotionSourceRuleModel> search = flexibleSearchService.search(promoQuery);
		final List<PromotionSourceRuleModel> promotionsList = search.getResult();

		if (!promotionsList.isEmpty())
		{
			final Optional<PromotionSourceRuleModel> promotions = promotionsList.stream()
					.filter(promotion -> (promotion.getPromotionFacetName() != null && !promotion.getPromotionFacetName().isEmpty()))
					.findFirst();
			if (promotions.isPresent())
			{
				return promotions.get().getPromotionFacetName();
			}
		}
		return facetValue;
	}

	public CustomerCouponDao getCustomerCouponDao() {
		return customerCouponDao;
	}

	@Required
	public void setCustomerCouponDao(final CustomerCouponDao customerCouponDao) {
		this.customerCouponDao = customerCouponDao;
	}

}
