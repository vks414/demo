/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.demo.fulfilmentprocess.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.demo.fulfilmentprocess.constants.DcxdemoFulfilmentProcessConstants;

public class DcxdemoFulfilmentProcessManager extends GeneratedDcxdemoFulfilmentProcessManager
{
	public static final DcxdemoFulfilmentProcessManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (DcxdemoFulfilmentProcessManager) em.getExtension(DcxdemoFulfilmentProcessConstants.EXTENSIONNAME);
	}
	
}
