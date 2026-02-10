/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.accounting.thirdparty.paypal;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;
import com.paypal.sdk.services.NVPCallerServices;


import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PayPalServicesContext;
/**
 * PayPalServices for NVP API communication
 */
public class PayPalServices {

    private static final String MODULE = PayPalServices.class.getName();
    private static final String RESOURCE = x.AccountingErrorUiLabels;

    // Used to maintain a weak reference to the ShoppingCart for customers who have gone to PayPal to checkout
    // so that we can quickly grab the cart, perform shipment estimates and send the info back to PayPal.
    // The weak key is a simple wrapper for the checkout token String and is stored as a cart attribute. The value
    // is a weak reference to the ShoppingCart itself.  Entries will be removed as carts are removed from the
    // session (i.e. on cart clear or successful checkout) or when the session is destroyed
    private static Map<TokenWrapper, WeakReference<ShoppingCart>> tokenCartMap = new WeakHashMap<>();

    public static Map<String, Object> setExpressCheckout(DispatchContext dctx, PayPalServicesContext context) {
        ShoppingCart cart = (ShoppingCart) context.get(x.cart);
        Locale locale = cart.getLocale();
        if (cart == null || cart.items().size() <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalShoppingCartIsEmpty, locale));
        }

        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalPaymentGatewayConfigCannotFind, locale));
        }


        NVPEncoder encoder = new NVPEncoder();

        // Set Express Checkout Request Parameters
        encoder.add(x.METHOD, x.SetExpressCheckout);
        String token = (String) cart.getAttribute(x.payPalCheckoutToken);
        if (UtilValidate.isNotEmpty(token)) {
            encoder.add(x.TOKEN, token);
        }
        encoder.add(x.RETURNURL, payPalConfig.getString(x.returnUrl));
        encoder.add(x.CANCELURL, payPalConfig.getString(x.cancelReturnUrl));
        if (!cart.shippingApplies()) {
            encoder.add(x.NOSHIPPING, x._1);
        } else {
            encoder.add(x.CALLBACK, payPalConfig.getString(x.shippingCallbackUrl));
            encoder.add(x.CALLBACKTIMEOUT, x._6);
            // Default to no
            String reqConfirmShipping = x.Y.equals(payPalConfig.getString(x.requireConfirmedShipping)) ? x._1 : x._0;
            encoder.add(x.REQCONFIRMSHIPPING, reqConfirmShipping);
            // Default shipment method
            encoder.add(x.L_SHIPPINGOPTIONISDEFAULT0, x._true);
            encoder.add(x.L_SHIPPINGOPTIONNAME0, x.Calculated_Offline);
            encoder.add(x.L_SHIPPINGOPTIONAMOUNT0, x._0_00);
        }
        encoder.add(x.ALLOWNOTE, x._1);
        encoder.add(x.INSURANCEOPTIONOFFERED, x._false);
        if (UtilValidate.isNotEmpty(payPalConfig.getString(x.imageUrl)));
        encoder.add(x.PAYMENTACTION, x.Order_1d75774c);

        // Cart information
        try {
            addCartDetails(encoder, cart);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalErrorDuringRetrievingCartDetails, locale));
        }

        NVPDecoder decoder;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, String> errorMessages = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errorMessages)) {
            if (errorMessages.containsKey(x._10411)) {
                // Token has expired, get a new one
                cart.setAttribute(x.payPalCheckoutToken, null);
                return PayPalServices.setExpressCheckout(dctx, context);
            }
            return ServiceUtil.returnError(UtilMisc.toList(errorMessages.values()));
        }

        token = decoder.get(x.TOKEN);
        cart.setAttribute(x.payPalCheckoutToken, token);
        TokenWrapper tokenWrapper = new TokenWrapper(token);
        cart.setAttribute(x.payPalCheckoutTokenObj, tokenWrapper);
        PayPalServices.tokenCartMap.put(tokenWrapper, new WeakReference<>(cart));
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> payPalCheckoutUpdate(DispatchContext dctx, PayPalServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        HttpServletRequest request = (HttpServletRequest) context.get(x.request);
        HttpServletResponse response = (HttpServletResponse) context.get(x.response);

        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);

        String token = (String) paramMap.get(x.TOKEN);
        WeakReference<ShoppingCart> weakCart = tokenCartMap.get(new TokenWrapper(token));
        ShoppingCart cart = null;
        if (weakCart != null) {
            cart = weakCart.get();
        }
        if (cart == null) {
            Debug.logError(x.Could_locate_the_ShoppingCart_for_token + token, MODULE);
            return ServiceUtil.returnSuccess();
        }
        // Since most if not all of the shipping estimate codes requires a persisted contactMechId we'll create one and
        // then delete once we're done, now is not the time to worry about updating everything
        String contactMechId = null;
        Map<String, Object> inMap = new HashMap<>();
        inMap.put(x.address1, paramMap.get(x.SHIPTOSTREET));
        inMap.put(x.address2, paramMap.get(x.SHIPTOSTREET2));
        inMap.put(x.city, paramMap.get(x.SHIPTOCITY));
        String countryGeoCode = (String) paramMap.get(x.SHIPTOCOUNTRY);
        String countryGeoId = PayPalServices.getCountryGeoIdFromGeoCode(countryGeoCode, delegator);
        if (countryGeoId == null) {
            return ServiceUtil.returnSuccess();
        }
        inMap.put(x.countryGeoId, countryGeoId);
        inMap.put(x.stateProvinceGeoId, parseStateProvinceGeoId((String) paramMap.get(x.SHIPTOSTATE), countryGeoId, delegator));
        inMap.put(x.postalCode, paramMap.get(x.SHIPTOZIP));

        try {
            GenericValue userLogin = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class)
                    .findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.system), true);
            inMap.put(x.userLogin, userLogin);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        boolean beganTransaction = false;
        Transaction parentTransaction = null;
        try {
            parentTransaction = TransactionUtil.suspend();
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException e1) {
            Debug.logError(e1, MODULE);
        }
        try {
            Map<String, Object> outMap = dispatcher.runSync(x.createPostalAddress, inMap);
            contactMechId = (String) outMap.get(x.contactMechId);
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnSuccess();
        }
        try {
            TransactionUtil.commit(beganTransaction);
            if (parentTransaction != null) TransactionUtil.resume(parentTransaction);
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
        }
        // clone the cart so we can modify it temporarily
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        String oldShipAddress = cart.getShippingContactMechId();
        coh.setCheckOutShippingAddress(contactMechId);
        ShippingEstimateWrapper estWrapper = new ShippingEstimateWrapper(dispatcher, cart, 0);
        int line = 0;
        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.CallbackResponse);

        for (GenericValue shipMethod : estWrapper.getShippingMethods()) {
            BigDecimal estimate = estWrapper.getShippingEstimate(shipMethod);
            //Check that we have a valid estimate (allowing zero value estimates for now)
            if (estimate == null || estimate.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            cart.setAllShipmentMethodTypeId(shipMethod.getString(x.shipmentMethodTypeId));
            cart.setAllCarrierPartyId(shipMethod.getString(x.partyId));
            try {
                coh.calcAndAddTax();
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                continue;
            }
            String estimateLabel = shipMethod.getString(x.partyId) + x.str_fc02e199 + shipMethod.getString(x.description);
            encoder.add(x.L_SHIPINGPOPTIONLABEL + line, estimateLabel);
            encoder.add(x.L_SHIPPINGOPTIONAMOUNT + line, estimate.setScale(2, RoundingMode.HALF_UP).toPlainString());
            // Just make this first one default for now
            encoder.add(x.L_SHIPPINGOPTIONISDEFAULT + line, line == 0 ? x._true : x._false);
            encoder.add(x.L_TAXAMT + line, cart.getTotalSalesTax().setScale(2, RoundingMode.HALF_UP).toPlainString());
            line++;
        }
        String responseMsg = null;
        try {
            responseMsg = encoder.encode();
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
        }
        if (responseMsg != null) {
            try {
                response.setContentLength(responseMsg.getBytes(x.UTF_8).length);
            } catch (UnsupportedEncodingException e) {
                Debug.logError(e, MODULE);
            }

            try (Writer writer = response.getWriter()) {
                writer.write(responseMsg);
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }

        // Remove the temporary ship address
        try {
            GenericValue postalAddress = DaoRegistry.getDao(delegator, x.PostalAddress, UserLoginDao.class)
                    .findOne(delegator, x.PostalAddress, UtilMisc.toMap(x.contactMechId, contactMechId), false);
            postalAddress.remove();
            GenericValue contactMech = DaoRegistry.getDao(delegator, x.ContactMech, UserLoginDao.class)
                    .findOne(delegator, x.ContactMech, UtilMisc.toMap(x.contactMechId, contactMechId), false);
            contactMech.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        coh.setCheckOutShippingAddress(oldShipAddress);
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, String> getErrorMessageMap(NVPDecoder decoder) {
        String ack = decoder.get(x.ACK);
        Map<String, String> result = null;
        if (!x.Success.equals(ack)) {
            result = new HashMap<String, Object>();
            int i = 0;
            while (UtilValidate.isNotEmpty(decoder.get(x.L_ERRORCODE + i))) {
                String errorCode = decoder.get(x.L_ERRORCODE + i);
                String longMsg = decoder.get(x.L_LONGMESSAGE + i);
                result.put(errorCode, x.PayPal_Response_Error + errorCode + x.str_4ff447b8 + longMsg);
                i++;
            }
        }
        return result;
    }

    private static void addCartDetails(NVPEncoder encoder, ShoppingCart cart) throws GenericEntityException {
        encoder.add(x.CURRENCYCODE, cart.getCurrency());
        int line = 0;
        for (ShoppingCartItem item : cart.items()) {
            encoder.add(x.L_NUMBER + line, item.getProductId());
            encoder.add(x.L_NAME + line, item.getName());
            encoder.add(x.L_AMT + line, item.getBasePrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
            encoder.add(x.L_QTY + line, item.getQuantity().toBigInteger().toString());
            line++;
            BigDecimal otherAdjustments = item.getOtherAdjustments();
            if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
                encoder.add(x.L_NUMBER + line, item.getProductId());
                encoder.add(x.L_NAME + line, item.getName() + x.Adjustments);
                encoder.add(x.L_AMT + line, otherAdjustments.setScale(2, RoundingMode.HALF_UP).toPlainString());
                encoder.add(x.L_QTY + line, x._1);
                line++;
            }
        }
        BigDecimal otherAdjustments = cart.getOrderOtherAdjustmentTotal();
        if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
            encoder.add(x.L_NUMBER + line, x.N_A);
            encoder.add(x.L_NAME + line, x.Order_Adjustments);
            encoder.add(x.L_AMT + line, otherAdjustments.setScale(2, RoundingMode.HALF_UP).toPlainString());
            encoder.add(x.L_QTY + line, x._1);
            line++;
        }
        encoder.add(x.ITEMAMT, cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
        encoder.add(x.SHIPPINGAMT, x._0_00);
        encoder.add(x.TAXAMT, x._0_00);
        encoder.add(x.AMT, cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
        //NOTE: The docs say this is optional but then won't work without it
        encoder.add(x.MAXAMT, cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
    }

    public static Map<String, Object> getExpressCheckout(DispatchContext dctx, PayPalServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        ShoppingCart cart = (ShoppingCart) context.get(x.cart);
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalPaymentGatewayConfigCannotFind, locale));
        }

        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.GetExpressCheckoutDetails);
        String token = (String) cart.getAttribute(x.payPalCheckoutToken);
        if (UtilValidate.isNotEmpty(token)) {
            encoder.add(x.TOKEN, token);
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalTokenNotFound, locale));
        }

        NVPDecoder decoder;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(decoder.get(x.NOTE))) {
            cart.addOrderNote(decoder.get(x.NOTE));
        }

        if (cart.getUserLogin() == null) {
            try {
                GenericValue userLogin = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class)
                        .findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.anonymous), false);
                try {
                    cart.setUserLogin(userLogin, dispatcher);
                } catch (CartItemModifyException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        boolean anon = x.anonymous.equals(cart.getUserLogin().getString(x.userLoginId));
        // Even if anon, a party could already have been created
        String partyId = cart.getOrderPartyId();
        if (partyId == null && anon) {
            // Check nothing has been set on the anon userLogin either
            partyId = cart.getUserLogin() != null ? cart.getUserLogin().getString(x.partyId) : null;
            cart.setOrderPartyId(partyId);
        }
        if (partyId != null) {
            GenericValue party = null;
            try {
                party = DaoRegistry.getDao(delegator, x.Party, UserLoginDao.class)
                        .findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (party == null) {
                partyId = null;
            }
        }

        Map<String, Object> inMap = new HashMap<>();
        Map<String, Object> outMap = null;
        // Create the person if necessary
        boolean newParty = false;
        if (partyId == null) {
            newParty = true;
            inMap.put(x.userLogin, cart.getUserLogin());
            inMap.put(x.personalTitle, decoder.get(x.SALUTATION));
            inMap.put(x.firstName, decoder.get(x.FIRSTNAME));
            inMap.put(x.middleName, decoder.get(x.MIDDLENAME));
            inMap.put(x.lastName, decoder.get(x.LASTNAME));
            inMap.put(x.suffix, decoder.get(x.SUFFIX));
            try {
                outMap = dispatcher.runSync(x.createPerson, inMap);
                partyId = (String) outMap.get(x.partyId);
                cart.setOrderPartyId(partyId);
                cart.getUserLogin().setString(x.partyId, partyId);
                inMap.clear();
                inMap.put(x.userLogin, cart.getUserLogin());
                inMap.put(x.partyId, partyId);
                inMap.put(x.roleTypeId, x.CUSTOMER_340f7cf7);
                dispatcher.runSync(x.createPartyRole, inMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        // Create a new email address if necessary
        String emailContactMechId = null;
        String emailContactPurposeTypeId = x.PRIMARY_EMAIL;
        String emailAddress = decoder.get(x.EMAIL);
        if (!newParty) {
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(UtilMisc.toMap(x.partyId, partyId, x.contactMechTypeId, x.EMAIL_ADDRESS)),
                    EntityCondition.makeCondition(EntityFunction.upperField(x.infoString), EntityComparisonOperator.EQUALS, EntityFunction.upper(emailAddress))));
            try {
                GenericValue matchingEmail = DaoRegistry.getDao(delegator, x.PartyAndContactMech, UserLoginDao.class)
                        .findFirstByCondition(delegator, x.PartyAndContactMech,
                                EntityCondition.makeCondition(cond, EntityUtil.getFilterByDateExpr()), null, UtilMisc.toList(x.fromDate), false);
                if (matchingEmail != null) {
                    emailContactMechId = matchingEmail.getString(x.contactMechId);
                } else {
                    // No email found so we'll need to create one but first check if it should be PRIMARY or just BILLING
                    EntityCondition primaryEmailCond = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(UtilMisc.toMap(x.partyId, partyId,
                                    x.contactMechTypeId, x.EMAIL_ADDRESS,
                                    x.contactMechPurposeTypeId, x.PRIMARY_EMAIL)),
                            EntityUtil.getFilterByDateExpr(x.contactFromDate, x.contactThruDate),
                            EntityUtil.getFilterByDateExpr(x.purposeFromDate, x.purposeThruDate)));
                    long primaryEmails = DaoRegistry.getDao(delegator, x.PartyContactWithPurpose, UserLoginDao.class)
                            .countByCondition(delegator, x.PartyContactWithPurpose, primaryEmailCond, null, null);
                    if (primaryEmails > 0) emailContactPurposeTypeId = x.BILLING_EMAIL;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (emailContactMechId == null) {
            inMap.clear();
            inMap.put(x.userLogin, cart.getUserLogin());
            inMap.put(x.contactMechPurposeTypeId, emailContactPurposeTypeId);
            inMap.put(x.emailAddress, emailAddress);
            inMap.put(x.partyId, partyId);
            inMap.put(x.roleTypeId, x.CUSTOMER_340f7cf7);
            inMap.put(x.verified, x.Y);  // Going to assume PayPal has taken care of this for us
            inMap.put(x.fromDate, UtilDateTime.nowTimestamp());
            try {
                outMap = dispatcher.runSync(x.createPartyEmailAddress, inMap);
                emailContactMechId = (String) outMap.get(x.contactMechId);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        cart.addContactMechId(x.ORDER_EMAIL, emailContactMechId);

        // Phone number
        String phoneNumber = decoder.get(x.PHONENUM);
        String phoneContactId = null;
        if (phoneNumber != null) {
            inMap.clear();
            if (phoneNumber.startsWith(x.str_a979ef10)) {
                // International, format is +XXX XXXXXXXX which we'll split into countryCode + contactNumber
                String[] phoneNumbers = phoneNumber.split(x.str_b858cb28);
                inMap.put(x.countryCode, StringUtil.removeNonNumeric(phoneNumbers[0]));
                inMap.put(x.contactNumber, phoneNumbers[1]);
            } else {
                // U.S., format is XXX-XXX-XXXX which we'll split into areaCode + contactNumber
                inMap.put(x.countryCode, x._1);
                String[] phoneNumbers = phoneNumber.split(x.str_3bc15c8a);
                inMap.put(x.areaCode, phoneNumbers[0]);
                inMap.put(x.contactNumber, phoneNumbers[1] + phoneNumbers[2]);
            }
            inMap.put(x.userLogin, cart.getUserLogin());
            inMap.put(x.partyId, partyId);
            try {
                outMap = dispatcher.runSync(x.createUpdatePartyTelecomNumber, inMap);
                phoneContactId = (String) outMap.get(x.contactMechId);
                cart.addContactMechId(x.PHONE_BILLING, phoneContactId);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
        }
        // Create a new Postal Address if necessary
        String postalContactId = null;
        boolean needsShippingPurpose = true;
        // if the cart for some reason already has a billing address, we'll leave it be
        boolean needsBillingPurpose = (cart.getContactMechId(x.BILLING_LOCATION) == null);
        Map<String, Object> postalMap = new HashMap<>();
        postalMap.put(x.toName, decoder.get(x.SHIPTONAME));
        postalMap.put(x.address1, decoder.get(x.SHIPTOSTREET));
        postalMap.put(x.address2, decoder.get(x.SHIPTOSTREET2));
        postalMap.put(x.city, decoder.get(x.SHIPTOCITY));
        String countryGeoId = PayPalServices.getCountryGeoIdFromGeoCode(decoder.get(x.SHIPTOCOUNTRYCODE), delegator);
        postalMap.put(x.countryGeoId, countryGeoId);
        postalMap.put(x.stateProvinceGeoId, parseStateProvinceGeoId(decoder.get(x.SHIPTOSTATE), countryGeoId, delegator));
        postalMap.put(x.postalCode, decoder.get(x.SHIPTOZIP));
        if (!newParty) {
            // We want an exact match only
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(postalMap),
                    EntityCondition.makeCondition(UtilMisc.toMap(x.attnName, null, x.directions, null, x.postalCodeExt, null, x.postalCodeGeoId, null)),
                    EntityCondition.makeCondition(x.partyId, partyId)));
            try {
                GenericValue postalMatch = DaoRegistry.getDao(delegator, x.PartyAndPostalAddress, UserLoginDao.class)
                        .findFirstByCondition(delegator, x.PartyAndPostalAddress,
                                EntityCondition.makeCondition(cond, EntityUtil.getFilterByDateExpr()), null, UtilMisc.toList(x.fromDate), false);
                if (postalMatch != null) {
                    postalContactId = postalMatch.getString(x.contactMechId);
                    List<GenericValue> postalPurposes = DaoRegistry.getDao(delegator, x.PartyContactMechPurpose, UserLoginDao.class)
                            .findListByWhere(delegator, x.PartyContactMechPurpose,
                                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, postalContactId), null, null, false, true);
                    List<Object> purposeStrings = EntityUtil.getFieldListFromEntityList(postalPurposes, x.contactMechPurposeTypeId, false);
                    if (UtilValidate.isNotEmpty(purposeStrings) && purposeStrings.contains(x.SHIPPING_LOCATION)) {
                        needsShippingPurpose = false;
                    }
                    if (needsBillingPurpose && UtilValidate.isNotEmpty(purposeStrings) && purposeStrings.contains(x.BILLING_LOCATION)) {
                        needsBillingPurpose = false;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (postalContactId == null) {
            postalMap.put(x.userLogin, cart.getUserLogin());
            postalMap.put(x.fromDate, UtilDateTime.nowTimestamp());
            try {
                outMap = dispatcher.runSync(x.createPartyPostalAddress, postalMap);
                postalContactId = (String) outMap.get(x.contactMechId);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (needsShippingPurpose || needsBillingPurpose) {
            inMap.clear();
            inMap.put(x.userLogin, cart.getUserLogin());
            inMap.put(x.contactMechId, postalContactId);
            inMap.put(x.partyId, partyId);
            try {
                if (needsShippingPurpose) {
                    inMap.put(x.contactMechPurposeTypeId, x.SHIPPING_LOCATION);
                    dispatcher.runSync(x.createPartyContactMechPurpose, inMap);
                }
                if (needsBillingPurpose) {
                    inMap.put(x.contactMechPurposeTypeId, x.BILLING_LOCATION);
                    dispatcher.runSync(x.createPartyContactMechPurpose, inMap);
                }
            } catch (GenericServiceException e) {
                // Not the end of the world, we'll carry on
                Debug.logInfo(e.getMessage(), MODULE);
            }
        }

        // Load the selected shipping method - thanks to PayPal's less than sane API all we've to work with is the shipping option label
        // that was shown to the customer
        String shipMethod = decoder.get(x.SHIPPINGOPTIONNAME);
        if (x.Calculated_Offline.equals(shipMethod)) {
            cart.setAllCarrierPartyId(x.NA);
            cart.setAllShipmentMethodTypeId(x.NO_SHIPPING);
        } else {
            String[] shipMethodSplit = shipMethod.split(x.str_fc02e199);
            cart.setAllCarrierPartyId(shipMethodSplit[0]);
            String shippingMethodTypeDesc = StringUtils.join(shipMethodSplit, x.str_fc02e199, 1, shipMethodSplit.length);
            try {
                GenericValue shipmentMethod = DaoRegistry.getDao(delegator, x.ProductStoreShipmentMethView, UserLoginDao.class)
                        .findFirstByWhere(delegator, x.ProductStoreShipmentMethView,
                                UtilMisc.toMap(x.productStoreId, cart.getProductStoreId(),
                                        x.partyId, shipMethodSplit[0],
                                        x.roleTypeId, x.CARRIER,
                                        x.description, shippingMethodTypeDesc),
                                null, null, false);
                cart.setAllShipmentMethodTypeId(shipmentMethod.getString(x.shipmentMethodTypeId));
            } catch (GenericEntityException e1) {
                Debug.logError(e1, MODULE);
            }
        }
        //Get rid of any excess ship groups
        List<CartShipInfo> shipGroups = cart.getShipGroups();
        for (int i = 1; i < shipGroups.size(); i++) {
            Map<ShoppingCartItem, BigDecimal> items = cart.getShipGroupItems(i);
            for (Map.Entry<ShoppingCartItem, BigDecimal> entry : items.entrySet()) {
                cart.positionItemToGroup(entry.getKey(), entry.getValue(), i, 0, false);
            }
        }
        cart.cleanUpShipGroups();
        cart.setAllShippingContactMechId(postalContactId);
        Map<String, Object> result = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, 0);
        if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
            return ServiceUtil.returnError((String) result.get(ModelService.ERROR_MESSAGE));
        }

        BigDecimal shippingTotal = (BigDecimal) result.get(x.shippingTotal);
        if (shippingTotal == null) {
            shippingTotal = BigDecimal.ZERO;
        }
        cart.setItemShipGroupEstimate(shippingTotal, 0);
        CheckOutHelper cho = new CheckOutHelper(dispatcher, delegator, cart);
        try {
            cho.calcAndAddTax();
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // Create the PayPal payment method
        inMap.clear();
        inMap.put(x.userLogin, cart.getUserLogin());
        inMap.put(x.partyId, partyId);
        inMap.put(x.contactMechId, postalContactId);
        inMap.put(x.fromDate, UtilDateTime.nowTimestamp());
        inMap.put(x.payerId, decoder.get(x.PAYERID));
        inMap.put(x.expressCheckoutToken, token);
        inMap.put(x.payerStatus, decoder.get(x.PAYERSTATUS));

        try {
            outMap = dispatcher.runSync(x.createPayPalPaymentMethod, inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String paymentMethodId = (String) outMap.get(x.paymentMethodId);

        cart.clearPayments();
        BigDecimal maxAmount = cart.getGrandTotal().setScale(2, RoundingMode.HALF_UP);
        cart.addPaymentAmount(paymentMethodId, maxAmount, true);

        return ServiceUtil.returnSuccess();

    }

    // Note we're not doing a lot of error checking here as this method is really only used
    // to confirm the order with PayPal, the subsequent authorizations will handle any errors
    // that may occur.
    public static Map<String, Object> doExpressCheckout(DispatchContext dctx, PayPalServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        OrderReadHelper orh = new OrderReadHelper(delegator, paymentPref.getString(x.orderId));
        Locale locale = (Locale) context.get(x.locale);

        GenericValue payPalPaymentSetting = getPaymentMethodGatewayPayPal(dctx, context, null);
        GenericValue payPalPaymentMethod = null;
        try {
            payPalPaymentMethod = paymentPref.getRelatedOne(x.PaymentMethod, false);
            payPalPaymentMethod = payPalPaymentMethod.getRelatedOne(x.PayPalPaymentMethod, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        BigDecimal processAmount = paymentPref.getBigDecimal(x.maxAmount);

        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.DoExpressCheckoutPayment);
        encoder.add(x.TOKEN, payPalPaymentMethod.getString(x.expressCheckoutToken));
        encoder.add(x.PAYMENTACTION, x.Order_1d75774c);
        encoder.add(x.PAYERID, payPalPaymentMethod.getString(x.payerId));
        // set the amount
        encoder.add(x.AMT, processAmount.setScale(2).toPlainString());
        encoder.add(x.CURRENCYCODE, orh.getCurrency());
        BigDecimal grandTotal = orh.getOrderGrandTotal();
        BigDecimal shippingTotal = orh.getShippingTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxTotal = orh.getTaxTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subTotal = grandTotal.subtract(shippingTotal).subtract(taxTotal).setScale(2, RoundingMode.HALF_UP);
        encoder.add(x.ITEMAMT, subTotal.toPlainString());
        encoder.add(x.SHIPPINGAMT, shippingTotal.toPlainString());
        encoder.add(x.TAXAMT, taxTotal.toPlainString());

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalPaymentSetting, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalUnknownError, locale));
        }

        Map<String, String> errorMessages = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errorMessages)) {
            if (errorMessages.containsKey(x._10417)) {
                // "The transaction cannot complete successfully, Instruct the customer to use an alternative payment method"
                // I've only encountered this once and there's no indication of the cause so the temporary solution is to try again
                boolean retry = context.get(x._RETRY_) == null || (Boolean) context.get(x._RETRY_);
                if (retry) {
                    context.put(x._RETRY_, false);
                    return PayPalServices.doExpressCheckout(dctx, context);
                }
            }
            return ServiceUtil.returnError(UtilMisc.toList(errorMessages.values()));
        }

        Map<String, Object> inMap = new HashMap<>();
        inMap.put(x.userLogin, userLogin);
        inMap.put(x.paymentMethodId, payPalPaymentMethod.get(x.paymentMethodId));
        inMap.put(x.transactionId, decoder.get(x.TRANSACTIONID));

        Map<String, Object> outMap = null;
        try {
            outMap = dispatcher.runSync(x.updatePayPalPaymentMethod, inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(outMap)) {
            Debug.logError(ServiceUtil.getErrorMessage(outMap), MODULE);
            return outMap;
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> doAuthorization(DispatchContext dctx, PayPalServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get(x.orderId);
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        GenericValue payPalPaymentMethod = (GenericValue) context.get(x.payPalPaymentMethod);
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, PaymentGatewayServices.AUTH_SERVICE_TYPE);
        Locale locale = (Locale) context.get(x.locale);

        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.DoAuthorization);
        encoder.add(x.TRANSACTIONID, payPalPaymentMethod.getString(x.transactionId));
        encoder.add(x.AMT, processAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        encoder.add(x.TRANSACTIONENTITY, x.Order_1d75774c);
        String currency = (String) context.get(x.currency);
        if (currency == null) {
            currency = orh.getCurrency();
        }
        encoder.add(x.CURRENCYCODE, currency);

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalUnknownError, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put(x.authResult, false);
            result.put(x.authRefNum, x.N_A);
            result.put(x.processAmount, BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put(x.authCode, error.getKey());
                result.put(x.authMessage, error.getValue());
            } else {
                result.put(x.authMessage, x.Multiple_errors_occurred_please_refer_to_the_gateway_response_messages);
                result.put(x.internalRespMsgs, errors);
            }
        } else {
            result.put(x.authResult, true);
            result.put(x.processAmount, new BigDecimal(decoder.get(x.AMT)));
            result.put(x.authRefNum, decoder.get(x.TRANSACTIONID));
        }
        //TODO: Look into possible PAYMENTSTATUS and PENDINGREASON return codes, it is unclear what should be checked for this type of transaction
        return result;
    }

    public static Map<String, Object> doCapture(DispatchContext dctx, PayPalServicesContext context) {
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal captureAmount = (BigDecimal) context.get(x.captureAmount);
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, PaymentGatewayServices.AUTH_SERVICE_TYPE);
        GenericValue authTrans = (GenericValue) context.get(x.authTrans);
        Locale locale = (Locale) context.get(x.locale);
        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.DoCapture);
        encoder.add(x.AUTHORIZATIONID, authTrans.getString(x.referenceNum));
        encoder.add(x.AMT, captureAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        encoder.add(x.CURRENCYCODE, authTrans.getString(x.currencyUomId));
        encoder.add(x.COMPLETETYPE, x.NotComplete);

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalUnknownError, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put(x.captureResult, false);
            result.put(x.captureRefNum, x.N_A);
            result.put(x.captureAmount, BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put(x.captureCode, error.getKey());
                result.put(x.captureMessage, error.getValue());
            } else {
                result.put(x.captureMessage, x.Multiple_errors_occurred_please_refer_to_the_gateway_response_messages);
                result.put(x.internalRespMsgs, errors);
            }
        } else {
            result.put(x.captureResult, true);
            result.put(x.captureAmount, new BigDecimal(decoder.get(x.AMT)));
            result.put(x.captureRefNum, decoder.get(x.TRANSACTIONID));
        }
        //TODO: Look into possible PAYMENTSTATUS and PENDINGREASON return codes, it is unclear what should be checked for this type of transaction
        return result;
    }

    public static Map<String, Object> doVoid(DispatchContext dctx, PayPalServicesContext context) {
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        Locale locale = (Locale) context.get(x.locale);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalPaymentGatewayConfigCannotFind, locale));
        }
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.DoVoid);
        encoder.add(x.AUTHORIZATIONID, authTrans.getString(x.referenceNum));
        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalUnknownError, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put(x.releaseResult, false);
            result.put(x.releaseRefNum, authTrans.getString(x.referenceNum));
            result.put(x.releaseAmount, BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put(x.releaseCode, error.getKey());
                result.put(x.releaseMessage, error.getValue());
            } else {
                result.put(x.releaseMessage, x.Multiple_errors_occurred_please_refer_to_the_gateway_response_messages);
                result.put(x.internalRespMsgs, errors);
            }
        } else {
            result.put(x.releaseResult, true);
            // PayPal voids the entire order amount minus any captures, that's a little difficult to figure out here
            // so until further testing proves we should do otherwise I'm just going to return requested void amount
            result.put(x.releaseAmount, context.get(x.releaseAmount));
            result.put(x.releaseRefNum, decoder.get(x.AUTHORIZATIONID));
        }
        return result;
    }

    public static Map<String, Object> doRefund (DispatchContext dctx, PayPalServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalPaymentGatewayConfigCannotFind, locale));
        }
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue captureTrans = PaymentGatewayServices.getCaptureTransaction(orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get(x.refundAmount);
        NVPEncoder encoder = new NVPEncoder();
        encoder.add(x.METHOD, x.RefundTransaction);
        encoder.add(x.TRANSACTIONID, captureTrans.getString(x.referenceNum));
        encoder.add(x.REFUNDTYPE, x.Partial);
        encoder.add(x.CURRENCYCODE, captureTrans.getString(x.currencyUomId));
        encoder.add(x.AMT, refundAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        encoder.add(x.NOTE, x.Order_f1e486fe + orderPaymentPreference.getString(x.orderId));
        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPayPalUnknownError, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put(x.refundResult, false);
            result.put(x.refundRefNum, captureTrans.getString(x.referenceNum));
            result.put(x.refundAmount, BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put(x.refundCode, error.getKey());
                result.put(x.refundMessage, error.getValue());
            } else {
                result.put(x.refundMessage, x.Multiple_errors_occurred_please_refer_to_the_gateway_response_messages);
                result.put(x.internalRespMsgs, errors);
            }
        } else {
            result.put(x.refundResult, true);
            result.put(x.refundAmount, new BigDecimal(decoder.get(x.GROSSREFUNDAMT)));
            result.put(x.refundRefNum, decoder.get(x.REFUNDTRANSACTIONID));
        }
        return result;
    }

    private static GenericValue getPaymentMethodGatewayPayPal(DispatchContext dctx, PayPalServicesContext context, String paymentServiceTypeEnumId) {
        Delegator delegator = dctx.getDelegator();
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        GenericValue payPalGatewayConfig = null;

        if (paymentGatewayConfigId == null) {
            String productStoreId = null;
            GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
            if (orderPaymentPreference != null) {
                OrderReadHelper orh = new OrderReadHelper(delegator, orderPaymentPreference.getString(x.orderId));
                productStoreId = orh.getProductStoreId();
            } else {
                ShoppingCart cart = (ShoppingCart) context.get(x.cart);
                if (cart != null) {
                    productStoreId = cart.getProductStoreId();
                }
            }
            if (productStoreId != null) {
                GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, x.EXT_PAYPAL, paymentServiceTypeEnumId, true);
                if (payPalPaymentSetting != null) {
                    paymentGatewayConfigId = payPalPaymentSetting.getString(x.paymentGatewayConfigId);
                }
            }
        }
        if (paymentGatewayConfigId != null) {
            try {
                payPalGatewayConfig = DaoRegistry.getDao(delegator, x.PaymentGatewayPayPal, UserLoginDao.class)
                        .findOne(delegator, x.PaymentGatewayPayPal,
                                UtilMisc.toMap(x.paymentGatewayConfigId, paymentGatewayConfigId), true);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return payPalGatewayConfig;
    }

    private static NVPDecoder sendNVPRequest(GenericValue payPalConfig, NVPEncoder encoder) throws PayPalException {
        NVPCallerServices caller = new NVPCallerServices();
        try {
            APIProfile profile = ProfileFactory.createSignatureAPIProfile();
            profile.setAPIUsername(payPalConfig.getString(x.apiUserName));
            profile.setAPIPassword(payPalConfig.getString(x.apiPassword));
            profile.setSignature(payPalConfig.getString(x.apiSignature));
            profile.setEnvironment(payPalConfig.getString(x.apiEnvironment));
            caller.setAPIProfile(profile);
        } catch (PayPalException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        String requestMessage = encoder.encode();
        String responseMessage = caller.call(requestMessage);

        NVPDecoder decoder = new NVPDecoder();
        decoder.decode(responseMessage);
        if (!x.Success.equals(decoder.get(x.ACK))) {
            Debug.logError(x.A_response_other_than_success_was_received_from_PayPal + responseMessage, MODULE);
        }

        return decoder;
    }

    private static String getCountryGeoIdFromGeoCode(String geoCode, Delegator delegator) {
        String geoId = null;
        try {
            GenericValue countryGeo = DaoRegistry.getDao(delegator, x.Geo, UserLoginDao.class)
                    .findFirstByWhere(delegator, x.Geo,
                            UtilMisc.toMap(x.geoTypeId, x.COUNTRY, x.geoCode, geoCode), null, null, true);
            if (countryGeo != null) {
                geoId = countryGeo.getString(x.geoId);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return geoId;
    }

    private static String parseStateProvinceGeoId(String payPalShipToState, String countryGeoId, Delegator delegator) {
        String lookupField = x.geoName;
        List<EntityCondition> conditionList = new LinkedList<>();
        conditionList.add(EntityCondition.makeCondition(x.geoAssocTypeId, x.REGIONS));
        if (x.USA.equals(countryGeoId) || x.CAN.equals(countryGeoId)) {
            // PayPal returns two letter code for US and Canadian States/Provinces
            String geoTypeId = x.USA.equals(countryGeoId) ? x.STATE : x.PROVINCE;
            conditionList.add(EntityCondition.makeCondition(x.geoTypeId, geoTypeId));
            lookupField = x.geoCode;
        }
        conditionList.add(EntityCondition.makeCondition(x.geoIdFrom, countryGeoId));
        conditionList.add(EntityCondition.makeCondition(lookupField, payPalShipToState));
        EntityCondition cond = EntityCondition.makeCondition(conditionList);
        GenericValue geoAssocAndGeoTo = null;
        try {
            geoAssocAndGeoTo = DaoRegistry.getDao(delegator, x.GeoAssocAndGeoTo, UserLoginDao.class)
                    .findFirstByCondition(delegator, x.GeoAssocAndGeoTo, cond, null, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (geoAssocAndGeoTo != null) {
            return geoAssocAndGeoTo.getString(x.geoId);
        }
        return null;
    }

    @SuppressWarnings(x.serial)
    public static class TokenWrapper implements Serializable {
        String theString;
        public TokenWrapper(String theString) {
            this.theString = theString;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof TokenWrapper)) return false;
            TokenWrapper other = (TokenWrapper) o;
            return theString.equals(other.theString);
        }
        @Override
        public int hashCode() {
            return theString.hashCode();
        }
    }
}
