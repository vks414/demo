/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.b2b.demo.core.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.b2b.demo.core.constants.Dcxb2bdemoCoreConstants;
import com.dcx.b2b.demo.core.setup.CoreSystemSetup;


/**
 * Do not use, please use {@link CoreSystemSetup} instead.
 * 
 */
public class Dcxb2bdemoCoreManager extends GeneratedDcxb2bdemoCoreManager
{
	public static final Dcxb2bdemoCoreManager getInstance()
	{
		final ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (Dcxb2bdemoCoreManager) em.getExtension(Dcxb2bdemoCoreConstants.EXTENSIONNAME);
	}
}
