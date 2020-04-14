/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.telco.fulfilmentprocess.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.telco.fulfilmentprocess.constants.DcxtelcoFulfilmentProcessConstants;

public class DcxtelcoFulfilmentProcessManager extends GeneratedDcxtelcoFulfilmentProcessManager
{
	public static final DcxtelcoFulfilmentProcessManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (DcxtelcoFulfilmentProcessManager) em.getExtension(DcxtelcoFulfilmentProcessConstants.EXTENSIONNAME);
	}
	
}
