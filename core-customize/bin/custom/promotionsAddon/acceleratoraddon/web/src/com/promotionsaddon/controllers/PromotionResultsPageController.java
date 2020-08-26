/**
 *
 */
package com.promotionsaddon.controllers;

import de.hybris.platform.acceleratorservices.customer.CustomerLocationService;
import de.hybris.platform.acceleratorservices.storefront.util.PageTitleResolver;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractSearchPageController;
import de.hybris.platform.acceleratorstorefrontcommons.util.XSSFilterUtil;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.category.CategoryService;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.servicelayer.services.CMSComponentService;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.category.CommerceCategoryService;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.search.restriction.SearchRestrictionService;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/*import com.promotions.storefront.controllers.pages.AbstractSearchPageController;
import com.promotions.storefront.controllers.pages.ShowMode;*/
/**
 * @author prprathy
 *
 */
@Controller
@RequestMapping(value = "/promotions")
public class PromotionResultsPageController extends AbstractSearchPageController

{

	/*
	 * private static final String DEALS_META_DESCRIPTION = "deals.meta.description"; private static final String
	 * DEALS_META_KEYWORDS = "deals.meta.keywords"; private static final String BULKOFFER_META_DESCRIPTION =
	 * "bulkoffer.meta.description"; private static final String BULKOFFER_META_KEYWORDS = "seo.meta.keywords.bulkoffer";
	 */
	protected static final String DEALS_QUERY = ":relevance:promotions:On Sale";
	//private static final String FILTER = Config.getParameter("deals.landingpage.title");
	//private static final String DEALS_PAGE_TITLE = "deals.page.title";
	private static final String PROMOTIONPAGE = "promotions";
	/*
	 * private static final String NO_RESULTS_CMS_PAGE_ID = "searchEmpty"; private static final String DEALSPROMOTIONPAGE
	 * = "dealsPromotions"; private static final String ONLINE_CATALOG_VERSION = "Online"; private static final String
	 * LAST_LINK_CLASS = "active";
	 */

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade<ProductData> productSearchFacade;



	@Resource(name = "customerLocationService")
	private CustomerLocationService customerLocationService;

	@Resource(name = "cmsComponentService")
	private CMSComponentService cmsComponentService;

	@Resource(name = "sessionService")
	private SessionService sessionService;
	@Autowired
	private CMSPageService cmsPageService;

	@Resource
	private CatalogVersionService catalogVersionService;

	@Resource
	private SearchRestrictionService searchRestrictionService;
	@Resource
	private CategoryService categoryService;
	@Resource(name = "commerceCategoryService")
	private CommerceCategoryService commerceCategoryService;

	@Resource(name = "pageTitleResolver")
	private PageTitleResolver pageTitleResolver;

	@SuppressWarnings("unused")
	@RequestMapping(method = RequestMethod.GET)
	public String promotionsPage(final Model model, @RequestParam(value = "q", required = false) String searchQuery,
			@RequestParam(value = "page", defaultValue = "0")
			final int page, @RequestParam(value = "show", defaultValue = "Page")
			final ShowMode showMode, @RequestParam(value = "sort", required = false)
			final String sortCode, @RequestParam(value = "pageSize", required = false, defaultValue = "0")
			final int pageSize, final HttpServletRequest request) throws CMSItemNotFoundException
	{
		
		final String breadcrumbQuery = DEALS_QUERY;
		if (StringUtils.isEmpty(searchQuery))
		{
			searchQuery = breadcrumbQuery;
		}
		final SearchQueryData searchQueryData = new SearchQueryData();
		searchQueryData.setValue(XSSFilterUtil.filter(searchQuery));
		final SearchStateData searchState = new SearchStateData();
		searchState.setQuery(searchQueryData);
		final PageableData pageableData = createPageableData(page, pageSize, sortCode, showMode);

		ProductSearchPageData<SearchStateData, ProductData> searchPageData = null;
		try
		{
			searchPageData = encodeSearchPageData(productSearchFacade.textSearch(searchState, pageableData));
		}
		catch (final ConversionException e) // NOSONAR
		{
			// nothing to do - the exception is logged in SearchSolrQueryPopulator
		}

		if (searchPageData == null)
		{
			storeDealsCmsPageInModel(model, getContentPageForLabelOrId(PROMOTIONPAGE));
		}
		else
		{
			/*
			 * final String[] split = breadcrumbQuery.split(":"); searchPageData = breadcrumbBuilder(split,
			 * searchPageData); SearchStateData currentQuery = searchPageData.getCurrentQuery();
			 * currentQuery.setUrl(DfgCoreConstants.DEALS_PAGE_URL+"/?"+StringUtils.substringAfter(currentQuery.getUrl( ),
			 * "?"));
			 */}
		/*
		 * final List<Breadcrumb> breadcrumbs = new ArrayList<>(); final String dealsPageTitle =
		 * MetaSanitizerUtil.sanitizeDescription( getMessageSource().getMessage(DEALS_PAGE_TITLE, null, DEALS_PAGE_TITLE,
		 * getI18nService().getCurrentLocale()));
		 */
		/*
		 * final Breadcrumb breadcrumb = new Breadcrumb("#", dealsPageTitle, PROMOTIONPAGE); breadcrumbs.add(breadcrumb);
		 * model.addAttribute(PromotionextConstants.BREADCRUMBS, breadcrumbs);
		 */
		populateModel(model, searchPageData, showMode);
		/*
		 * model.addAttribute(PromotionextConstants.SEARCH_PAGE_DATA, searchPageData);
		 * model.addAttribute(PromotionextConstants.SEARCH_URL, "/promotions/?q=" +
		 * StringEscapeUtils.escapeHtml(searchQuery)); model.addAttribute(PromotionextConstants.FROM_LANDING_PAGE, true);
		 * model.addAttribute(PromotionextConstants.LANDING_URL, "/promotions/");
		 * model.addAttribute(PromotionextConstants.APPLIED_FILTER, FILTER);
		 * model.addAttribute(PromotionextConstants.TITLE, FILTER);
		 * model.addAttribute("baseUrl",DfgCoreConstants.BASEURL);
		 */
		storeDealsCmsPageInModel(model, getContentPageForLabelOrId(PROMOTIONPAGE));
		/*
		 * model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.INDEX_FOLLOW);
		 * final String metaDescription = MetaSanitizerUtil
		 * .sanitizeDescription(getMessageSource().getMessage(DEALS_META_DESCRIPTION, null, DEALS_META_DESCRIPTION,
		 * getI18nService().getCurrentLocale())); String metaKeywords= MetaSanitizerUtil
		 * .sanitizeDescription(getMessageSource().getMessage(DEALS_META_KEYWORDS, null, DEALS_META_KEYWORDS,
		 * getI18nService().getCurrentLocale()));; setUpMetaData(model,metaKeywords,metaDescription);
		 */
		return getViewForPage(model);

	}

	protected PageTitleResolver getDefaultPageTitleResolver()
	{
		return pageTitleResolver;
	}

	public void storeDealsCmsPageInModel(final Model model, final AbstractPageModel cmsPage)
	{
		if (model != null && cmsPage != null)
		{
			model.addAttribute(CMS_PAGE_MODEL, cmsPage);
			if (cmsPage instanceof ContentPageModel)
			{
				storeContentPageTitleInModel(model, getDefaultPageTitleResolver().resolveContentPageTitle(cmsPage.getTitle()));
			}
		}
	}

}
