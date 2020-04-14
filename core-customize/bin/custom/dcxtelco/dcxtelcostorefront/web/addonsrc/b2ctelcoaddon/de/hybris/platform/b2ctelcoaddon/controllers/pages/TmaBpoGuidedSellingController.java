/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2ctelcoaddon.controllers.pages;

import de.hybris.platform.acceleratorfacades.customerlocation.CustomerLocationFacade;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.b2ctelcoaddon.controllers.TelcoControllerConstants;
import de.hybris.platform.b2ctelcofacades.bpoguidedselling.TmaBpoGuidedSellingFacade;
import de.hybris.platform.b2ctelcofacades.configurableguidedselling.EntryGroupFacade;
import de.hybris.platform.b2ctelcofacades.data.GuidedSellingStepData;
import de.hybris.platform.b2ctelcoservices.enums.TmaProcessType;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Controller Returns the Lister page for respective Product offering Groups associated with BPO
 *
 * @since 6.7
 */
@Controller
@RequestMapping(value = "/bpo")
public class TmaBpoGuidedSellingController extends AbstractSearchPageController
{

	private static final String NO_RESULTS_CMS_PAGE_ID = "searchEmpty";
	private static final String BPO_LISTER_PAGE = "bpoGuidedSellingProductList";

	private static final Logger LOG = Logger.getLogger(TmaBpoGuidedSellingController.class);

	private TmaBpoGuidedSellingFacade tmaBpoGuidedSellingFacade;
	private EntryGroupFacade entryGroupFacade;
	private CustomerLocationFacade customerLocationFacade;
	private final String[] DISALLOWED_FIELDS = new String[] {};

	@RequestMapping(value =
	{ "/configure/{bpoCode}", "/configure/{bpoCode}/{groupId}",
			"/configure/{bpoCode}/{groupId}/{groupNumber}" }, method = RequestMethod.GET)
	public String getGuidedSellingJourneyForGroup(@PathVariable("bpoCode") final String bpoCode,
			@PathVariable(value = "groupId", required = false) final String groupId,
			@PathVariable(value = "groupNumber", required = false) final Integer groupNumber,
			@RequestParam(value = "q", required = false) final String searchQuery,
			@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode, final Model model,
			final HttpServletRequest request) throws CMSItemNotFoundException
	{
		String currentStepKey = null;
		String nextStepKey = null;
		try
		{
			final LinkedMap<String, GuidedSellingStepData> bundleProductOfferingGuidedSellingStepData = (LinkedMap<String, GuidedSellingStepData>) getTmaBpoGuidedSellingFacade()
					.getCalculatedStepsForBPO(bpoCode);
			getTmaBpoGuidedSellingFacade().validateAndGetBPOEntryGroup(bpoCode, groupNumber);
			if (null != groupId)
			{
				final Boolean isValidStep = bundleProductOfferingGuidedSellingStepData.containsKey(groupId);
				if (!isValidStep)
				{
					return setupErrorPage(model, NO_RESULTS_CMS_PAGE_ID, "bpo.guidedselling.group.error");
				}
				currentStepKey = (groupId);
			}
			if (groupId == null && !bundleProductOfferingGuidedSellingStepData.isEmpty())
			{
				currentStepKey = bundleProductOfferingGuidedSellingStepData.firstKey();
			}
			if (!bundleProductOfferingGuidedSellingStepData.isEmpty())
			{
				nextStepKey = bundleProductOfferingGuidedSellingStepData.nextKey(currentStepKey);
			}
			final PageableData pageableData = createPageableData(page, getSearchPageSize(), sortCode, showMode);
			final ProductSearchPageData<SearchStateData, ProductData> searchPageData = getTmaBpoGuidedSellingFacade()
					.getProductsAssociatedWithGroups(currentStepKey, pageableData, searchQuery, bpoCode, groupNumber); // todo move all this logic to facade+service
			populateModelWithAttributes(bpoCode, model, currentStepKey, bundleProductOfferingGuidedSellingStepData, searchPageData,
					nextStepKey, groupNumber);
			populateModel(model, searchPageData, showMode);
			setUpPageTitle(getPageTitle(), model);
			storeContinueUrl(request);
			storeCmsPageInModel(model, getCmsPageService().getPageForId(BPO_LISTER_PAGE));
		}
		catch (final IllegalArgumentException e)
		{
			LOG.error(" No group with number " + groupNumber + " found in the order", e);
			return setupErrorPage(model, NO_RESULTS_CMS_PAGE_ID, "bpo.guidedselling.group.error");
		}
		catch (final ModelNotFoundException | UnknownIdentifierException e)
		{
			LOG.error(" BPO does not exist", e);
			return setupErrorPage(model, NO_RESULTS_CMS_PAGE_ID, "bpo.guidedselling.code.error");
		}

		return getViewForPage(model);
	}

