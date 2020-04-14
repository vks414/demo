/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2ctelcoaddon.controllers.misc;

import de.hybris.platform.acceleratorstorefrontcommons.controllers.AbstractController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.UpdateQuantityForm;
import de.hybris.platform.acceleratorstorefrontcommons.util.XSSFilterUtil;
import de.hybris.platform.b2ctelcoaddon.controllers.TelcoControllerConstants;
import de.hybris.platform.b2ctelcofacades.configurableguidedselling.EntryGroupFacade;
import de.hybris.platform.b2ctelcofacades.data.TmaBpoPreConfigData;
import de.hybris.platform.b2ctelcofacades.order.TmaCartFacade;
import de.hybris.platform.b2ctelcofacades.product.TmaBpoPreConfigFacade;
import de.hybris.platform.b2ctelcoservices.enums.TmaProcessType;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.core.order.EntryGroup;
import de.hybris.platform.jalo.JaloObjectNoLongerValidException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Controller for Add to Cart functionality which is not specific to a certain page.
 */
@Controller("TelcoAddToCartController")
public class AddToCartController extends AbstractController
{

	private static final Logger LOG = Logger.getLogger(AddToCartController.class);

	private static final String BPO_PRECONFIG_INVALID_ERROR_MESSAGE = "bpo.preconfig.invalid.error.message";

	private static final String INVALID_PRE_CONFIG_CALL = "invalidPreConfigCall";

	private static final String MODIFIED_CART_DATA = "modifiedCartData";

	private static final String BASKET_INFORMATION_QUANTITY_NO_ITEMS_ADDED = "basket.information.quantity.noItemsAdded";

	private static final String BASKET_ERROR_OCCURRED = "basket.error.occurred";

	private static final String CART_ENTRY_WAS_NOT_CREATED_REASON = "Cart entry was not created. Reason: ";

	private static final String CART = "/cart";

	private static final String BPO_CONFIGURE_PATH = "/bpo/configure/";

	private static final String BASKET_ERROR_QUANTITY_INVALID = "basket.error.quantity.invalid";

	private static final String QUANTITY = "quantity";

	private static final String ERROR_MSG_TYPE = "errorMsg";

	private final String[] DISALLOWED_FIELDS = new String[] {};

	protected static final String SUCCESS = "success";

	@Resource(name = "cartFacade")
	private CartFacade cartFacade;

	@Resource(name = "productFacade")
	private ProductFacade productFacade;

	@Resource(name = "cartFacade")
	private TmaCartFacade tmaCartFacade;

	@Resource(name = "entryGroupFacade")
	private EntryGroupFacade entryGroupFacade;

	@Resource(name = "tmaBpoPreConfigFacade")
	private TmaBpoPreConfigFacade tmaBpoPreConfigFacade;



	/**
	 * Adds a new {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} entry to cart. The
	 * entry can be added as a simple product offering, or also as part of a bundled product offering identified through
	 * the rootBpoCode parameter.
	 *
	 * @param productCodePost
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel#CODE}
	 * @param processType
	 *           represents the process flow in the context of which the entry is added to cart (Acquisition, Retention,
	 *           etc.)
	 * @param qty
	 *           quantity to be added;default value is 1
	 * @param rootBpoCode
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel#CODE} of the root
	 *           Bundled Product Offering, as part of which the
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} is added
	 * @param cartGroupNo
	 *           specifies the cart entries group number where the entry to be added too; if -1, then a separate cart
	 *           entry group is created
	 * @param subscriptionTermId
	 *           specifies the identifier of the new subscription term
	 * @param subscriberIdentity
	 *           represents subscriber identity for an existing customer
	 * @param subscriberBillingId
	 *           represents subscriber billing system id for an existing customer
	 * @param model
	 *           the Spring model
	 * @return the path for AddToCart popup showing the new added entries
	 */
	@RequestMapping(value = "/cart/add", method = RequestMethod.POST, produces = "application/json")
	public String addSpoToCart(@RequestParam("productCodePost") final String productCodePost,
			@RequestParam(value = "processType") final String processType,
			@RequestParam(value = "qty", required = false, defaultValue = "1") final long qty,
			@RequestParam(value = "rootBpoCode", required = false) final String rootBpoCode,
			@RequestParam(value = "cartGroupNo", required = false, defaultValue = "-1") final int cartGroupNo,
			@RequestParam(value = "subscriptionTermId", required = false) final String subscriptionTermId,
			@RequestParam(value = "subscriberIdentity", required = false) final String subscriberIdentity,
			@RequestParam(value = "subscriberBillingId", required = false) final String subscriberBillingId, final Model model)
	{
		if (qty <= 0)
		{
			model.addAttribute(ERROR_MSG_TYPE, BASKET_ERROR_QUANTITY_INVALID);
			model.addAttribute(QUANTITY, 0L);
		}
		else
		{
			addProductOfferingToCart(productCodePost, qty, processType, rootBpoCode, cartGroupNo, subscriptionTermId,
					subscriberIdentity, subscriberBillingId, model);
		}

		model.addAttribute("product",
				getProductFacade().getProductForCodeAndOptions(productCodePost, Collections.singletonList(ProductOption.BASIC)));

		return TelcoControllerConstants.Views.Fragments.Cart.ADD_TO_CART_POPUP;
	}


