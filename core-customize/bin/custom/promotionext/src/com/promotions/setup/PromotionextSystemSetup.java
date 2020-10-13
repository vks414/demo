/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.setup;

import static com.promotions.constants.PromotionextConstants.PLATFORM_LOGO_CODE;

import de.hybris.platform.core.initialization.SystemSetup;

import java.io.InputStream;

import com.promotions.constants.PromotionextConstants;
import com.promotions.service.PromotionextService;


@SystemSetup(extension = PromotionextConstants.EXTENSIONNAME)
public class PromotionextSystemSetup
{
	private final PromotionextService promotionextService;

	public PromotionextSystemSetup(final PromotionextService promotionextService)
	{
		this.promotionextService = promotionextService;
	}

	@SystemSetup(process = SystemSetup.Process.INIT, type = SystemSetup.Type.ESSENTIAL)
	public void createEssentialData()
	{
		promotionextService.createLogo(PLATFORM_LOGO_CODE);
	}

	private InputStream getImageStream()
	{
		return PromotionextSystemSetup.class.getResourceAsStream("/promotionext/sap-hybris-platform.png");
	}
}