	private String getProcessTypeByGroup(final Integer groupId)
	{
		return groupId != null ? getEntryGroupFacade().getCurrentEntryGroup(groupId).getProcessType()
				: TmaProcessType.ACQUISITION.getCode();
	}

	private void populateModelWithAttributes(final String bpoCode, final Model model, final String currentStepKey,
			final Map<String, GuidedSellingStepData> bundleProductOfferingGuidedSellingStepData,
			final ProductSearchPageData<SearchStateData, ProductData> searchPageData, final String nextStepKey,
			final Integer groupNumber) throws CMSItemNotFoundException
	{
		if (searchPageData == null)
		{
			storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_CMS_PAGE_ID));
		}
		else if (searchPageData.getPagination().getTotalNumberOfResults() == 0)
		{
			model.addAttribute("searchPageData", searchPageData);
			storeCmsPageInModel(model, getContentPageForLabelOrId(NO_RESULTS_CMS_PAGE_ID));
		}
		else
		{
			model.addAttribute("currentStep", currentStepKey);
			model.addAttribute("bpoCode", bpoCode);
			model.addAttribute("dashboard", getTmaBpoGuidedSellingFacade().getDashBoardForBPO(bpoCode, groupNumber));
			model.addAttribute("nextStep", nextStepKey);
			model.addAttribute("groupNumber", groupNumber);
			model.addAttribute("cgsProcessType", getProcessTypeByGroup(groupNumber));
		}
		model.addAttribute("userLocation", getCustomerLocationFacade().getUserLocationData());
	}

	protected String setupErrorPage(final Model model, final String label, final String errorMessage)
			throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(label));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(label));
		GlobalMessages.addErrorMessage(model, errorMessage);
		return TelcoControllerConstants.Views.Pages.Error.ERROR_NOT_FOUND_PAGE;
	}

	protected void setUpPageTitle(final String bundleName, final Model model)
	{
		storeContentPageTitleInModel(model, getPageTitleResolver().resolveContentPageTitle(bundleName));
	}

	protected String getPageTitle()
	{
		return getMessageSource().getMessage("bpo.guidedsellling.component.name.default", null,
				getI18nService().getCurrentLocale());
	}

	protected TmaBpoGuidedSellingFacade getTmaBpoGuidedSellingFacade()
	{
		return tmaBpoGuidedSellingFacade;
	}

	@Autowired
	public void setTmaBpoGuidedSellingFacade(final TmaBpoGuidedSellingFacade tmaBpoGuidedSellingFacade)
	{
		this.tmaBpoGuidedSellingFacade = tmaBpoGuidedSellingFacade;
	}

	protected EntryGroupFacade getEntryGroupFacade()
	{
		return entryGroupFacade;
	}

	@Autowired
	public void setEntryGroupFacade(final EntryGroupFacade entryGroupFacade)
	{
		this.entryGroupFacade = entryGroupFacade;
	}

	protected CustomerLocationFacade getCustomerLocationFacade()
	{
		return customerLocationFacade;
	}

	@Autowired
	public void setCustomerLocationFacade(final CustomerLocationFacade customerLocationFacade)
	{
		this.customerLocationFacade = customerLocationFacade;
	}

	@InitBinder
	public void initBinder(final WebDataBinder binder)
	{
		binder.setDisallowedFields(DISALLOWED_FIELDS);
	}
}