	/**
	 * Adds multiple {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} entries to cart, as
	 * part of a bundle product offering specified by the given rootBpoCode. The entries can be added under an existing
	 * cart entries group indicated by the cartGroupNo parameter, or under a new entry group .
	 *
	 * @param simpleProductOfferings
	 *           a list of {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel#CODE}
	 * @param processType
	 *           represents the process flow in the context of which the entry is added to cart (Acquisition, Retention,
	 *           etc.)
	 * @param rootBpoCode
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel#CODE} of the root
	 *           Bundled Product Offering, as part of which the
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} are added
	 * @param cartGroupNo
	 *           specifies the cart entries group number where the entries to be added; if -1, then a separate cart entry
	 *           group is created
	 * @param model
	 *           the Spring model
	 * @return the link to cart page
	 */
	@RequestMapping(value = "/cart/addBpo", method = RequestMethod.POST)
	public String addBpoToCart(@RequestParam(value = "simpleProductOfferings") final List<String> simpleProductOfferings,
			@RequestParam(value = "processType") final String processType,
			@RequestParam(value = "rootBpoCode") final String rootBpoCode,
			@RequestParam(value = "cartGroupNo", required = false, defaultValue = "-1") final int cartGroupNo,
			@RequestParam(value = "subscriptionTermId", required = false) final String subscriptionTermId,
			@RequestParam(value = "subscriberIdentity", required = false) final String subscriberIdentity,
			@RequestParam(value = "subscriberBillingId", required = false) final String subscriberBillingId, final Model model)
	{
		try
		{
			final List<CartModificationData> cartModifications = getTmaCartFacade().addBpoSelectedOfferings(rootBpoCode,
					simpleProductOfferings, processType, cartGroupNo, subscriptionTermId, subscriberIdentity, subscriberBillingId);
			model.addAttribute(MODIFIED_CART_DATA, cartModifications);

			for (final CartModificationData cartModification : cartModifications)
			{
				if (cartModification.getEntry() == null)
				{
					GlobalMessages.addErrorMessage(model, BASKET_INFORMATION_QUANTITY_NO_ITEMS_ADDED);
					model.addAttribute(ERROR_MSG_TYPE, BASKET_INFORMATION_QUANTITY_NO_ITEMS_ADDED);
					throw new CommerceCartModificationException(CART_ENTRY_WAS_NOT_CREATED_REASON + cartModification.getStatusCode());
				}
			}
		}
		catch (final CommerceCartModificationException ex)
		{
			model.addAttribute(ERROR_MSG_TYPE, BASKET_ERROR_OCCURRED);
			LOG.error("Couldn't add BPO of code " + rootBpoCode + " and spos with codes" + simpleProductOfferings, ex);
		}
		return REDIRECT_PREFIX + CART;
	}

