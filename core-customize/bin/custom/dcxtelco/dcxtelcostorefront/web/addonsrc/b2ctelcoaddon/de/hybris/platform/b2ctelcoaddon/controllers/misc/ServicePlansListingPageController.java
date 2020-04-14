/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2ctelcoaddon.controllers.misc;

import de.hybris.platform.acceleratorstorefrontcommons.controllers.AbstractController;
import de.hybris.platform.b2ctelcoaddon.controllers.TelcoControllerConstants;
import de.hybris.platform.b2ctelcofacades.product.TmaPoFacade;
import de.hybris.platform.b2ctelcofacades.product.TmaProductOfferFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


/**
 * Controller exposing ServicePlans category page actions
 *
 * @since 6.7
 */
@Controller
@RequestMapping(value = "/servicePlanList")
public class ServicePlansListingPageController extends AbstractController
{
	private static final Collection<ProductOption> PRODUCT_OPTIONS = Arrays.asList(ProductOption.BASIC,
			ProductOption.PRICE, ProductOption.DESCRIPTION, ProductOption.STOCK, ProductOption.SOLD_INDIVIDUALLY);

	@Resource(name = "tmaProductOfferFacade")
	private TmaProductOfferFacade tmaProductOfferFacade;

	@RequestMapping(value = "/show")
	public ModelAndView showServicePlanBpoList(@RequestParam final String spoCode, final Model model)
	{
		final List<ProductData> productDataList = new ArrayList<>(
				getTmaProductOfferFacade().getParentsForCodeAndOptions(spoCode, PRODUCT_OPTIONS));

		final ProductData spo = getTmaProductOfferFacade().getPoForCode(spoCode, PRODUCT_OPTIONS);
		if (spo.isSoldIndividually())
		{
			productDataList.add(spo);
		}

		model.addAttribute("bpoData", productDataList);
		model.addAttribute("configurableSpo", spo);

		return new ModelAndView(TelcoControllerConstants.Views.Pages.Misc.MISC_BPO_LISTING);
	}

	protected TmaProductOfferFacade getTmaProductOfferFacade()
	{
		return tmaProductOfferFacade;
	}
}
