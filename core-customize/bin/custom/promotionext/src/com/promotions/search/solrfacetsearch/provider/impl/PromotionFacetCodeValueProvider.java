/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.search.solrfacetsearch.provider.impl;


import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.customercouponservices.daos.CustomerCouponDao;
import de.hybris.platform.europe1.model.DiscountRowModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.promotionengineservices.model.PromotionSourceRuleModel;
import de.hybris.platform.promotions.model.PromotionGroupModel;
import de.hybris.platform.solrfacetsearch.config.IndexConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedProperty;
import de.hybris.platform.solrfacetsearch.config.exceptions.FieldValueProviderException;
import de.hybris.platform.solrfacetsearch.provider.FieldNameProvider;
import de.hybris.platform.solrfacetsearch.provider.FieldValue;
import de.hybris.platform.solrfacetsearch.provider.FieldValueProvider;
import de.hybris.platform.solrfacetsearch.provider.impl.AbstractPropertyFieldValueProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;

import com.promotions.core.conversion.CustomDateFormatter;


/**
 * This ValueProvider will provide a list of promotion codes associated with the product. This implementation uses only
 * the DefaultPromotionGroup.
 */
public class PromotionFacetCodeValueProvider extends AbstractPropertyFieldValueProvider implements FieldValueProvider
{
	private FieldNameProvider fieldNameProvider;
	private CustomerCouponDao customerCouponDao;
	@Resource(name = "customDateFormater")
	private CustomDateFormatter dateFormater;
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
		if (model instanceof ProductModel)
		{
			final ProductModel product = (ProductModel) model;
			final Collection<FieldValue> fieldValues = new ArrayList<FieldValue>();

			if (indexedProperty.isMultiValue())
			{
				fieldValues.addAll(createFieldValues(product, indexConfig, indexedProperty));
			}
			else
			{
				fieldValues.addAll(createFieldValue(product, indexConfig, indexedProperty));
			}
			return fieldValues;
		}
		else
		{
			throw new FieldValueProviderException("Cannot get promotion codes of non-product item");
		}
	}

	protected List<FieldValue> createFieldValue(final ProductModel product, final IndexConfig indexConfig,
			final IndexedProperty indexedProperty)
	{
		final List<FieldValue> fieldValues = new ArrayList<FieldValue>();
		final List<PromotionSourceRuleModel> promotionSourceRules = new ArrayList<>();
		promotionSourceRules.addAll(getAllPromotionForCategory(product));
		promotionSourceRules.addAll(getPromotionForProduct(product));
		final List<PromotionSourceRuleModel> validPromotionSourceRules = getValidPromotions(promotionSourceRules);

		if (CollectionUtils.isNotEmpty(validPromotionSourceRules))
		{
			validPromotionSourceRules.forEach(promotionSourceRule -> {

				final PromotionGroupModel promotionGroup = promotionSourceRule.getWebsite();
				final BaseSiteModel baseSiteModel = indexConfig.getBaseSite();

				if (baseSiteModel != null && baseSiteModel.getDefaultPromotionGroup() != null
						&& checkPromotionGroup(baseSiteModel, promotionGroup)
						&& StringUtils.isNotEmpty(promotionSourceRule.getPromotionFacetCode()))
				{

					addFieldValues(fieldValues, indexedProperty, null, promotionSourceRule.getPromotionFacetCode());
				}
			});
		}
		return fieldValues;
	}

	protected List<FieldValue> createFieldValues(final ProductModel product, final IndexConfig indexConfig,
			final IndexedProperty indexedProperty)
	{
		final List<FieldValue> fieldValues = new ArrayList<FieldValue>();
		final List<PromotionSourceRuleModel> promotionSourceRules = new ArrayList<>();
		promotionSourceRules.addAll(getAllPromotionForCategory(product));
		promotionSourceRules.addAll(getPromotionForProduct(product));
		final List<PromotionSourceRuleModel> validPromotionSourceRules = getValidPromotions(promotionSourceRules);

		if (CollectionUtils.isNotEmpty(validPromotionSourceRules))
		{
			validPromotionSourceRules.forEach(promotionSourceRule -> {
				final PromotionGroupModel promotionGroup = promotionSourceRule.getWebsite();
				final BaseSiteModel baseSiteModel = indexConfig.getBaseSite();

				if (baseSiteModel != null && baseSiteModel.getDefaultPromotionGroup() != null
						&& checkPromotionGroup(baseSiteModel, promotionGroup)
						&& StringUtils.isNotEmpty(promotionSourceRule.getPromotionFacetCode()))
				{

					addFieldValues(fieldValues, indexedProperty, null, promotionSourceRule.getPromotionFacetCode());
				}
			});
		}
		return fieldValues;
	}

	private static boolean checkPromotionGroup(final BaseSiteModel baseSiteModel, final PromotionGroupModel promotionGroup)
	{
		return promotionGroup != null
				&& StringUtils.equals(baseSiteModel.getDefaultPromotionGroup().getIdentifier(), promotionGroup.getIdentifier());

	}

	protected void addFieldValues(final List<FieldValue> fieldValues, final IndexedProperty indexedProperty,
			final LanguageModel language, final Object value)
	{
		final Collection<String> fieldNames = getFieldNameProvider().getFieldNames(indexedProperty,
				language == null ? null : language.getIsocode());
		for (final String fieldName : fieldNames)
		{
			fieldValues.add(new FieldValue(fieldName, value));
		}
	}

	protected List<PromotionSourceRuleModel> getPromotionForProduct(final ProductModel product)
	{
		List<PromotionSourceRuleModel> promotionSourceRules = new ArrayList<>();
		if (product != null)
		{
			promotionSourceRules = getCustomerCouponDao().findPromotionSourceRuleByProduct(product.getCode());
		}
		return promotionSourceRules;
	}

	protected List<PromotionSourceRuleModel> getAllPromotionForCategory(final ProductModel product)
	{
		final List<PromotionSourceRuleModel> promotionSourceRules = new ArrayList<>();
		if (product != null && CollectionUtils.isNotEmpty(product.getSupercategories()))
		{
			product.getSupercategories().forEach(superCategory -> {
				final List<PromotionSourceRuleModel> exclPromotionSourceRules = getCustomerCouponDao()
						.findExclPromotionSourceRuleByCategory(superCategory.getCode());
				if (CollectionUtils.isEmpty(exclPromotionSourceRules))
				{
					promotionSourceRules.addAll(getCustomerCouponDao().findPromotionSourceRuleByCategory(superCategory.getCode()));
					promotionSourceRules.addAll(getPromotionsForCategory(superCategory.getAllSupercategories()));
				}
			});
		}
		return promotionSourceRules;
	}

	protected List<PromotionSourceRuleModel> getPromotionsForCategory(final Collection<CategoryModel> superCategories)
	{
		final List<PromotionSourceRuleModel> promotionSourceRules = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(superCategories))
		{
			superCategories.forEach(categoryModel -> {
				final List<PromotionSourceRuleModel> exclPromotionSourceRules = getCustomerCouponDao()
						.findExclPromotionSourceRuleByCategory(categoryModel.getCode());
				if (CollectionUtils.isEmpty(exclPromotionSourceRules))
				{
					promotionSourceRules.addAll(getCustomerCouponDao().findPromotionSourceRuleByCategory(categoryModel.getCode()));
				}
			});
		}
		return promotionSourceRules;
	}

	private List<PromotionSourceRuleModel> getValidPromotions(final List<PromotionSourceRuleModel> promotionSourceRules)
	{
		final List<PromotionSourceRuleModel> validPromotionSourceRules = new ArrayList<>();
		for (final PromotionSourceRuleModel promotionSourceRuleModel : promotionSourceRules)
		{
			if (((promotionSourceRuleModel.getStartDate() != null)
					&& (promotionSourceRuleModel.getStartDate().compareTo(new Date()) >= 0))
					|| ((promotionSourceRuleModel.getEndDate() != null)
							&& (promotionSourceRuleModel.getEndDate().compareTo(new Date()) <= 0)))
			{
				//skipping this promotion because it is expired/not active
			}
			else
			{
				validPromotionSourceRules.add(promotionSourceRuleModel);
			}
		}
		return validPromotionSourceRules;
	}

	private boolean getDiscountedPrice(final ProductModel product, final IndexConfig indexConfig,
			final IndexedProperty indexedProperty)
	{
		double discountValue = 0.0d;
		if (CollectionUtils.isNotEmpty(indexConfig.getCurrencies()))
		{
			final Iterator var8 = indexConfig.getCurrencies().iterator();

			while (var8.hasNext())
			{
				final CurrencyModel currency = (CurrencyModel) var8.next();
				this.i18nService.setCurrentCurrency(currency);
			}

		}
		for (final DiscountRowModel discountRow : product.getEurope1Discounts())
		{
			final Date currentDate = new Date();
			if (discountRow != null
					&& discountRow.getCurrency().getIsocode().equalsIgnoreCase(i18nService.getCurrentCurrency().getIsocode()))
			{
				if (dateFormater.compareDate(discountRow, currentDate))
				{
					discountValue = discountRow.getValue();
					break;
				}
			}
		}
		if (discountValue > 0)
		{
			return true;
		}
		else
		{
			return false;
		}

	}
}