/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.telco.core.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.telco.core.constants.DcxtelcoCoreConstants;
import com.dcx.telco.core.setup.CoreSystemSetup;


/**
 * Do not use, please use {@link CoreSystemSetup} instead.
 * 
 */
public class DcxtelcoCoreManager extends GeneratedDcxtelcoCoreManager
{
	public static final DcxtelcoCoreManager getInstance()
	{
		final ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (DcxtelcoCoreManager) em.getExtension(DcxtelcoCoreConstants.EXTENSIONNAME);
	}
}
