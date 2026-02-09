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
package org.apache.ofbiz.accounting.payment;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PaymentMethodServicesContext;
/**
 * Services for Payment maintenance
 */
public class PaymentMethodServices {

    private static final String MODULE = PaymentMethodServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";
    private static final String RES_ERROR = "AccountingUiLabels";

    /**
     * Deletes a PaymentMethod entity according to the parameters passed in the context
     * <b>security check</b>: userLogin partyId must equal paymentMethod partyId, or must have PAY_INFO_DELETE permission
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> deletePaymentMethod(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        // never delete a PaymentMethod, just put a to date on the link to the party
        String paymentMethodId = (String) context.get(x.paymentMethodId);
        GenericValue paymentMethod = null;

        try {
            paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingPaymentMethodCannotBeDeleted",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingPaymentMethodCannotBeDeleted",
                    UtilMisc.toMap("errorString", ""), locale));
        }

        // <b>security check</b>: userLogin partyId must equal paymentMethod partyId, or must have PAY_INFO_DELETE permission
        if (paymentMethod.get(x.partyId) == null || !paymentMethod.getString(x.partyId).equals(userLogin.getString(x.partyId))) {
            if (!security.hasEntityPermission("PAY_INFO", "_DELETE", userLogin)
                    && !security.hasEntityPermission("ACCOUNTING", "_DELETE", userLogin)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingPaymentMethodNoPermissionToDelete", locale));
            }
        }

        paymentMethod.set(x.thruDate, now);
        try {
            paymentMethod.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingPaymentMethodCannotBeDeletedWriteFailure",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> makeExpireDate(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        String expMonth = (String) context.get(x.expMonth);
        String expYear = (String) context.get(x.expYear);

        StringBuilder expDate = new StringBuilder();
        expDate.append(expMonth);
        expDate.append("/");
        expDate.append(expYear);
        result.put("expireDate", expDate.toString());
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Creates CreditCard and PaymentMethod entities according to the parameters passed in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PAY_INFO_CREATE permission
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createCreditCard(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_CREATE", "ACCOUNTING", "_CREATE");

        if (!result.isEmpty()) {
            return result;
        }

        // do some more complicated/critical validation...
        List<String> messages = new LinkedList<>();

        // first remove all spaces from the credit card number
        context.put(x.cardNumber, StringUtil.removeSpaces((String) context.get(x.cardNumber)));
        if (!UtilValidate.isCardMatch((String) context.get(x.cardType), (String) context.get(x.cardNumber))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, "AccountingCreditCardNumberInvalid",
                            UtilMisc.toMap("cardType", (String) context.get(x.cardType),
                                    "validCardType", UtilValidate.getCardType((String) context.get(x.cardNumber))), locale));
        }

        if (!UtilValidate.isDateAfterToday((String) context.get(x.expireDate))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, "AccountingCreditCardExpireDateBeforeToday",
                            UtilMisc.toMap("expireDate", (String) context.get(x.expireDate)), locale));
        }

        if (!messages.isEmpty()) {
            return ServiceUtil.returnError(messages);
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue("PaymentMethod");

        toBeStored.add(newPm);
        GenericValue newCc = delegator.makeValue("CreditCard");

        toBeStored.add(newCc);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId("PaymentMethod");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingCreditCardCreateIdGenerationFailure", locale));
            }
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.description, context.get(x.description));
        newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
        newPm.set(x.thruDate, context.get(x.thruDate));
        newCc.set(x.companyNameOnCard, context.get(x.companyNameOnCard));
        newCc.set(x.titleOnCard, context.get(x.titleOnCard));
        newCc.set(x.firstNameOnCard, context.get(x.firstNameOnCard));
        newCc.set(x.middleNameOnCard, context.get(x.middleNameOnCard));
        newCc.set(x.lastNameOnCard, context.get(x.lastNameOnCard));
        newCc.set(x.suffixOnCard, context.get(x.suffixOnCard));
        newCc.set(x.cardType, context.get(x.cardType));
        newCc.set(x.cardNumber, context.get(x.cardNumber));
        newCc.set(x.expireDate, context.get(x.expireDate));

        newPm.set(x.paymentMethodId, newPmId);
        newPm.set(x.paymentMethodTypeId, "CREDIT_CARD");
        newCc.set(x.paymentMethodId, newPmId);

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId) && !"_NEW_".equals(contactMechId)) {
            // set the contactMechId on the credit card
            newCc.set(x.contactMechId, context.get(x.contactMechId));
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId",
                                contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (newPartyContactMechPurpose != null) {
            toBeStored.add(newPartyContactMechPurpose);
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingCreditCardCreateWriteFailure", locale) + e.getMessage());
        }

        result.put("paymentMethodId", newCc.getString(x.paymentMethodId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates CreditCard and PaymentMethod entities according to the parameters passed in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PAY_INFO_UPDATE permission
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateCreditCard(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_UPDATE",
                "ACCOUNTING", "_UPDATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        GenericValue paymentMethod = null;
        GenericValue newPm = null;
        GenericValue creditCard = null;
        GenericValue newCc = null;
        String paymentMethodId = (String) context.get(x.paymentMethodId);

        try {
            creditCard = EntityQuery.use(delegator).from("CreditCard").where("paymentMethodId", paymentMethodId).queryOne();
            paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingCreditCardUpdateReadFailure", locale) + e.getMessage());
        }

        if (creditCard == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingCreditCardUpdateWithPaymentMethodId", locale) + paymentMethodId);
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission("PAY_INFO", "_UPDATE",
                userLogin) && !security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingCreditCardUpdateWithoutPermission", UtilMisc.toMap("partyId", partyId,
                            "paymentMethodId", paymentMethodId), locale));
        }

        // do some more complicated/critical validation...
        List<String> messages = new LinkedList<>();

        // first remove all spaces from the credit card number
        String updatedCardNumber = StringUtil.removeSpaces((String) context.get(x.cardNumber));
        if (updatedCardNumber.startsWith("*")) {
            // get the masked card number from the db
            String origCardNumber = creditCard.getString(x.cardNumber);
            int cardLength = origCardNumber.length() - 4;
            // use builder for better performance
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < cardLength; i++) {
                builder.append("*");
            }
            String origMaskedNumber = builder.append(origCardNumber.substring(cardLength)).toString();

            // compare the two masked numbers
            if (updatedCardNumber.equals(origMaskedNumber)) {
                updatedCardNumber = origCardNumber;
            }
        }
        context.put(x.cardNumber, updatedCardNumber);

        if (!UtilValidate.isCardMatch((String) context.get(x.cardType), (String) context.get(x.cardNumber))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, "AccountingCreditCardNumberInvalid",
                            UtilMisc.toMap("cardType", (String) context.get(x.cardType),
                                    "validCardType", UtilValidate.getCardType((String) context.get(x.cardNumber))), locale));
        }

        if (!UtilValidate.isDateAfterToday((String) context.get(x.expireDate))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, "AccountingCreditCardExpireDateBeforeToday",
                            UtilMisc.toMap("expireDate", (String) context.get(x.expireDate)), locale));
        }

        if (!messages.isEmpty()) {
            return ServiceUtil.returnError(messages);
        }

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newCc = GenericValue.create(creditCard);
        toBeStored.add(newCc);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId("PaymentMethod");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingCreditCardUpdateIdGenerationFailure", locale));

        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.fromDate, context.get(x.fromDate), false);
        newPm.set(x.description, context.get(x.description));
        // The following check is needed to avoid to reactivate an expired pm
        if (newPm.get(x.thruDate) == null) {
            newPm.set(x.thruDate, context.get(x.thruDate));
        }
        newCc.set(x.companyNameOnCard, context.get(x.companyNameOnCard));
        newCc.set(x.titleOnCard, context.get(x.titleOnCard));
        newCc.set(x.firstNameOnCard, context.get(x.firstNameOnCard));
        newCc.set(x.middleNameOnCard, context.get(x.middleNameOnCard));
        newCc.set(x.lastNameOnCard, context.get(x.lastNameOnCard));
        newCc.set(x.suffixOnCard, context.get(x.suffixOnCard));

        newCc.set(x.cardType, context.get(x.cardType));
        newCc.set(x.cardNumber, context.get(x.cardNumber));
        newCc.set(x.expireDate, context.get(x.expireDate));

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId) && !"_NEW_".equals(contactMechId)) {
            // set the contactMechId on the credit card
            newCc.set(x.contactMechId, contactMechId);
        }

        if (!newCc.equals(creditCard) || !newPm.equals(paymentMethod)) {
            newPm.set(x.paymentMethodId, newPmId);
            newCc.set(x.paymentMethodId, newPmId);

            newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
            isModified = true;
        }

        if (UtilValidate.isNotEmpty(contactMechId) && !"_NEW_".equals(contactMechId)) {

            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId",
                                contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (isModified) {
            if (newPartyContactMechPurpose != null) {
                toBeStored.add(newPartyContactMechPurpose);
            }

            // set thru date on old paymentMethod
            paymentMethod.set(x.thruDate, now);
            toBeStored.add(paymentMethod);

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingCreditCardUpdateWriteFailure", locale) + e.getMessage());
            }
        } else {
            result.put("paymentMethodId", paymentMethodId);
            result.put("oldPaymentMethodId", paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            if (contactMechId == null || !"_NEW_".equals(contactMechId)) {
                result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                        "AccountingNoChangesMadeNotUpdatingCreditCard", locale));
            }

            return result;
        }

        result.put("oldPaymentMethodId", paymentMethodId);
        result.put("paymentMethodId", newCc.getString(x.paymentMethodId));

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> clearCreditCardData(DispatchContext dctx, PaymentMethodServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String paymentMethodId = (String) context.get(x.paymentMethodId);

        // get the cc object
        Delegator delegator = dctx.getDelegator();
        GenericValue creditCard;
        try {
            creditCard = EntityQuery.use(delegator).from("CreditCard").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // clear the info and store it
        creditCard.set(x.cardNumber, "0000000000000000"); // set so it doesn't blow up in UIs
        creditCard.set(x.expireDate, "01/1970"); // same here
        try {
            delegator.store(creditCard);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // expire the payment method
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> expireCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin,
                "paymentMethodId", paymentMethodId);
        Map<String, Object> expireResp;
        try {
            expireResp = dispatcher.runSync("deletePaymentMethod", expireCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(expireResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(expireResp));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createGiftCard(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_CREATE", "ACCOUNTING", "_CREATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue("PaymentMethod");
        toBeStored.add(newPm);
        GenericValue newGc = delegator.makeValue("GiftCard");
        toBeStored.add(newGc);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId("PaymentMethod");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingGiftCardCannotBeCreated", locale));
            }
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
        newPm.set(x.thruDate, context.get(x.thruDate));
        newPm.set(x.description, context.get(x.description));

        newGc.set(x.cardNumber, context.get(x.cardNumber));
        newGc.set(x.pinNumber, context.get(x.pinNumber));
        newGc.set(x.expireDate, context.get(x.expireDate));

        newPm.set(x.paymentMethodId, newPmId);
        newPm.set(x.paymentMethodTypeId, "GIFT_CARD");
        newGc.set(x.paymentMethodId, newPmId);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCardCannotBeCreatedWriteFailure",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        result.put("paymentMethodId", newGc.getString(x.paymentMethodId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> updateGiftCard(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_UPDATE", "ACCOUNTING", "_UPDATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        GenericValue paymentMethod = null;
        GenericValue newPm = null;
        GenericValue giftCard = null;
        GenericValue newGc = null;
        String paymentMethodId = (String) context.get(x.paymentMethodId);

        try {
            giftCard = EntityQuery.use(delegator).from("GiftCard").where("paymentMethodId", paymentMethodId).queryOne();
            paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCardCannotBeUpdated",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (giftCard == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCardCannotBeUpdated",
                    UtilMisc.toMap("errorString", paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission("PAY_INFO", "_UPDATE", userLogin)
                && !security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCardPartyNotAuthorized",
                    UtilMisc.toMap("partyId", partyId, "paymentMethodId", paymentMethodId), locale));
        }


        // card number (masked)
        String cardNumber = StringUtil.removeSpaces((String) context.get(x.cardNumber));
        if (cardNumber.startsWith("*")) {
            // get the masked card number from the db
            String origCardNumber = giftCard.getString(x.cardNumber);
            StringBuilder origMaskedNumber = new StringBuilder("");
            int cardLength = origCardNumber.length() - 4;
            if (cardLength > 0) {
                for (int i = 0; i < cardLength; i++) {
                    origMaskedNumber.append("*");
                }
                origMaskedNumber.append(origCardNumber.substring(cardLength));
            } else {
                origMaskedNumber.append(origCardNumber);
            }

            // compare the two masked numbers
            if (cardNumber.equals(origMaskedNumber.toString())) {
                cardNumber = origCardNumber;
            }
        }
        context.put(x.cardNumber, cardNumber);

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newGc = GenericValue.create(giftCard);
        toBeStored.add(newGc);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId("PaymentMethod");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCardCannotBeCreated", locale));
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.fromDate, context.get(x.fromDate), false);
        newPm.set(x.thruDate, context.get(x.thruDate));
        newPm.set(x.description, context.get(x.description));

        newGc.set(x.cardNumber, context.get(x.cardNumber));
        newGc.set(x.pinNumber, context.get(x.pinNumber));
        newGc.set(x.expireDate, context.get(x.expireDate));

        if (!newGc.equals(giftCard) || !newPm.equals(paymentMethod)) {
            newPm.set(x.paymentMethodId, newPmId);
            newGc.set(x.paymentMethodId, newPmId);

            newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
            isModified = true;
        }

        if (isModified) {
            // set thru date on old paymentMethod
            paymentMethod.set(x.thruDate, now);
            toBeStored.add(paymentMethod);

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingEftAccountCannotBeUpdated",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        } else {
            result.put("paymentMethodId", paymentMethodId);
            result.put("oldPaymentMethodId", paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                    "AccountingNoChangesMadeNotUpdatingEftAccount", locale));

            return result;
        }

        result.put("paymentMethodId", newGc.getString(x.paymentMethodId));
        result.put("oldPaymentMethodId", paymentMethodId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Creates EftAccount and PaymentMethod entities according to the parameters passed in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PAY_INFO_CREATE permission
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createEftAccount(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_CREATE", "ACCOUNTING", "_CREATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue("PaymentMethod");

        toBeStored.add(newPm);
        GenericValue newEa = delegator.makeValue("EftAccount");

        toBeStored.add(newEa);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId("PaymentMethod");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingEftAccountCannotBeCreated", locale));
            }
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
        newPm.set(x.thruDate, context.get(x.thruDate));
        newPm.set(x.description, context.get(x.description));
        newEa.set(x.bankName, context.get(x.bankName));
        newEa.set(x.routingNumber, context.get(x.routingNumber));
        newEa.set(x.accountType, context.get(x.accountType));
        newEa.set(x.accountNumber, context.get(x.accountNumber));
        newEa.set(x.nameOnAccount, context.get(x.nameOnAccount));
        newEa.set(x.companyNameOnAccount, context.get(x.companyNameOnAccount));
        newEa.set(x.contactMechId, context.get(x.contactMechId));

        newPm.set(x.paymentMethodId, newPmId);
        newPm.set(x.paymentMethodTypeId, "EFT_ACCOUNT");
        newEa.set(x.paymentMethodId, newPmId);

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                    UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId,
                            "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (newPartyContactMechPurpose != null) {
            toBeStored.add(newPartyContactMechPurpose);
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingEftAccountCannotBeCreatedWriteFailure",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        result.put("paymentMethodId", newEa.getString(x.paymentMethodId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates EftAccount and PaymentMethod entities according to the parameters passed in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PAY_INFO_UPDATE permission
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateEftAccount(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_UPDATE", "ACCOUNTING", "_UPDATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        GenericValue paymentMethod = null;
        GenericValue newPm = null;
        GenericValue eftAccount = null;
        GenericValue newEa = null;
        String paymentMethodId = (String) context.get(x.paymentMethodId);

        try {
            eftAccount = EntityQuery.use(delegator).from("EftAccount").where("paymentMethodId", paymentMethodId).queryOne();
            paymentMethod =
                EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingEftAccountCannotBeUpdatedReadFailure",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (eftAccount == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingEftAccountCannotBeUpdated",
                    UtilMisc.toMap("errorString", paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission("PAY_INFO", "_UPDATE", userLogin)
                && !security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingEftAccountCannotBeUpdated",
                    UtilMisc.toMap("partyId", partyId, "paymentMethodId", paymentMethodId), locale));
        }

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newEa = GenericValue.create(eftAccount);
        toBeStored.add(newEa);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId("PaymentMethod");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingEftAccountCannotBeCreated", locale));
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.fromDate, context.get(x.fromDate), false);
        newPm.set(x.thruDate, context.get(x.thruDate));
        newPm.set(x.description, context.get(x.description));
        newEa.set(x.bankName, context.get(x.bankName));
        newEa.set(x.routingNumber, context.get(x.routingNumber));
        newEa.set(x.accountType, context.get(x.accountType));
        newEa.set(x.accountNumber, context.get(x.accountNumber));
        newEa.set(x.nameOnAccount, context.get(x.nameOnAccount));
        newEa.set(x.companyNameOnAccount, context.get(x.companyNameOnAccount));
        newEa.set(x.contactMechId, context.get(x.contactMechId));

        if (!newEa.equals(eftAccount) || !newPm.equals(paymentMethod)) {
            newPm.set(x.paymentMethodId, newPmId);
            newEa.set(x.paymentMethodId, newPmId);
            newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
            isModified = true;
        }

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId,
                                "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (isModified) {
            // Debug.logInfo("yes, is modified", MODULE);
            if (newPartyContactMechPurpose != null) {
                toBeStored.add(newPartyContactMechPurpose);
            }

            // set thru date on old paymentMethod
            paymentMethod.set(x.thruDate, now);
            toBeStored.add(paymentMethod);

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingEftAccountCannotBeUpdated",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        } else {
            result.put("paymentMethodId", paymentMethodId);
            result.put("oldPaymentMethodId", paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                    "AccountingNoChangesMadeNotUpdatingEftAccount", locale));

            return result;
        }

        result.put("paymentMethodId", newEa.getString(x.paymentMethodId));
        result.put("oldPaymentMethodId", paymentMethodId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> createCheckAccount(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_CREATE", "ACCOUNTING", "_CREATE");
        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue("PaymentMethod");
        toBeStored.add(newPm);

        GenericValue newCa = delegator.makeValue("CheckAccount");

        toBeStored.add(newCa);
        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId("PaymentMethod");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "AccountingCheckNotAdded", locale));
            }
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.description, context.get(x.description));
        newPm.set(x.paymentMethodTypeId, context.get(x.paymentMethodTypeId));
        newPm.set(x.fromDate, now);
        newPm.set(x.paymentMethodId, newPmId);

        newCa.set(x.bankName, context.get(x.bankName));
        newCa.set(x.routingNumber, context.get(x.routingNumber));
        newCa.set(x.accountType, context.get(x.accountType));
        newCa.set(x.accountNumber, context.get(x.accountNumber));
        newCa.set(x.nameOnAccount, context.get(x.nameOnAccount));
        newCa.set(x.companyNameOnAccount, context.get(x.companyNameOnAccount));
        newCa.set(x.contactMechId, context.get(x.contactMechId));
        newCa.set(x.paymentMethodId, newPmId);

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId,
                                "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (newPartyContactMechPurpose != null) {
            toBeStored.add(newPartyContactMechPurpose);
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "AccountingCheckNotAdded",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        result.put("paymentMethodId", newPm.getString(x.paymentMethodId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> updateCheckAccount(DispatchContext ctx, PaymentMethodServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PAY_INFO", "_UPDATE", "ACCOUNTING", "_UPDATE");

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        GenericValue paymentMethod = null;
        GenericValue newPm = null;
        GenericValue checkAccount = null;
        GenericValue newCa = null;
        String paymentMethodId = (String) context.get(x.paymentMethodId);

        try {
            checkAccount = EntityQuery.use(delegator).from("CheckAccount").where("paymentMethodId", paymentMethodId).queryOne();
            paymentMethod =
                    EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingCheckAccountCannotBeUpdated",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (checkAccount == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingCheckAccountCannotBeUpdated",
                    UtilMisc.toMap("errorString", paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission("PAY_INFO", "_UPDATE", userLogin)
                && !security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingCheckAccountCannotBeUpdated",
                    UtilMisc.toMap("partyId", partyId, "paymentMethodId", paymentMethodId), locale));
        }

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newCa = GenericValue.create(checkAccount);
        toBeStored.add(newCa);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId("PaymentMethod");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingCheckAccountCannotBeUpdated", locale));
        }

        newPm.set(x.partyId, partyId);
        newPm.set(x.paymentMethodTypeId, context.get(x.paymentMethodTypeId));
        newPm.set(x.fromDate, context.get(x.fromDate), false);
        newPm.set(x.description, context.get(x.description));
        newCa.set(x.bankName, context.get(x.bankName));
        newCa.set(x.routingNumber, context.get(x.routingNumber));
        newCa.set(x.accountType, context.get(x.accountType));
        newCa.set(x.accountNumber, context.get(x.accountNumber));
        newCa.set(x.nameOnAccount, context.get(x.nameOnAccount));
        newCa.set(x.companyNameOnAccount, context.get(x.companyNameOnAccount));
        newCa.set(x.contactMechId, context.get(x.contactMechId));

        if (!newCa.equals(checkAccount) || !newPm.equals(paymentMethod)) {
            newPm.set(x.paymentMethodId, newPmId);
            newCa.set(x.paymentMethodId, newPmId);
            newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
            isModified = true;
        }

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = "BILLING_LOCATION";

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                        .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId).queryList();
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "contactFromDate", "contactThruDate", true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, "purposeFromDate", "purposeThruDate", true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId,
                                "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", now));
            }
        }

        if (isModified) {
            // Debug.logInfo("yes, is modified", MODULE);
            if (newPartyContactMechPurpose != null) {
                toBeStored.add(newPartyContactMechPurpose);
            }

            // set thru date on old paymentMethod
            paymentMethod.set(x.thruDate, now);
            toBeStored.add(paymentMethod);

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingCheckAccountCannotBeUpdated",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        } else {
            result.put("paymentMethodId", paymentMethodId);
            result.put("oldPaymentMethodId", paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE,
                    UtilProperties.getMessage(RESOURCE, "AccountingCheckAccountCannotBeUpdated", locale));

            return result;
        }

        result.put("paymentMethodId", newCa.getString(x.paymentMethodId));
        result.put("oldPaymentMethodId", paymentMethodId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
