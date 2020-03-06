/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.dcx.b2b.demo.fulfilmentprocess.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import com.dcx.b2b.demo.fulfilmentprocess.constants.Dcxb2bdemoFulfilmentProcessConstants;

public class Dcxb2bdemoFulfilmentProcessManager extends GeneratedDcxb2bdemoFulfilmentProcessManager
{
	public static final Dcxb2bdemoFulfilmentProcessManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (Dcxb2bdemoFulfilmentProcessManager) em.getExtension(Dcxb2bdemoFulfilmentProcessConstants.EXTENSIONNAME);
	}
	
}
