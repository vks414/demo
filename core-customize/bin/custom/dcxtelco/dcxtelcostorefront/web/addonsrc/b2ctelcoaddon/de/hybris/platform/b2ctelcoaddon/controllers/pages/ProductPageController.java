/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2ctelcoaddon.controllers.pages;

import de.hybris.platform.acceleratorservices.controllers.page.PageType;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.Breadcrumb;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractPageController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.ReviewForm;
import de.hybris.platform.acceleratorstorefrontcommons.forms.validation.ReviewValidator;
import de.hybris.platform.acceleratorstorefrontcommons.util.MetaSanitizerUtil;
import de.hybris.platform.acceleratorstorefrontcommons.util.XSSFilterUtil;
import de.hybris.platform.acceleratorstorefrontcommons.variants.VariantSortStrategy;
import de.hybris.platform.b2ctelcoaddon.breadcrumb.TmaProductOfferingBreadcrumbBuilder;
import de.hybris.platform.b2ctelcoaddon.constants.B2ctelcoaddonWebConstants;
import de.hybris.platform.b2ctelcoaddon.constants.WebConstants;
import de.hybris.platform.b2ctelcoaddon.controllers.TelcoControllerConstants;
import de.hybris.platform.b2ctelcoaddon.controllers.util.ProductDataHelper;
import de.hybris.platform.b2ctelcofacades.product.TmaProductOfferFacade;
import de.hybris.platform.b2ctelcoservices.model.TmaProductOfferingModel;
import de.hybris.platform.b2ctelcoservices.services.TmaPoService;
import de.hybris.platform.catalog.enums.ProductReferenceTypeEnum;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.BaseOptionData;
import de.hybris.platform.commercefacades.product.data.CategoryData;
import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.commercefacades.product.data.ImageDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ProductReferenceData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.commerceservices.category.CommerceCategoryService;
import de.hybris.platform.commerceservices.url.UrlResolver;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Controller for product details page.
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/**/p")
public class ProductPageController extends AbstractPageController
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(ProductPageController.class);

	/**
	 * We use this suffix pattern because of an issue with Spring 3.1 where a Uri value is incorrectly extracted if it
	 * contains on or more '.' characters. Please see https://jira.springsource.org/browse/SPR-6164 for a discussion on
	 * the issue and future resolution.
	 */
	private static final String PRODUCT_CODE_PATH_VARIABLE_PATTERN = "/{productCode:.*}";
	private static final String REVIEWS_PATH_VARIABLE_PATTERN = "{numberOfReviews:.*}";
	private static final String PRODUCT_DATA = "product";
	private final String[] DISALLOWED_FIELDS = new String[] {};

	@Resource(name = "productModelUrlResolver")
	private UrlResolver<ProductModel> productModelUrlResolver;

	@Resource(name = "productFacade")
	private ProductFacade productFacade;

	@Resource(name = "tmaProductOfferFacade")
	private TmaProductOfferFacade productOfferFacade;

	@Resource(name = "productOfferingBreadcrumbBuilder")
	private TmaProductOfferingBreadcrumbBuilder productBreadcrumbBuilder;

	@Resource(name = "cmsPageService")
	private CMSPageService cmsPageService;

	@Resource(name = "variantSortStrategy")
	private VariantSortStrategy variantSortStrategy;

	@Resource(name = "reviewValidator")
	private ReviewValidator reviewValidator;

	@Resource(name = "categoryConverter")
	private Converter<CategoryModel, CategoryData> categoryConverter;

	@Resource(name = "commerceCategoryService")
	private CommerceCategoryService commerceCategoryService;

	@Resource(name = "categoryDataUrlResolver")
	private UrlResolver<CategoryData> categoryDataUrlResolver;

	@Resource(name = "tmaPoService")
	private TmaPoService tmaPoService;

	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	public String productDetail(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request, final HttpServletResponse response)
			throws CMSItemNotFoundException, UnsupportedEncodingException
	{
		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);
		final String redirection = checkRequestUrl(request, response, productModelUrlResolver.resolve(productModel));
		if (StringUtils.isNotEmpty(redirection))
		{
			return redirection;
		}
		updatePageTitle(productModel, model);
		populateProductDetailForDisplay(productModel, model, request);
		model.addAttribute(new ReviewForm());
		final List<ProductReferenceData> productReferences = productFacade.getProductReferencesForCode(productCode,
				Arrays.asList(ProductReferenceTypeEnum.SIMILAR, ProductReferenceTypeEnum.ACCESSORIES),
				Arrays.asList(ProductOption.BASIC, ProductOption.PRICE), null);
		model.addAttribute("productReferences", productReferences);
		model.addAttribute("pageType", PageType.PRODUCT.name());

		final ProductData productData = (ProductData) model.asMap().get(PRODUCT_DATA);
		final String metaKeywords = MetaSanitizerUtil.sanitizeKeywords(productData.getKeywords());
		final String metaDescription = MetaSanitizerUtil.sanitizeDescription(productData.getDescription());
		setUpMetaData(model, metaKeywords, metaDescription);
		return getViewForPage(model);
	}

	/**
	 * Zoom images.
	 *
	 * @param productCode
	 * @param galleryPosition
	 * @param model
	 * @return
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/zoomImages", method = RequestMethod.GET)
	public String showZoomImages(@PathVariable("productCode") final String productCode,
			@RequestParam(value = "galleryPosition", required = false) final String galleryPosition, final Model model,
			final HttpServletRequest request)
	{
		final ProductData productData = getProductOfferFacade().getPoForCode(productCode,
				Collections.singleton(ProductOption.GALLERY));
		final List<Map<String, ImageData>> images = getGalleryImages(productData);
		populateProductData(productData, model, request);
		if (galleryPosition != null)
		{
			try
			{
				model.addAttribute("zoomImageUrl", images.get(Integer.parseInt(galleryPosition)).get("zoom").getUrl());
			}
			catch (final IndexOutOfBoundsException | NumberFormatException ioebe)
			{
				LOG.info(ioebe);
				model.addAttribute("zoomImageUrl", "");
			}
		}
		return TelcoControllerConstants.Views.Fragments.Product.ZOOM_IMAGES_POPUP;
	}

	/**
	 * Quick view.
	 *
	 * @param productCode
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/quickView", method = RequestMethod.GET)
	public String showQuickView(@PathVariable("productCode") final String productCode, final Model model,
			final HttpServletRequest request)
	{
		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);
		final ProductData productData = getProductOfferFacade().getPoForCode(productCode,
				Arrays.asList(ProductOption.BASIC, ProductOption.PRICE, ProductOption.SUMMARY, ProductOption.DESCRIPTION,
						ProductOption.CATEGORIES, ProductOption.PROMOTIONS, ProductOption.STOCK, ProductOption.REVIEW,
						ProductOption.VARIANT_FULL, ProductOption.DELIVERY_MODE_AVAILABILITY));

		sortVariantOptionData(productData);
		populateProductData(productData, model, request);
		getRequestContextData(request).setProduct(productModel);

		return TelcoControllerConstants.Views.Fragments.Product.QUICK_VIEW_POPUP;
	}

	/**
	 * Post review.
	 *
	 * @param productCode
	 * @param form
	 * @param result
	 * @param model
	 * @param request
	 * @param redirectAttrs
	 * @return
	 * @throws CMSItemNotFoundException
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/review", method =
	{ RequestMethod.GET, RequestMethod.POST })
	public String postReview(@PathVariable final String productCode, final ReviewForm form, final BindingResult result,
			final Model model, final HttpServletRequest request, final RedirectAttributes redirectAttrs)
			throws CMSItemNotFoundException
	{
		getReviewValidator().validate(form, result);

		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);

		if (result.hasErrors())
		{
			updatePageTitle(productModel, model);
			GlobalMessages.addErrorMessage(model, "review.general.error");
			model.addAttribute("showReviewForm", Boolean.TRUE);
			populateProductDetailForDisplay(productModel, model, request);
			storeCmsPageInModel(model, getPageForProduct(productModel.getCode()));
			return getViewForPage(model);
		}

		final ReviewData review = new ReviewData();
		review.setHeadline(XSSFilterUtil.filter(form.getHeadline()));
		review.setComment(XSSFilterUtil.filter(form.getComment()));
		review.setRating(form.getRating());
		review.setAlias(XSSFilterUtil.filter(form.getAlias()));
		productFacade.postReview(productCode, review);
		GlobalMessages.addFlashMessage(redirectAttrs, GlobalMessages.CONF_MESSAGES_HOLDER, "review.confirmation.thank.you.title");

		return REDIRECT_PREFIX + productModelUrlResolver.resolve(productModel);
	}

	/**
	 * Show review.
	 *
	 * @param productCode
	 * @param numberOfReviews
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/reviewhtml/"
			+ REVIEWS_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	public String reviewHtml(@PathVariable("productCode") final String productCode,
			@PathVariable("numberOfReviews") final String numberOfReviews, final Model model, final HttpServletRequest request)
	{
		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);
		final List<ReviewData> reviews;
		final ProductData productData = getProductOfferFacade().getPoForCode(productCode,
				Arrays.asList(ProductOption.BASIC, ProductOption.REVIEW));

		if ("all".equals(numberOfReviews))
		{
			reviews = productFacade.getReviews(productCode);
		}
		else
		{
			final int reviewCount = Math.min(Integer.parseInt(numberOfReviews),
					(productData.getNumberOfReviews() == null ? 0 : productData.getNumberOfReviews().intValue()));
			reviews = productFacade.getReviews(productCode, Integer.valueOf(reviewCount));
		}

		getRequestContextData(request).setProduct(productModel);
		reviews.forEach(review -> {
			review.setHeadline(XSSFilterUtil.filter(review.getHeadline()));
			review.setComment(XSSFilterUtil.filter(review.getComment()));
			review.setAlias(XSSFilterUtil.filter(review.getAlias()));
		});
		model.addAttribute("reviews", reviews);
		model.addAttribute("reviewsTotal", productData.getNumberOfReviews());
		model.addAttribute(new ReviewForm());

		return TelcoControllerConstants.Views.Fragments.Product.REVIEWS_TAB;
	}

	/**
	 * Write review dialog.
	 *
	 * @param productCode
	 * @param model
	 * @return
	 * @throws CMSItemNotFoundException
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/writeReview", method = RequestMethod.GET)
	public String writeReview(@PathVariable final String productCode, final Model model) throws CMSItemNotFoundException
	{
		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);
		model.addAttribute(new ReviewForm());
		setUpReviewPage(model, productModel);
		return TelcoControllerConstants.Views.Pages.Product.WRITE_REVIEW;
	}

	protected void setUpReviewPage(final Model model, final ProductModel productModel) throws CMSItemNotFoundException
	{
		final ProductData productData = getProductOfferFacade().getPoForCode(productModel.getCode(),
				Arrays.asList(ProductOption.BASIC, ProductOption.DESCRIPTION));
		final String metaKeywords = MetaSanitizerUtil.sanitizeKeywords(productData.getKeywords());
		final String metaDescription = MetaSanitizerUtil.sanitizeDescription(productData.getDescription());
		setUpMetaData(model, metaKeywords, metaDescription);
		storeCmsPageInModel(model, getPageForProduct(productData.getCode()));
		model.addAttribute("product", productData);
		updatePageTitle(productModel, model);
	}

	/**
	 * Write review.
	 *
	 * @param productCode
	 * @param form
	 * @param result
	 * @param model
	 * @param request
	 * @param redirectAttrs
	 * @return
	 * @throws CMSItemNotFoundException
	 */
	@RequestMapping(value = PRODUCT_CODE_PATH_VARIABLE_PATTERN + "/writeReview", method = RequestMethod.POST)
	public String writeReview(@PathVariable final String productCode, final ReviewForm form, final BindingResult result,
			final Model model, final HttpServletRequest request, final RedirectAttributes redirectAttrs)
			throws CMSItemNotFoundException
	{
		getReviewValidator().validate(form, result);

		final TmaProductOfferingModel productModel = getTmaPoService().getPoForCode(productCode);

		if (result.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "review.general.error");
			populateProductDetailForDisplay(productModel, model, request);
			setUpReviewPage(model, productModel);
			return TelcoControllerConstants.Views.Pages.Product.WRITE_REVIEW;
		}

		final ReviewData review = new ReviewData();
		review.setHeadline(XSSFilterUtil.filter(form.getHeadline()));
		review.setComment(XSSFilterUtil.filter(form.getComment()));
		review.setRating(form.getRating());
		review.setAlias(XSSFilterUtil.filter(form.getAlias()));
		productFacade.postReview(productCode, review);
		GlobalMessages.addFlashMessage(redirectAttrs, GlobalMessages.CONF_MESSAGES_HOLDER, "review.confirmation.thank.you.title");

		return REDIRECT_PREFIX + productModelUrlResolver.resolve(productModel);
	}

	/**
	 * Unknown identifier error.
	 *
	 * @param exception
	 * @param request
	 * @return
	 */

	@ExceptionHandler(UnknownIdentifierException.class)
	public String handleUnknownIdentifierException(final UnknownIdentifierException exception, final HttpServletRequest request)
	{
		request.setAttribute("message", exception.getMessage());
		return FORWARD_PREFIX + "/404";
	}

	protected void updatePageTitle(final ProductModel productModel, final Model model)
	{
		storeContentPageTitleInModel(model, getPageTitleResolver().resolveProductPageTitle(productModel));
	}

	protected void populateProductDetailForDisplay(final TmaProductOfferingModel productModel, final Model model,
			final HttpServletRequest request) throws CMSItemNotFoundException
	{
		getRequestContextData(request).setProduct(productModel);

		final List<ProductOption> options = Arrays.asList(ProductOption.BASIC, ProductOption.PRICE, ProductOption.SUMMARY,
				ProductOption.DESCRIPTION, ProductOption.SOLD_INDIVIDUALLY, ProductOption.GALLERY, ProductOption.CATEGORIES,
				ProductOption.REVIEW, ProductOption.PROMOTIONS, ProductOption.CLASSIFICATION, ProductOption.VARIANT_MATRIX,
				ProductOption.VARIANT_MATRIX_ALL_OPTIONS, ProductOption.STOCK, ProductOption.VOLUME_PRICES,
				ProductOption.DELIVERY_MODE_AVAILABILITY, ProductOption.SPO_BUNDLE_TABS);

		final ProductData productData = getProductOfferFacade().getPoForCode(productModel.getCode(), options);
		sortVariantOptionData(productData);

		storeCmsPageInModel(model, getPageForProduct(productData.getCode()));
		populateProductData(productData, model, request);

		final List<Breadcrumb> breadcrumbs = productBreadcrumbBuilder.getBreadcrumbs(productData.getCode());
		// Note: This is the index of the category above the product's supercategory
		int productSuperSuperCategoryIndex = breadcrumbs.size() - 3;
		final List<CategoryData> superCategories = new ArrayList<CategoryData>();
		if (productSuperSuperCategoryIndex == 0)
		{
			// The category at index 0 is never displayed as a supercategory; for
			// display purposes, the category at index 1 is the root category
			productSuperSuperCategoryIndex = 1;
		}
		if (productSuperSuperCategoryIndex > 0)
		{
			// When product has any supercategory
			final Breadcrumb productSuperSuperCategory = breadcrumbs.get(productSuperSuperCategoryIndex);
			final CategoryModel superSuperCategory = commerceCategoryService
					.getCategoryForCode(productSuperSuperCategory.getCategoryCode());
			for (final CategoryModel superCategory : superSuperCategory.getCategories())
			{
				final CategoryData categoryData = new CategoryData();
				categoryData.setName(superCategory.getName());
				categoryData.setUrl(categoryDataUrlResolver.resolve(categoryConverter.convert(superCategory)));
				superCategories.add(categoryData);
			}
		}

		model.addAttribute(B2ctelcoaddonWebConstants.SUPERCATEGORIES_KEY, superCategories);
		model.addAttribute(WebConstants.BREADCRUMBS_KEY, productBreadcrumbBuilder.getBreadcrumbs(productData.getCode()));
	}

	protected void populateProductData(final ProductData productData, final Model model, final HttpServletRequest request)
	{
		ProductDataHelper.setCurrentProduct(request, productData.getCode());
		model.addAttribute("galleryImages", getGalleryImages(productData));
		model.addAttribute(PRODUCT_DATA, productData);
	}

	protected void sortVariantOptionData(final ProductData productData)
	{
		if (CollectionUtils.isNotEmpty(productData.getBaseOptions()))
		{
			for (final BaseOptionData baseOptionData : productData.getBaseOptions())
			{
				if (CollectionUtils.isNotEmpty(baseOptionData.getOptions()))
				{
					Collections.sort(baseOptionData.getOptions(), variantSortStrategy);
				}
			}
		}

		if (CollectionUtils.isNotEmpty(productData.getVariantOptions()))
		{
			Collections.sort(productData.getVariantOptions(), variantSortStrategy);
		}
	}

	protected List<Map<String, ImageData>> getGalleryImages(final ProductData productData)
	{
		final List<Map<String, ImageData>> galleryImages = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(productData.getImages()))
		{
			final List<ImageData> images = new ArrayList<>();
			for (final ImageData image : productData.getImages())
			{
				if (ImageDataType.GALLERY.equals(image.getImageType()))
				{
					images.add(image);
				}
			}
			Collections.sort(images, new Comparator<ImageData>()
			{
				@Override
				public int compare(final ImageData image1, final ImageData image2)
				{
					return image1.getGalleryIndex().compareTo(image2.getGalleryIndex());
				}
			});

			if (CollectionUtils.isNotEmpty(images))
			{
				int currentIndex = images.get(0).getGalleryIndex().intValue();
				Map<String, ImageData> formats = new HashMap<String, ImageData>();
				for (final ImageData image : images)
				{
					if (currentIndex != image.getGalleryIndex().intValue())
					{
						galleryImages.add(formats);
						formats = new HashMap<>();
						currentIndex = image.getGalleryIndex().intValue();
					}
					formats.put(image.getFormat(), image);
				}
				if (!formats.isEmpty())
				{
					galleryImages.add(formats);
				}
			}
		}
		return galleryImages;
	}

	protected ReviewValidator getReviewValidator()
	{
		return reviewValidator;
	}

	protected AbstractPageModel getPageForProduct(final String productCode) throws CMSItemNotFoundException
	{
		return cmsPageService.getPageForProductCode(productCode);
	}

	protected TmaProductOfferFacade getProductOfferFacade()
	{
		return productOfferFacade;
	}

	protected TmaPoService getTmaPoService()
	{
		return tmaPoService;
	}

	@InitBinder
	public void initBinder(final WebDataBinder binder)
	{
		binder.setDisallowedFields(DISALLOWED_FIELDS);
	}
}
