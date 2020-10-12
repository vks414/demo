/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.search.solrfacetsearch.provider.impl;

import de.hybris.platform.acceleratorservices.config.SiteConfigService;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.customercouponservices.daos.CustomerCouponDao;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.promotions.PromotionsService;
import de.hybris.platform.promotions.model.AbstractPromotionModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.time.TimeService;
import de.hybris.platform.solrfacetsearch.config.IndexConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedProperty;
import de.hybris.platform.solrfacetsearch.config.exceptions.FieldValueProviderException;
import de.hybris.platform.solrfacetsearch.provider.FieldNameProvider;
import de.hybris.platform.solrfacetsearch.provider.FieldValue;
import de.hybris.platform.solrfacetsearch.provider.FieldValueProvider;
import de.hybris.platform.solrfacetsearch.provider.impl.AbstractPropertyFieldValueProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Required;

import com.promotions.core.conversion.CustomDateFormatter;


/**
 *
 */
public class PromotionDealsValueProvider extends AbstractPropertyFieldValueProvider implements FieldValueProvider
{
	private FieldNameProvider fieldNameProvider;
	private CustomerCouponDao customerCouponDao;
	@Resource
	private PromotionsService promotionsService;
	@Resource
	private CommonI18NService commonI18NService;
	@Resource
	private TimeService timeService;
	@Resource(name = "customDateFormater")
	private CustomDateFormatter customDateFormater;

	@Resource
	private SiteConfigService siteConfigService;

	private ProductService productService;

	public ProductService getProductService()
	{
		return productService;
	}

	public void setProductService(final ProductService productService)
	{
		this.productService = productService;
	}

	protected FieldNameProvider getFieldNameProvider()
	{
		return fieldNameProvider;
	}

	@Required
	public void setFieldNameProvider(final FieldNameProvider fieldNameProvider)
	{
		this.fieldNameProvider = fieldNameProvider;
	}

	public CustomerCouponDao getCustomerCouponDao()
	{
		return customerCouponDao;
	}

	@Required
	public void setCustomerCouponDao(final CustomerCouponDao customerCouponDao)
	{
		this.customerCouponDao = customerCouponDao;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hybris.platform.solrfacetsearch.provider.FieldValueProvider#getFieldValues(de.hybris.platform.solrfacetsearch.
	 * config.IndexConfig, de.hybris.platform.solrfacetsearch.config.IndexedProperty, java.lang.Object)
	 */
	@Override
	public Collection<FieldValue> getFieldValues(final IndexConfig indexConfig, final IndexedProperty indexedProperty,
			final Object model) throws FieldValueProviderException
	{

		final List<FieldValue> fieldValues = new ArrayList<FieldValue>();

		if (model instanceof ProductModel)
		{
			final ProductModel productModel = (ProductModel) model;
			/*
			 * if (CollectionUtils.isNotEmpty(productModel.getVariants())) { productModel = (DFGProductModel)
			 * productService.getDefaultVariantProduct(productModel); }
			 */
			if (checkPromotions(indexConfig, productModel, indexedProperty))
			{
				fieldValues.addAll(createFieldValue("On Sale", indexedProperty));
			}
		}
		return fieldValues;

	}

	private boolean checkPromotions(final IndexConfig indexConfig, final ProductModel product,
			final IndexedProperty indexedProperty)
	{
		final BaseSiteModel baseSiteModel = indexConfig.getBaseSite();
		if (baseSiteModel != null && baseSiteModel.getDefaultPromotionGroup() != null)
		{
			final Date currentTimeRoundedToMinute = DateUtils.round(timeService.getCurrentTime(), Calendar.MINUTE);
			final List<AbstractPromotionModel> promotions = (List<AbstractPromotionModel>) promotionsService
					.getAbstractProductPromotions(Collections.singletonList(baseSiteModel.getDefaultPromotionGroup()), product, true,
							currentTimeRoundedToMinute);
			if (CollectionUtils.isNotEmpty(promotions))
			{
				return true;
			}
			/*
			 * for (final AbstractPromotionModel abstractPromotionModel : promotions) { final RuleBasedPromotionModel
			 * promotion = (RuleBasedPromotionModel) abstractPromotionModel; final String[] promotionTypesList;
			 *
			 * String promotionTypesListforHK = siteConfigService.getString("deals.promotions.list", StringUtils.EMPTY);
			 * if(!promotionTypesListforHK.isEmpty()){ promotionTypesList=promotionTypesListforHK.split(","); }else{
			 * promotionTypesList = Config.getParameter("deals.promotions.list").split(","); } List<String> promotionTypes
			 * = Arrays.asList(promotionTypesList); if (promotionTypes.contains(promotion.getDfgPromotionType())) { return
			 * true; }
			 *
			 * return true; }
			 */

		}
		return false;
		//return getDiscountedPrice(product, indexConfig, indexedProperty);

	}


	/*
	 * private boolean getDiscountedPrice(final ProductModel product, final IndexConfig indexConfig, final
	 * IndexedProperty indexedProperty) { double discountValue = 0.0d; if
	 * (CollectionUtils.isNotEmpty(indexConfig.getCurrencies())) { final Iterator var8 =
	 * indexConfig.getCurrencies().iterator();
	 *
	 * while (var8.hasNext()) { final CurrencyModel currency = (CurrencyModel) var8.next();
	 * this.i18nService.setCurrentCurrency(currency); }
	 *
	 * } for (final DiscountRowModel discountRow : product.getEurope1Discounts()) { final Date currentDate = new Date();
	 * if (discountRow != null &&
	 * discountRow.getCurrency().getIsocode().equalsIgnoreCase(i18nService.getCurrentCurrency().getIsocode())) { if
	 * (customDateFormater.compareDate(discountRow, currentDate)) { discountValue = discountRow.getValue(); break; } } }
	 * if (discountValue > 0) { return true; } else { return false; }
	 *
	 * }
	 */

	protected List<FieldValue> createFieldValue(final String value, final IndexedProperty indexedProperty)
	{
		final List<FieldValue> fieldValues = new ArrayList<FieldValue>();

		final Collection<String> fieldNames = fieldNameProvider.getFieldNames(indexedProperty, null);
		for (final String fieldName : fieldNames)
		{
			fieldValues.add(new FieldValue(fieldName, value));
		}
		return fieldValues;
	}

}
