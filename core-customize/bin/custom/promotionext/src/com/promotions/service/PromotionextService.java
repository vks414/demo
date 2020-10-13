/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.service;

public interface PromotionextService
{
	String getHybrisLogoUrl(String logoCode);

	void createLogo(String logoCode);
}
