package com.dcx.demo.facades.customer;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import de.hybris.platform.commercefacades.customer.impl.DefaultCustomerFacade;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.core.model.security.PrincipalGroupModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserGroupModel;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;


public class DefaultDCXCustomerFacade extends DefaultCustomerFacade
{
	private static final Logger LOG = Logger.getLogger(DefaultDCXCustomerFacade.class);

	@Override
	public void register(final RegisterData registerData) throws DuplicateUidException
	{
		validateParameterNotNullStandardMessage("registerData", registerData);
		Assert.hasText(registerData.getFirstName(), "The field [FirstName] cannot be empty");
		Assert.hasText(registerData.getLastName(), "The field [LastName] cannot be empty");
		Assert.hasText(registerData.getLogin(), "The field [Login] cannot be empty");

		final CustomerModel newCustomer = getModelService().create(CustomerModel.class);
		try
		{
			final UserGroupModel customCustomerGroup = getUserService().getUserGroupForUID("customCustomerGroup");
			final Set<PrincipalGroupModel> groups = new HashSet<PrincipalGroupModel>();
			groups.addAll(newCustomer.getGroups());
			groups.add(customCustomerGroup);
			newCustomer.setGroups(groups);
		}
		catch (final Exception e)
		{
			LOG.error("No group found with id-customCustomerGroup", e);
		}
		setCommonPropertiesForRegister(registerData, newCustomer);
		getCustomerAccountService().register(newCustomer, registerData.getPassword());
	}

}