	/**
	 * Adds a new {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} entry to cart for the
	 * BPO. The entry can be added as a simple product offering as a part of a bundled product offering identified
	 * through the rootBpoCode parameter and group number if any
	 *
	 * @param productCodePost
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel#CODE}
	 * @param qty
	 *           quantity to be added;default value is 1
	 * @param processType
	 *           represents the process flow in the context of which the entry is added to cart (Acquisition, Retention,
	 *           etc.)
	 * @param rootBpoCode
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel#CODE} of the root
	 *           Bundled Product Offering, as part of which the
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} is added
	 * @param currentStep
	 *           Step id or the group Id from where the SPO is added to the BPO
	 * @param rootGroupNumber
	 *           specifies the cart entries group number where the entry to be added too; if -1, then a separate cart
	 *           entry group is created
	 * @param model
	 *           the Spring model
	 * @param query
	 *           The applied query on page
	 * @param page
	 *           The page number from which the product is added to the cart
	 * @return the path for bpo guided selling for the group appended with the group number
	 */
	@RequestMapping(value = "/cart/addSpo", method = RequestMethod.POST, produces = "application/json")
	public String addSpoToBpoConfiguration(@RequestParam("productCodePost") final String productCodePost,
			@RequestParam(value = "qty", required = false, defaultValue = "1") final long qty,
			@RequestParam(value = "processType") final String processType,
			@RequestParam(value = "rootBpoCode") final String rootBpoCode,
			@RequestParam(value = "currentStep", required = false) final String currentStep,
			@RequestParam(value = "rootGroupNumber", required = false, defaultValue = "-1") final int rootGroupNumber,
			final Model model, final RedirectAttributes redirectModel,
			@RequestParam(value = "q", required = false) final String query,
			@RequestParam(value = "page", required = false) final String page)
	{

		try
		{
			Integer groupNumber = null;
			EntryGroup rootEntryGroup = new EntryGroup();
			if (qty <= 0)
			{
				model.addAttribute(ERROR_MSG_TYPE, BASKET_ERROR_QUANTITY_INVALID);
				model.addAttribute(QUANTITY, Long.valueOf(0L));
			}
			else
			{
				final CartModificationData cartModification = addProductOfferingToCart(productCodePost, qty, processType, rootBpoCode,
						rootGroupNumber, Strings.EMPTY, Strings.EMPTY, Strings.EMPTY, model);

				if (cartModification.getEntry() == null || !cartModification.getStatusCode().equals(SUCCESS))
				{
					GlobalMessages.addErrorMessage(model, BASKET_INFORMATION_QUANTITY_NO_ITEMS_ADDED);
					model.addAttribute(ERROR_MSG_TYPE, BASKET_INFORMATION_QUANTITY_NO_ITEMS_ADDED);
					throw new CommerceCartModificationException(CART_ENTRY_WAS_NOT_CREATED_REASON + cartModification.getStatusCode());
				}

				for (final Integer parentGroup : cartModification.getEntry().getEntryGroupNumbers())
				{
					groupNumber = parentGroup;
				}
				rootEntryGroup = getEntryGroupFacade().getRootEntryGroup(groupNumber);

				model.addAttribute("groupNumber", rootEntryGroup.getGroupNumber());
			}
			model.addAttribute("product",
					getProductFacade().getProductForCodeAndOptions(productCodePost, Collections.singletonList(ProductOption.BASIC)));
			return this.extractJourneyRedirectUrl(XSSFilterUtil.filter(rootBpoCode), rootEntryGroup,
					XSSFilterUtil.filter(currentStep), XSSFilterUtil.filter(page), XSSFilterUtil.filter(query));
		}
		catch (final CommerceCartModificationException ex)
		{
			model.addAttribute(ERROR_MSG_TYPE, BASKET_ERROR_OCCURRED);
			LOG.error("Couldn't add SPO of code " + productCodePost + " as part of " + rootBpoCode, ex);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, BASKET_ERROR_OCCURRED);
			return REDIRECT_PREFIX + BPO_CONFIGURE_PATH + XSSFilterUtil.filter(rootBpoCode) + "/" + XSSFilterUtil.filter(currentStep)
					+ "/" + rootGroupNumber;
		}
	}

	private CartModificationData addProductOfferingToCart(final String spoCode, final long quantity, final String processType,
			final String rootBpoCode, final int cartGroupNo, final String subscriptionTermId, final String subscriberId,
			final String subscriberBillingId, final Model model)
	{
		CartModificationData cartModification = new CartModificationData();

		try
		{
			cartModification = getTmaCartFacade().addProductOfferingToCart(spoCode, quantity, processType, rootBpoCode, cartGroupNo,
					subscriptionTermId, subscriberId, subscriberBillingId);

			model.addAttribute(MODIFIED_CART_DATA, Collections.singleton(cartModification));

			if (cartModification.getQuantityAdded() == 0L)
			{
				GlobalMessages.addErrorMessage(model, "basket.information.quantity.noItemsAdded." + cartModification.getStatusCode());
				model.addAttribute(ERROR_MSG_TYPE, "basket.information.quantity.noItemsAdded." + cartModification.getStatusCode());
			}
			else if (cartModification.getQuantityAdded() < quantity)
			{
				GlobalMessages.addErrorMessage(model,
						"basket.information.quantity.reducedNumberOfItemsAdded." + cartModification.getStatusCode());
				model.addAttribute(ERROR_MSG_TYPE,
						"basket.information.quantity.reducedNumberOfItemsAdded." + cartModification.getStatusCode());

			}

			model.addAttribute("entry", cartModification.getEntry());
			model.addAttribute("cartCode", cartModification.getCartCode());
			model.addAttribute(QUANTITY, quantity);
		}
		catch (final CommerceCartModificationException ex)
		{
			model.addAttribute(ERROR_MSG_TYPE, BASKET_ERROR_OCCURRED);
			LOG.warn("Couldn't add product of code " + spoCode + " to cart.", ex);
		}
		final CartData cartData = getCartFacade().getSessionCart();
		model.addAttribute("cartData", cartData);
		return cartModification;
	}

	/**
	 * Removes the SPO added to the journey step
	 *
	 * @param entryNumber
	 *           entry number of the cart entry to be updated
	 * @param groupId
	 *           The groupId of the journey
	 * @param bpoCode
	 *           The parent BPO code for the journey
	 * @param cartGroupNumber
	 *           specifies the cart entries group number where the entry is to be removed from
	 * @param model
	 *           page model to be populated with information
	 * @param form
	 *           update quantity form specifying the new quantity of the product from the entry with the number given
	 * @param bindingResult
	 *           request binding result to retrieve validation errors from
	 * @param redirectModel
	 *           redirect model to be populated with information
	 * @return redirect url to the journey page
	 */

	@RequestMapping(value = "/cart/removeSpo", method = RequestMethod.POST)
	public String removeSpoFromJourneyStep(@RequestParam("entryNumber") final long entryNumber,
			@RequestParam("bpoCode") final String bpoCode, @RequestParam("groupId") final String groupId,
			@RequestParam("cartGroupNumber") final String cartGroupNumber, final Model model, @Valid final UpdateQuantityForm form,
			final BindingResult bindingResult, final RedirectAttributes redirectModel)
	{

		if (bindingResult.hasErrors())
		{
			populatePageModelWithErrors(model, bindingResult);
		}
		else if (getCartFacade().hasEntries())
		{
			try
			{
				final long newQuantity = form.getQuantity().longValue();
				final CartModificationData cartModification = getCartFacade().updateCartEntry(entryNumber, newQuantity);
				String alertMessageCode;
				if (cartModification.getQuantity() == newQuantity)
				{
					// Success in either removal or updating the quantity
					alertMessageCode = cartModification.getQuantity() == 0 ? "basket.page.message.remove"
							: "basket.page.message.update";
					GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, alertMessageCode);
				}
			}
			catch (final JaloObjectNoLongerValidException | CommerceCartModificationException ex)
			{
				LOG.error("Could not update quantity of product with entry number: " + entryNumber + ".", ex);
			}
		}
		return REDIRECT_PREFIX + BPO_CONFIGURE_PATH + XSSFilterUtil.filter(bpoCode) + "/" + XSSFilterUtil.filter(groupId) + "/"
				+ XSSFilterUtil.filter(cartGroupNumber);
	}

	@RequestMapping(value = "/cart/preconfig/{preConfig}", method = RequestMethod.GET)
	public String addpreConfig(@PathVariable("preConfig") final String preConfig, final Model model,
			final RedirectAttributes redirectModel)
	{
		try
		{
			final TmaBpoPreConfigData tmaBpoPreConfigData = getTmaBpoPreConfigFacade().getPreConfigBpo(preConfig);
			if (tmaBpoPreConfigData != null && CollectionUtils.isNotEmpty(tmaBpoPreConfigData.getSpoList()))
			{
				final List<CartModificationData> cartModifications = getTmaCartFacade().addBpoSelectedOfferings(
						tmaBpoPreConfigData.getRootBpoCode(), tmaBpoPreConfigData.getSpoList(), TmaProcessType.ACQUISITION.getCode(),
						-1, Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
				model.addAttribute(MODIFIED_CART_DATA, cartModifications);

				for (final CartModificationData cartModification : cartModifications)
				{
					if (cartModification.getEntry() == null)
					{
						throw new CommerceCartModificationException(
								CART_ENTRY_WAS_NOT_CREATED_REASON + cartModification.getStatusCode());
					}
				}
			}

		}

		catch (final ModelNotFoundException | IllegalArgumentException modelNotFoundException)
		{
			LOG.info(modelNotFoundException);
			redirectModel.addFlashAttribute(INVALID_PRE_CONFIG_CALL, BPO_PRECONFIG_INVALID_ERROR_MESSAGE);
		}
		catch (final CommerceCartModificationException cartModificationException)
		{
			LOG.info(cartModificationException);
			redirectModel.addFlashAttribute(INVALID_PRE_CONFIG_CALL, BASKET_ERROR_OCCURRED);
		}
		return REDIRECT_PREFIX + CART;
	}


	private void populatePageModelWithErrors(final Model model, final BindingResult bindingResult)
	{
		for (final ObjectError error : bindingResult.getAllErrors())
		{
			final String errorMessageCode = "".equals(error.getCode()) ? BASKET_ERROR_QUANTITY_INVALID : error.getDefaultMessage();
			GlobalMessages.addErrorMessage(model, errorMessageCode);
		}
	}



	/**
	 * This generates the redirect url for the BPO guided selling journey , if the current Step is empty it redirects to
	 * the TmaEditEntryGroupController
	 *
	 * @param rootBpoCode
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel#CODE} of the root
	 *           Bundled Product Offering, as part of which the
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} is added
	 * @param currentStep
	 *           The Current step or the Group of
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel} of the root Bundled
	 *           Product Offering, from where
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaSimpleProductOfferingModel} is added
	 * @param rootEntryGroup
	 *           The {@link de.hybris.platform.core.order.EntryGroup} to which
	 *           {@link de.hybris.platform.b2ctelcoservices.model.TmaBundledProductOfferingModel} is added to the cart
	 * @param page
	 *           The page number from which the product is added to the cart
	 * @param query
	 *           The applied query on page
	 * @return redirect url String
	 */
	private String extractJourneyRedirectUrl(final String rootBpoCode, final EntryGroup rootEntryGroup, final String currentStep,
			final String page, final String query)
	{
		if (!StringUtils.isBlank(currentStep))
		{

			if (!StringUtils.isBlank(page))
			{
				return REDIRECT_PREFIX + BPO_CONFIGURE_PATH + rootBpoCode + "/" + currentStep + "/" + rootEntryGroup.getGroupNumber()
						+ "?q=" + query + "&page=" + page;

			}
			return REDIRECT_PREFIX + BPO_CONFIGURE_PATH + rootBpoCode + "/" + currentStep + "/" + rootEntryGroup.getGroupNumber();
		}
		return REDIRECT_PREFIX + "/bpo/edit/configure/" + rootBpoCode + "/" + rootEntryGroup.getGroupNumber();
	}

	protected CartFacade getCartFacade()
	{
		return cartFacade;
	}

	protected void setCartFacade(final CartFacade cartFacade)
	{
		this.cartFacade = cartFacade;
	}

	protected ProductFacade getProductFacade()
	{
		return productFacade;
	}

	public void setProductFacade(final ProductFacade productFacade)
	{
		this.productFacade = productFacade;
	}

	protected TmaCartFacade getTmaCartFacade()
	{
		return tmaCartFacade;
	}

	public void setTmaCartFacade(final TmaCartFacade tmaCartFacade)
	{
		this.tmaCartFacade = tmaCartFacade;
	}

	protected EntryGroupFacade getEntryGroupFacade()
	{
		return entryGroupFacade;
	}

	public void setEntryGroupFacade(final EntryGroupFacade entryGroupFacade)
	{
		this.entryGroupFacade = entryGroupFacade;
	}

	public TmaBpoPreConfigFacade getTmaBpoPreConfigFacade()
	{
		return tmaBpoPreConfigFacade;
	}

	public void setTmaBpoPreConfigFacade(final TmaBpoPreConfigFacade tmaBpoPreConfigFacade)
	{
		this.tmaBpoPreConfigFacade = tmaBpoPreConfigFacade;
	}

	@InitBinder
	public void initBinder(final WebDataBinder binder)
	{
		binder.setDisallowedFields(DISALLOWED_FIELDS);
	}
}
