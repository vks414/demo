/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.controller;

import static com.dcx.demo.constants.DcxdeliverypickupslotsolutionConstants.PLATFORM_LOGO_CODE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.dcx.demo.service.DcxdeliverypickupslotsolutionService;


@Controller
public class DcxdeliverypickupslotsolutionHelloController
{
	@Autowired
	private DcxdeliverypickupslotsolutionService dcxdeliverypickupslotsolutionService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String printWelcome(final ModelMap model)
	{
		model.addAttribute("logoUrl", dcxdeliverypickupslotsolutionService.getHybrisLogoUrl(PLATFORM_LOGO_CODE));
		return "welcome";
	}
}
