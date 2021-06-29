package com.dcx.demo.core.mail.service;

import de.hybris.platform.acceleratorservices.email.impl.DefaultEmailService;
import de.hybris.platform.acceleratorservices.model.email.EmailMessageModel;

import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;


/**
 * Service to create and send emails.
 */
public class DefaultDCXEmailService extends DefaultEmailService
{
	private static final Logger LOG = Logger.getLogger(DefaultDCXEmailService.class);

	@Override
	protected void logInfo(final EmailMessageModel message, final EmailException e)
	{
		LOG.warn(
				"Could not send e-mail pk [" + message.getPk() + "] subject [" + message.getSubject() + "] cause: " + e.getMessage());

		LOG.error("Exception occured while sending email:" + message.getSubject(), e);

	}

}

