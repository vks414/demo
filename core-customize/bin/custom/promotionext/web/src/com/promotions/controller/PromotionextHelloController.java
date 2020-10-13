/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.controller;

import static com.promotions.constants.PromotionextConstants.PLATFORM_LOGO_CODE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.promotions.service.PromotionextService;


@Controller
public class PromotionextHelloController
{
	@Autowired
	private PromotionextService promotionextService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String printWelcome(final ModelMap model)
	{
		model.addAttribute("logoUrl", promotionextService.getHybrisLogoUrl(PLATFORM_LOGO_CODE));
		return "welcome";
	}
}
