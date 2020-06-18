/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.setup;

import static com.dcx.demo.constants.DcxdeliverypickupslotsolutionConstants.PLATFORM_LOGO_CODE;

import de.hybris.platform.core.initialization.SystemSetup;

import java.io.InputStream;

import com.dcx.demo.constants.DcxdeliverypickupslotsolutionConstants;
import com.dcx.demo.service.DcxdeliverypickupslotsolutionService;


@SystemSetup(extension = DcxdeliverypickupslotsolutionConstants.EXTENSIONNAME)
public class DcxdeliverypickupslotsolutionSystemSetup
{
	private final DcxdeliverypickupslotsolutionService dcxdeliverypickupslotsolutionService;

	public DcxdeliverypickupslotsolutionSystemSetup(final DcxdeliverypickupslotsolutionService dcxdeliverypickupslotsolutionService)
	{
		this.dcxdeliverypickupslotsolutionService = dcxdeliverypickupslotsolutionService;
	}

	@SystemSetup(process = SystemSetup.Process.INIT, type = SystemSetup.Type.ESSENTIAL)
	public void createEssentialData()
	{
		dcxdeliverypickupslotsolutionService.createLogo(PLATFORM_LOGO_CODE);
	}

	private InputStream getImageStream()
	{
		return DcxdeliverypickupslotsolutionSystemSetup.class.getResourceAsStream("/dcxdeliverypickupslotsolution/sap-hybris-platform.png");
	}
}
