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
import org.apache.ofbiz.persistence.dao.CheckAccountDao;
import org.apache.ofbiz.persistence.dao.CreditCardDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.EftAccountDao;
import org.apache.ofbiz.persistence.dao.GiftCardDao;
import org.apache.ofbiz.persistence.dao.PartyContactMechPurposeDao;
import org.apache.ofbiz.persistence.dao.PaymentMethodDao;
import org.apache.ofbiz.persistence.entity.CheckAccountEntity;
import org.apache.ofbiz.persistence.entity.CreditCardEntity;
import org.apache.ofbiz.persistence.entity.EftAccountEntity;
import org.apache.ofbiz.persistence.entity.GiftCardEntity;
import org.apache.ofbiz.persistence.entity.PaymentMethodEntity;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PaymentMethodServicesContext;
/**
 * Services for Payment maintenance
 */
public class PaymentMethodServices {

    private static final String MODULE = PaymentMethodServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final String RES_ERROR = x.AccountingUiLabels;

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
            paymentMethod = getPaymentMethodValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingPaymentMethodCannotBeDeleted,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingPaymentMethodCannotBeDeleted,
                    UtilMisc.toMap(x.errorString, x.emptyString), locale));
        }

        // <b>security check</b>: userLogin partyId must equal paymentMethod partyId, or must have PAY_INFO_DELETE permission
        if (paymentMethod.get(x.partyId) == null || !paymentMethod.getString(x.partyId).equals(userLogin.getString(x.partyId))) {
            if (!security.hasEntityPermission(x.PAY_INFO, x.DELETE, userLogin)
                    && !security.hasEntityPermission(x.ACCOUNTING, x.DELETE, userLogin)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingPaymentMethodNoPermissionToDelete, locale));
            }
        }

        paymentMethod.set(x.thruDate, now);
        try {
            paymentMethod.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingPaymentMethodCannotBeDeletedWriteFailure,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
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
        expDate.append(x.str_42099b4a);
        expDate.append(expYear);
        result.put(x.expireDate, expDate.toString());
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.CREATE, x.ACCOUNTING, x.CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        // do some more complicated/critical validation...
        List<String> messages = new LinkedList<>();

        // first remove all spaces from the credit card number
        context.put(x.cardNumber, StringUtil.removeSpaces((String) context.get(x.cardNumber)));
        if (!UtilValidate.isCardMatch((String) context.get(x.cardType), (String) context.get(x.cardNumber))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, x.AccountingCreditCardNumberInvalid,
                            UtilMisc.toMap(x.cardType, (String) context.get(x.cardType),
                                    x.validCardType, UtilValidate.getCardType((String) context.get(x.cardNumber))), locale));
        }

        if (!UtilValidate.isDateAfterToday((String) context.get(x.expireDate))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, x.AccountingCreditCardExpireDateBeforeToday,
                            UtilMisc.toMap(x.expireDate, (String) context.get(x.expireDate)), locale));
        }

        if (!messages.isEmpty()) {
            return ServiceUtil.returnError(messages);
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue(x.PaymentMethod);

        toBeStored.add(newPm);
        GenericValue newCc = delegator.makeValue(x.CreditCard);

        toBeStored.add(newCc);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId(x.PaymentMethod);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingCreditCardCreateIdGenerationFailure, locale));
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
        newPm.set(x.paymentMethodTypeId, x.CREDIT_CARD);
        newCc.set(x.paymentMethodId, newPmId);

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId) && !x.NEW.equals(contactMechId)) {
            // set the contactMechId on the credit card
            newCc.set(x.contactMechId, context.get(x.contactMechId));
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId, x.contactMechPurposeTypeId,
                                contactMechPurposeTypeId, x.fromDate, now));
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
                    x.AccountingCreditCardCreateWriteFailure, locale) + e.getMessage());
        }

        result.put(x.paymentMethodId, newCc.getString(x.paymentMethodId));
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.UPDATE_f97c688e,
                x.ACCOUNTING, x.UPDATE_f97c688e);

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
            creditCard = getCreditCardValue(delegator, paymentMethodId);
            paymentMethod = getPaymentMethodValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCreditCardUpdateReadFailure, locale) + e.getMessage());
        }

        if (creditCard == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCreditCardUpdateWithPaymentMethodId, locale) + paymentMethodId);
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission(x.PAY_INFO, x.UPDATE_f97c688e,
                userLogin) && !security.hasEntityPermission(x.ACCOUNTING, x.UPDATE_f97c688e, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCreditCardUpdateWithoutPermission, UtilMisc.toMap(x.partyId, partyId,
                            x.paymentMethodId, paymentMethodId), locale));
        }

        // do some more complicated/critical validation...
        List<String> messages = new LinkedList<>();

        // first remove all spaces from the credit card number
        String updatedCardNumber = StringUtil.removeSpaces((String) context.get(x.cardNumber));
        if (updatedCardNumber.startsWith(x.str_df58248c)) {
            // get the masked card number from the db
            String origCardNumber = creditCard.getString(x.cardNumber);
            int cardLength = origCardNumber.length() - 4;
            // use builder for better performance
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < cardLength; i++) {
                builder.append(x.str_df58248c);
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
                    UtilProperties.getMessage(RESOURCE, x.AccountingCreditCardNumberInvalid,
                            UtilMisc.toMap(x.cardType, (String) context.get(x.cardType),
                                    x.validCardType, UtilValidate.getCardType((String) context.get(x.cardNumber))), locale));
        }

        if (!UtilValidate.isDateAfterToday((String) context.get(x.expireDate))) {
            messages.add(
                    UtilProperties.getMessage(RESOURCE, x.AccountingCreditCardExpireDateBeforeToday,
                            UtilMisc.toMap(x.expireDate, (String) context.get(x.expireDate)), locale));
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
            newPmId = delegator.getNextSeqId(x.PaymentMethod);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCreditCardUpdateIdGenerationFailure, locale));

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

        if (UtilValidate.isNotEmpty(contactMechId) && !x.NEW.equals(contactMechId)) {
            // set the contactMechId on the credit card
            newCc.set(x.contactMechId, contactMechId);
        }

        if (!newCc.equals(creditCard) || !newPm.equals(paymentMethod)) {
            newPm.set(x.paymentMethodId, newPmId);
            newCc.set(x.paymentMethodId, newPmId);

            newPm.set(x.fromDate, (context.get(x.fromDate) != null ? context.get(x.fromDate) : now));
            isModified = true;
        }

        if (UtilValidate.isNotEmpty(contactMechId) && !x.NEW.equals(contactMechId)) {

            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId, x.contactMechPurposeTypeId,
                                contactMechPurposeTypeId, x.fromDate, now));
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
                        x.AccountingCreditCardUpdateWriteFailure, locale) + e.getMessage());
            }
        } else {
            result.put(x.paymentMethodId, paymentMethodId);
            result.put(x.oldPaymentMethodId, paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            if (contactMechId == null || !x.NEW.equals(contactMechId)) {
                result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                        x.AccountingNoChangesMadeNotUpdatingCreditCard, locale));
            }

            return result;
        }

        result.put(x.oldPaymentMethodId, paymentMethodId);
        result.put(x.paymentMethodId, newCc.getString(x.paymentMethodId));

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
            creditCard = getCreditCardValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // clear the info and store it
        creditCard.set(x.cardNumber, x._0000000000000000); // set so it doesn't blow up in UIs
        creditCard.set(x.expireDate, x._01_1970); // same here
        try {
            delegator.store(creditCard);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // expire the payment method
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> expireCtx = UtilMisc.<String, Object>toMap(x.userLogin, userLogin,
                x.paymentMethodId, paymentMethodId);
        Map<String, Object> expireResp;
        try {
            expireResp = dispatcher.runSync(x.deletePaymentMethod, expireCtx);
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.CREATE, x.ACCOUNTING, x.CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue(x.PaymentMethod);
        toBeStored.add(newPm);
        GenericValue newGc = delegator.makeValue(x.GiftCard);
        toBeStored.add(newGc);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId(x.PaymentMethod);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCardCannotBeCreated, locale));
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
        newPm.set(x.paymentMethodTypeId, x.GIFT_CARD);
        newGc.set(x.paymentMethodId, newPmId);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCardCannotBeCreatedWriteFailure,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        result.put(x.paymentMethodId, newGc.getString(x.paymentMethodId));
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.UPDATE_f97c688e, x.ACCOUNTING, x.UPDATE_f97c688e);

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
            giftCard = getGiftCardValue(delegator, paymentMethodId);
            paymentMethod = getPaymentMethodValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCardCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (giftCard == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCardCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission(x.PAY_INFO, x.UPDATE_f97c688e, userLogin)
                && !security.hasEntityPermission(x.ACCOUNTING, x.UPDATE_f97c688e, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCardPartyNotAuthorized,
                    UtilMisc.toMap(x.partyId, partyId, x.paymentMethodId, paymentMethodId), locale));
        }


        // card number (masked)
        String cardNumber = StringUtil.removeSpaces((String) context.get(x.cardNumber));
        if (cardNumber.startsWith(x.str_df58248c)) {
            // get the masked card number from the db
            String origCardNumber = giftCard.getString(x.cardNumber);
            StringBuilder origMaskedNumber = new StringBuilder(x.emptyString);
            int cardLength = origCardNumber.length() - 4;
            if (cardLength > 0) {
                for (int i = 0; i < cardLength; i++) {
                    origMaskedNumber.append(x.str_df58248c);
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
            newPmId = delegator.getNextSeqId(x.PaymentMethod);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCardCannotBeCreated, locale));
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
                        x.AccountingEftAccountCannotBeUpdated,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
        } else {
            result.put(x.paymentMethodId, paymentMethodId);
            result.put(x.oldPaymentMethodId, paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                    x.AccountingNoChangesMadeNotUpdatingEftAccount, locale));

            return result;
        }

        result.put(x.paymentMethodId, newGc.getString(x.paymentMethodId));
        result.put(x.oldPaymentMethodId, paymentMethodId);
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.CREATE, x.ACCOUNTING, x.CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue(x.PaymentMethod);

        toBeStored.add(newPm);
        GenericValue newEa = delegator.makeValue(x.EftAccount);

        toBeStored.add(newEa);

        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId(x.PaymentMethod);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingEftAccountCannotBeCreated, locale));
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
        newPm.set(x.paymentMethodTypeId, x.EFT_ACCOUNT);
        newEa.set(x.paymentMethodId, newPmId);

        GenericValue newPartyContactMechPurpose = null;
        String contactMechId = (String) context.get(x.contactMechId);

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add a PartyContactMechPurpose of BILLING_LOCATION if necessary
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId,
                            x.contactMechPurposeTypeId, contactMechPurposeTypeId, x.fromDate, now));
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
                    x.AccountingEftAccountCannotBeCreatedWriteFailure,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        result.put(x.paymentMethodId, newEa.getString(x.paymentMethodId));
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.UPDATE_f97c688e, x.ACCOUNTING, x.UPDATE_f97c688e);

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
            eftAccount = getEftAccountValue(delegator, paymentMethodId);
            paymentMethod =
                getPaymentMethodValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingEftAccountCannotBeUpdatedReadFailure,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (eftAccount == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingEftAccountCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission(x.PAY_INFO, x.UPDATE_f97c688e, userLogin)
                && !security.hasEntityPermission(x.ACCOUNTING, x.UPDATE_f97c688e, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingEftAccountCannotBeUpdated,
                    UtilMisc.toMap(x.partyId, partyId, x.paymentMethodId, paymentMethodId), locale));
        }

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newEa = GenericValue.create(eftAccount);
        toBeStored.add(newEa);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId(x.PaymentMethod);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingEftAccountCannotBeCreated, locale));
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
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId,
                                x.contactMechPurposeTypeId, contactMechPurposeTypeId, x.fromDate, now));
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
                        x.AccountingEftAccountCannotBeUpdated,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
        } else {
            result.put(x.paymentMethodId, paymentMethodId);
            result.put(x.oldPaymentMethodId, paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                    x.AccountingNoChangesMadeNotUpdatingEftAccount, locale));

            return result;
        }

        result.put(x.paymentMethodId, newEa.getString(x.paymentMethodId));
        result.put(x.oldPaymentMethodId, paymentMethodId);
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.CREATE, x.ACCOUNTING, x.CREATE);
        if (!result.isEmpty()) {
            return result;
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        GenericValue newPm = delegator.makeValue(x.PaymentMethod);
        toBeStored.add(newPm);

        GenericValue newCa = delegator.makeValue(x.CheckAccount);

        toBeStored.add(newCa);
        String newPmId = (String) context.get(x.paymentMethodId);
        if (UtilValidate.isEmpty(newPmId)) {
            try {
                newPmId = delegator.getNextSeqId(x.PaymentMethod);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.AccountingCheckNotAdded, locale));
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
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);

                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId,
                                x.contactMechPurposeTypeId, contactMechPurposeTypeId, x.fromDate, now));
            }
        }

        if (newPartyContactMechPurpose != null) {
            toBeStored.add(newPartyContactMechPurpose);
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.AccountingCheckNotAdded,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        result.put(x.paymentMethodId, newPm.getString(x.paymentMethodId));
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

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PAY_INFO, x.UPDATE_f97c688e, x.ACCOUNTING, x.UPDATE_f97c688e);

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
            checkAccount = getCheckAccountValue(delegator, paymentMethodId);
            paymentMethod =
                    getPaymentMethodValue(delegator, paymentMethodId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingCheckAccountCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (checkAccount == null || paymentMethod == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingCheckAccountCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, paymentMethodId), locale));
        }
        if (!paymentMethod.getString(x.partyId).equals(partyId) && !security.hasEntityPermission(x.PAY_INFO, x.UPDATE_f97c688e, userLogin)
                && !security.hasEntityPermission(x.ACCOUNTING, x.UPDATE_f97c688e, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingCheckAccountCannotBeUpdated,
                    UtilMisc.toMap(x.partyId, partyId, x.paymentMethodId, paymentMethodId), locale));
        }

        newPm = GenericValue.create(paymentMethod);
        toBeStored.add(newPm);
        newCa = GenericValue.create(checkAccount);
        toBeStored.add(newCa);

        String newPmId = null;
        try {
            newPmId = delegator.getNextSeqId(x.PaymentMethod);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingCheckAccountCannotBeUpdated, locale));
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
            String contactMechPurposeTypeId = x.BILLING_LOCATION;

            GenericValue tempVal;
            try {
                List<GenericValue> allPCWPs = getPartyContactWithPurposeValues(delegator, partyId, contactMechId, contactMechPurposeTypeId);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.contactFromDate, x.contactThruDate, true);
                allPCWPs = EntityUtil.filterByDate(allPCWPs, now, x.purposeFromDate, x.purposeThruDate, true);
                tempVal = EntityUtil.getFirst(allPCWPs);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                tempVal = null;
            }

            if (tempVal == null) {
                // no value found, create a new one
                newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId,
                                x.contactMechPurposeTypeId, contactMechPurposeTypeId, x.fromDate, now));
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
                        x.AccountingCheckAccountCannotBeUpdated,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
        } else {
            result.put(x.paymentMethodId, paymentMethodId);
            result.put(x.oldPaymentMethodId, paymentMethodId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE,
                    UtilProperties.getMessage(RESOURCE, x.AccountingCheckAccountCannotBeUpdated, locale));

            return result;
        }

        result.put(x.paymentMethodId, newCa.getString(x.paymentMethodId));
        result.put(x.oldPaymentMethodId, paymentMethodId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
    private static GenericValue getPaymentMethodValue(Delegator delegator, String paymentMethodId) throws GenericEntityException {
        PaymentMethodDao paymentMethodDao = DaoRegistry.getDao(delegator, x.PaymentMethod, PaymentMethodDao.class);
        try {
            PaymentMethodEntity paymentMethodEntity = paymentMethodDao.get(paymentMethodId).orElse(null);
            return paymentMethodEntity == null ? null : delegator.makeValue(x.PaymentMethod, Beans.beanToMap(paymentMethodEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getCreditCardValue(Delegator delegator, String paymentMethodId) throws GenericEntityException {
        CreditCardDao creditCardDao = DaoRegistry.getDao(delegator, x.CreditCard, CreditCardDao.class);
        try {
            CreditCardEntity creditCardEntity = creditCardDao.get(paymentMethodId).orElse(null);
            return creditCardEntity == null ? null : delegator.makeValue(x.CreditCard, Beans.beanToMap(creditCardEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getGiftCardValue(Delegator delegator, String paymentMethodId) throws GenericEntityException {
        GiftCardDao giftCardDao = DaoRegistry.getDao(delegator, x.GiftCard, GiftCardDao.class);
        try {
            GiftCardEntity giftCardEntity = giftCardDao.get(paymentMethodId).orElse(null);
            return giftCardEntity == null ? null : delegator.makeValue(x.GiftCard, Beans.beanToMap(giftCardEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getEftAccountValue(Delegator delegator, String paymentMethodId) throws GenericEntityException {
        EftAccountDao eftAccountDao = DaoRegistry.getDao(delegator, x.EftAccount, EftAccountDao.class);
        try {
            EftAccountEntity eftAccountEntity = eftAccountDao.get(paymentMethodId).orElse(null);
            return eftAccountEntity == null ? null : delegator.makeValue(x.EftAccount, Beans.beanToMap(eftAccountEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getCheckAccountValue(Delegator delegator, String paymentMethodId) throws GenericEntityException {
        CheckAccountDao checkAccountDao = DaoRegistry.getDao(delegator, x.CheckAccount, CheckAccountDao.class);
        try {
            CheckAccountEntity checkAccountEntity = checkAccountDao.get(paymentMethodId).orElse(null);
            return checkAccountEntity == null ? null : delegator.makeValue(x.CheckAccount, Beans.beanToMap(checkAccountEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static List<GenericValue> getPartyContactWithPurposeValues(Delegator delegator, String partyId, String contactMechId,
            String contactMechPurposeTypeId) throws GenericEntityException {
        PartyContactMechPurposeDao partyContactMechPurposeDao = DaoRegistry.getDao(delegator, x.PartyContactMechPurpose,
                PartyContactMechPurposeDao.class);
        return partyContactMechPurposeDao.listPartyContactWithPurpose(delegator, partyId, contactMechId, contactMechPurposeTypeId);
    }
}

