/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.promotions.core.conversion;

import de.hybris.platform.europe1.model.DiscountRowModel;

import java.util.Date;
/**
 *
 */
public class CustomDateFormatter
{

	public boolean compareDate(final DiscountRowModel discountRow, final Date currentDate)
	{
		final Date startDate = discountRow.getStartTime();
		final Date enddate = discountRow.getEndTime();
		boolean isDiscountedDate = false;
		if (isDateNotEmpty(startDate, enddate, currentDate) && startDate.compareTo(currentDate) < 0
				&& enddate.compareTo(currentDate) >= 0)
		{
			isDiscountedDate = true;
		}
		else if ((startDate != null && enddate == null && startDate.compareTo(currentDate) <= 0) || (startDate == null))
		{
			isDiscountedDate = true;
		}
		return isDiscountedDate;
	}

	private boolean isDateNotEmpty(final Date startDate, final Date enddate, final Date currentDate)
	{
		return startDate != null && enddate != null && currentDate != null;
	}
}
