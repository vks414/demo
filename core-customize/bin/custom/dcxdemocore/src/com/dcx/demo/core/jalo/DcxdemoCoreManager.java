/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.core.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.demo.core.constants.DcxdemoCoreConstants;
import com.dcx.demo.core.setup.CoreSystemSetup;


/**
 * Do not use, please use {@link CoreSystemSetup} instead.
 * 
 */
public class DcxdemoCoreManager extends GeneratedDcxdemoCoreManager
{
	public static final DcxdemoCoreManager getInstance()
	{
		final ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (DcxdemoCoreManager) em.getExtension(DcxdemoCoreConstants.EXTENSIONNAME);
	}
}
