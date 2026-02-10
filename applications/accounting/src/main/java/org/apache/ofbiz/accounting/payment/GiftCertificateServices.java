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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.GiftCertificateServicesContext;
public class GiftCertificateServices {

    private static final String MODULE = GiftCertificateServices.class.getName();
    private static final String RES_ERROR = x.AccountingErrorUiLabels;
    private static final String RES_ORDER_ERROR = x.OrderErrorUiLabels;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // These are default settings, in case ProductStoreFinActSetting does not have them
    public static final int CARD_NUMBER_LENGTH = 14;
    public static final int PIN_NUMBER_LENGTH = 6;


    // Base Gift Certificate Services
    public static Map<String, Object> createGiftCertificate(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String orderId = (String) context.get(x.orderId);
        BigDecimal initialAmount = (BigDecimal) context.get(x.initialAmount);
        String currency = (String) context.get(x.currency);
        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }
        String currencyUom = (String) context.get(x.currency);
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        String cardNumber = null;
        String pinNumber = null;
        String refNum = null;
        String finAccountId = null;
        try {
            final String accountName = x.Gift_Certificate_Account;
            final String deposit = x.DEPOSIT;

            GenericValue giftCertSettings = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreFinActSetting,
                            UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()),
                            true);
            Map<String, Object> acctResult = null;

            if (x.Y.equals(giftCertSettings.getString(x.requirePinCode))) {
                // TODO: move this code to createFinAccountForStore as well
                int cardNumberLength = CARD_NUMBER_LENGTH;
                int pinNumberLength = PIN_NUMBER_LENGTH;
                if (giftCertSettings.getLong(x.accountCodeLength) != null) {
                    cardNumberLength = giftCertSettings.getLong(x.accountCodeLength).intValue();
                }
                if (giftCertSettings.getLong(x.pinCodeLength) != null) {
                    pinNumberLength = giftCertSettings.getLong(x.pinCodeLength).intValue();
                }
                cardNumber = generateNumber(delegator, cardNumberLength, true);
                pinNumber = generateNumber(delegator, pinNumberLength, false);

                // in this case, the card number is the finAccountId
                finAccountId = cardNumber;

                // create the FinAccount
                Map<String, Object> acctCtx = UtilMisc.<String, Object>toMap(x.finAccountId, finAccountId);
                acctCtx.put(x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId());
                acctCtx.put(x.finAccountName, accountName);
                acctCtx.put(x.finAccountCode, pinNumber);
                acctCtx.put(x.userLogin, userLogin);
                acctResult = dispatcher.runSync(x.createFinAccount, acctCtx);
                if (ServiceUtil.isError(acctResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(acctResult));
                }
            } else {
                Map<String, Object> createAccountCtx = new HashMap<>();
                createAccountCtx.put(x.ownerPartyId, partyId);
                createAccountCtx.put(x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId());
                createAccountCtx.put(x.productStoreId, productStoreId);
                createAccountCtx.put(x.currencyUomId, currency);
                createAccountCtx.put(x.finAccountName, accountName + x.for_party_fef2d707 + partyId + x.str_4ff447b8);
                createAccountCtx.put(x.userLogin, userLogin);
                acctResult = dispatcher.runSync(x.createFinAccountForStore, createAccountCtx);
                if (ServiceUtil.isError(acctResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(acctResult));
                }
                if (acctResult.get(x.finAccountId) != null) {
                    cardNumber = (String) acctResult.get(x.finAccountId);
                    finAccountId = cardNumber;
                }
                if (acctResult.get(x.finAccountCode) != null) {
                    cardNumber = (String) acctResult.get(x.finAccountCode);
                }
            }

            // create the initial (deposit) transaction
            // do something tricky here: run as the "system" user
            // that can actually create a financial account transaction
            GenericValue permUserLogin = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class)
                    .findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.system), true);
            refNum = createTransaction(delegator, dispatcher, permUserLogin, initialAmount, productStoreId,
                    partyId, currencyUom, deposit, finAccountId, locale, orderId);

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCreationError, locale));
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.cardNumber, cardNumber);
        result.put(x.pinNumber, pinNumber);
        result.put(x.initialAmount, initialAmount);
        result.put(x.processResult, Boolean.TRUE);
        result.put(x.responseCode, x._1);
        result.put(x.referenceNum, refNum);
        Debug.logInfo(x.Create_GC_Result + result, MODULE);
        return result;
    }

    public static Map<String, Object> addFundsToGiftCertificate(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        final String deposit = x.DEPOSIT;

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String cardNumber = (String) context.get(x.cardNumber);
        String pinNumber = (String) context.get(x.pinNumber);
        BigDecimal amount = (BigDecimal) context.get(x.amount);

        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }
        String currencyUom = (String) context.get(x.currency);
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        String finAccountId = null;
        GenericValue finAccount = null;
         // validate the pin if the store requires it and figure out the finAccountId from card number
        try {
            GenericValue giftCertSettings = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreFinActSetting,
                            UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()),
                            true);
            if (x.Y.equals(giftCertSettings.getString(x.requirePinCode))) {
                if (!validatePin(delegator, cardNumber, pinNumber)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingGiftCertificateNumberPinNotValid, locale));
                }
                finAccountId = cardNumber;
            } else {
                finAccount = FinAccountHelper.getFinAccountFromCode(cardNumber, delegator);
                if (finAccount != null) {
                    finAccountId = finAccount.getString(x.finAccountId);
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountSetting,
                    UtilMisc.toMap(x.productStoreId, productStoreId,
                            x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()), locale));
        }

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
        }

        if (finAccount == null) {
            try {
                finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                        .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, finAccountId), locale));
            }
        }

        // get the previous balance
        BigDecimal previousBalance = BigDecimal.ZERO;
        if (finAccount.get(x.availableBalance) != null) {
            previousBalance = finAccount.getBigDecimal(x.availableBalance);
        }

        // create the transaction
        BigDecimal balance = BigDecimal.ZERO;
        String refNum = null;
        try {
            refNum = GiftCertificateServices.createTransaction(delegator, dispatcher, userLogin, amount, productStoreId, partyId,
                    currencyUom, deposit, finAccountId, locale);
            finAccount.refresh();
            balance = finAccount.getBigDecimal(x.availableBalance);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.previousBalance, previousBalance);
        result.put(x.balance, balance);
        result.put(x.amount, amount);
        result.put(x.processResult, Boolean.TRUE);
        result.put(x.responseCode, x._1);
        result.put(x.referenceNum, refNum);
        Debug.logInfo(x.Add_Funds_GC_Result + result, MODULE);
        return result;
    }

    public static Map<String, Object> redeemGiftCertificate(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        final String withdrawl = x.WITHDRAWAL;
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String orderId = (String) context.get(x.orderId);
        String cardNumber = (String) context.get(x.cardNumber);
        String pinNumber = (String) context.get(x.pinNumber);
        BigDecimal amount = (BigDecimal) context.get(x.amount);

        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }
        String currencyUom = (String) context.get(x.currency);
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // validate the amount
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountMustBePositive, locale));
        }

        // validate the pin if the store requires it
        try {
            GenericValue giftCertSettings = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreFinActSetting,
                            UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()),
                            true);
            if (x.Y.equals(giftCertSettings.getString(x.requirePinCode)) && !validatePin(delegator, cardNumber, pinNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberPinNotValid, locale));
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountSetting,
                    UtilMisc.toMap(x.productStoreId, productStoreId,
                            x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()), locale));
        }
        Debug.logInfo(x.Attempting_to_redeem_GC_for + amount, MODULE);

        GenericValue finAccount = null;
        try {
            finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                    .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, cardNumber), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, cardNumber), locale));
        }

        // check the actual balance (excluding authorized amounts) and create the transaction if it is sufficient
        BigDecimal previousBalance = finAccount.get(x.actualBalance) == null ? BigDecimal.ZERO : finAccount.getBigDecimal(x.actualBalance);

        BigDecimal balance = BigDecimal.ZERO;
        String refNum = null;
        Boolean procResult;
        if (previousBalance.compareTo(amount) >= 0) {
            try {
                refNum = GiftCertificateServices.createTransaction(delegator, dispatcher, userLogin, amount, productStoreId,
                        partyId, currencyUom, withdrawl, cardNumber, locale, orderId);
                finAccount.refresh();
                balance = finAccount.get(x.availableBalance) == null ? BigDecimal.ZERO : finAccount.getBigDecimal(x.availableBalance);
                procResult = Boolean.TRUE;
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            procResult = Boolean.FALSE;
            balance = previousBalance;
            refNum = x.N_A;
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.previousBalance, previousBalance);
        result.put(x.balance, balance);
        result.put(x.amount, amount);
        result.put(x.processResult, procResult);
        result.put(x.responseCode, x._2);
        result.put(x.referenceNum, refNum);
        Debug.logInfo(x.Redeem_GC_Result + result, MODULE);
        return result;
    }

    public static Map<String, Object> checkGiftCertificateBalance(DispatchContext dctx, GiftCertificateServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String cardNumber = (String) context.get(x.cardNumber);
        String pinNumber = (String) context.get(x.pinNumber);
        Locale locale = (Locale) context.get(x.locale);

        // validate the pin
        if (!validatePin(delegator, cardNumber, pinNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberPinNotValid, locale));
        }

        GenericValue finAccount = null;
        try {
            finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                    .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, cardNumber), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, cardNumber), locale));
        }

        // TODO: get the real currency from context
        // get the balance
        BigDecimal balance = finAccount.get(x.availableBalance) == null ? BigDecimal.ZERO : finAccount.getBigDecimal(x.availableBalance);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.balance, balance);
        Debug.logInfo(x.GC_Balance_Result + result, MODULE);
        return result;
    }

    // Fullfilment Services
    public static Map<String, Object> giftCertificateProcessor(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        BigDecimal amount = (BigDecimal) context.get(x.processAmount);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the authorizations
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountCannotCapture, locale));
        }

        // get the gift certificate and its authorization from the authorization
        String finAccountAuthId = authTransaction.getString(x.referenceNum);
        try {
            GenericValue finAccountAuth = DaoRegistry.getDao(delegator, x.FinAccountAuth, UserLoginDao.class)
                    .findOne(delegator, x.FinAccountAuth, UtilMisc.toMap(x.finAccountAuthId, finAccountAuthId), false);
            GenericValue giftCard = finAccountAuth.getRelatedOne(x.FinAccount, false);
            // make sure authorization has not expired
            Timestamp authExpiration = finAccountAuth.getTimestamp(x.thruDate);
            if ((authExpiration != null) && (authExpiration.before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountAuthorizationExpired,
                        UtilMisc.toMap(x.paymentGatewayResponseId, authTransaction.getString(x.paymentGatewayResponseId),
                                x.authExpiration, authExpiration), locale));
            }
            // make sure the fin account itself has not expired
            if ((giftCard.getTimestamp(x.thruDate) != null) && (giftCard.getTimestamp(x.thruDate).before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberExpired,
                        UtilMisc.toMap(x.thruDate, giftCard.getTimestamp(x.thruDate)), locale));
            }

            // obtain the order information
            OrderReadHelper orh = new OrderReadHelper(delegator, orderPaymentPreference.getString(x.orderId));

            Map<String, Object> redeemCtx = new HashMap<>();
            redeemCtx.put(x.userLogin, userLogin);
            redeemCtx.put(x.productStoreId, orh.getProductStoreId());
            redeemCtx.put(x.cardNumber, giftCard.get(x.finAccountId));
            redeemCtx.put(x.pinNumber, giftCard.get(x.finAccountCode));
            redeemCtx.put(x.currency, currency);
            redeemCtx.put(x.orderId, orderId);
            if (orh.getBillToParty() != null) {
                redeemCtx.put(x.partyId, orh.getBillToParty().get(x.partyId));
            }
            redeemCtx.put(x.amount, amount);

            // invoke the redeem service
            Map<String, Object> redeemResult = null;
            redeemResult = dispatcher.runSync(x.redeemGiftCertificate, redeemCtx);
            if (ServiceUtil.isError(redeemResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(redeemResult));
            }

            // now release the authorization should this use the gift card release service?
            Map<String, Object> releaseResult = dispatcher.runSync(x.expireFinAccountAuth,
                    UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.finAccountAuthId, finAccountAuthId));
            if (ServiceUtil.isError(releaseResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
            }

            String authRefNum = authTransaction.getString(x.referenceNum);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (redeemResult != null) {
                Boolean processResult = (Boolean) redeemResult.get(x.processResult);
                result.put(x.processAmount, amount);
                result.put(x.captureResult, processResult);
                result.put(x.captureCode, x.C);
                result.put(x.captureRefNum, redeemResult.get(x.referenceNum));
                result.put(x.authRefNum, authRefNum);
            }

            return result;

        } catch (GenericEntityException | GenericServiceException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotProcess,
                    UtilMisc.toMap(x.errorString, ex.getMessage()), locale));
        }
    }


    public static Map<String, Object> giftCertificateAuthorize(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue giftCard = (GenericValue) context.get(x.giftCard);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        BigDecimal amount = (BigDecimal) context.get(x.processAmount);

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // obtain the order information
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();
        try {
            // if the store requires pin codes, then validate pin code against card number, and the gift certificate's finAccountId is the
            // gift card's card number
            // otherwise, the gift card's card number is an ecrypted string, which must be decoded to find the FinAccount
            GenericValue giftCertSettings = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreFinActSetting,
                            UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()),
                            true);
            GenericValue finAccount = null;
            String finAccountId = null;
            if (UtilValidate.isNotEmpty(giftCertSettings)) {
                if (x.Y.equals(giftCertSettings.getString(x.requirePinCode))) {
                    if (validatePin(delegator, giftCard.getString(x.cardNumber), giftCard.getString(x.pinNumber))) {
                        finAccountId = giftCard.getString(x.cardNumber);
                        finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                                .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
                    }
                } else {
                    finAccount = FinAccountHelper.getFinAccountFromCode(giftCard.getString(x.cardNumber), delegator);
                    if (finAccount == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.AccountingGiftCertificateNumberNotFound,
                                UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
                    }
                    finAccountId = finAccount.getString(x.finAccountId);
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountSetting,
                        UtilMisc.toMap(x.productStoreId, productStoreId,
                                x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()), locale));
            }

            if (finAccountId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberPinNotValid, locale));
            }

            // check for expiration date
            if ((finAccount.getTimestamp(x.thruDate) != null) && (finAccount.getTimestamp(x.thruDate).before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberExpired,
                        UtilMisc.toMap(x.thruDate, finAccount.getTimestamp(x.thruDate)), locale));
            }

            // check the amount to authorize against the available balance of fin account, which includes active authorizations as well as
            // transactions
            BigDecimal availableBalance = finAccount.getBigDecimal(x.availableBalance);
            Boolean processResult = null;
            String refNum = null;
            Map<String, Object> result = ServiceUtil.returnSuccess();

            // make sure to round and scale it to the same as availableBalance
            amount = amount.setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());

            // if availableBalance equal to or greater than amount, then auth
            if (UtilValidate.isNotEmpty(availableBalance) && availableBalance.compareTo(amount) >= 0) {
                Timestamp thruDate = null;
                if (giftCertSettings.getLong(x.authValidDays) != null) {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), giftCertSettings.getLong(x.authValidDays));
                }
                Map<String, Object> tmpResult = dispatcher.runSync(x.createFinAccountAuth,
                        UtilMisc.<String, Object>toMap(x.finAccountId, finAccountId,
                                x.amount, amount, x.currencyUomId, currency,
                                x.thruDate, thruDate, x.userLogin, userLogin));
                if (ServiceUtil.isError(tmpResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                } else {
                    refNum = (String) tmpResult.get(x.finAccountAuthId);
                    processResult = Boolean.TRUE;
                }
            } else {
                Debug.logError(x.Attempted_to_authorize + amount + x.against_a_balance_of_only + availableBalance + x.str_4ff447b8, MODULE);
                refNum = x.N_A;      // a refNum is always required from authorization
                processResult = Boolean.FALSE;
            }

            result.put(x.processAmount, amount);
            result.put(x.authResult, processResult);
            result.put(x.authFlag, x._2);
            result.put(x.authCode, x.A);
            result.put(x.captureCode, x.C);
            result.put(x.authRefNum, refNum);

            return result;
        } catch (GenericEntityException | GenericServiceException ex) {
            Debug.logError(ex, x.Cannot_authorize_gift_certificate, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotAuthorize,
                    UtilMisc.toMap(x.errorString, ex.getMessage()), locale));
        }
    }

    public static Map<String, Object> giftCertificateRefund(DispatchContext dctx, GiftCertificateServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String currency = (String) context.get(x.currency);
        BigDecimal amount = (BigDecimal) context.get(x.refundAmount);
        Locale locale = (Locale) context.get(x.locale);
        return giftCertificateRestore(dctx, userLogin, paymentPref, amount, currency, x.refund, locale);
    }

    public static Map<String, Object> giftCertificateRelease(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);

        String err = UtilProperties.getMessage(RES_ERROR,
                x.AccountingGiftCertificateNumberCannotBeExpired, locale);
        try {
            // expire the related financial authorization transaction
            GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTransaction == null) {
                return ServiceUtil.returnError(err + UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountCannotFindAuthorization, locale));
            }
            Map<String, Object> input = UtilMisc.<String, Object>toMap(x.userLogin, userLogin,
                    x.finAccountAuthId, authTransaction.get(x.referenceNum));
            Map<String, Object> serviceResults = dispatcher.runSync(x.expireFinAccountAuth, input);
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.releaseRefNum, authTransaction.getString(x.referenceNum));
            result.put(x.releaseAmount, authTransaction.getBigDecimal(x.amount));
            result.put(x.releaseResult, Boolean.TRUE);

            return result;
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(err + e.getMessage());
        }
    }

    private static Map<String, Object> giftCertificateRestore(DispatchContext dctx, GenericValue userLogin, GenericValue paymentPref,
            BigDecimal amount, String currency, String resultPrefix, Locale locale) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        // get the orderId for tracking
        String orderId = paymentPref.getString(x.orderId);
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString(x.partyId);
        }

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne(x.GiftCard, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_GiftCard_from_OrderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotLocateItFromOrderPaymentPreference, locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotRelease, locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        Map<String, Object> refundCtx = new HashMap<>();
        refundCtx.put(x.productStoreId, productStoreId);
        refundCtx.put(x.currency, currency);
        refundCtx.put(x.partyId, partyId);
        refundCtx.put(x.cardNumber, giftCard.get(x.cardNumber));
        refundCtx.put(x.pinNumber, giftCard.get(x.pinNumber));
        refundCtx.put(x.amount, amount);
        refundCtx.put(x.userLogin, userLogin);

        Map<String, Object> restoreGcResult = null;
        try {
            restoreGcResult = dispatcher.runSync(x.addFundsToGiftCertificate, refundCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberRefundCallError, locale));
        }
        if (ServiceUtil.isError(restoreGcResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(restoreGcResult));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (restoreGcResult != null) {
            Boolean processResult = (Boolean) restoreGcResult.get(x.processResult);
            result.put(resultPrefix + x.Amount, amount);
            result.put(resultPrefix + x.Result, processResult);
            result.put(resultPrefix + x.Code, x.R);
            result.put(resultPrefix + x.Flag, restoreGcResult.get(x.responseCode));
            result.put(resultPrefix + x.RefNum, restoreGcResult.get(x.referenceNum));
        }

        return result;
    }

    public static Map<String, Object> giftCertificatePurchase(DispatchContext dctx, GiftCertificateServicesContext context) {
        // this service should always be called via FULFILLMENT_EXTASYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        Locale locale = (Locale) context.get(x.locale);

        // order ID for tracking
        String orderId = orderItem.getString(x.orderId);

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_OrderHeader_from_OrderItem, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.OrderCannotGetOrderHeader, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotProcess,
                    UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString(x.partyId);
        }

        // amount/quantity of the gift card(s)
        BigDecimal amount = orderItem.getBigDecimal(x.unitPrice);
        BigDecimal quantity = orderItem.getBigDecimal(x.quantity);

        // the product entity needed for information
        GenericValue product = null;
        try {
            product = orderItem.getRelatedOne(x.Product, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_Product_from_OrderItem, MODULE);
        }
        if (product == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfill, locale));
        }

        // Gift certificate settings are per store in this entity
        GenericValue giftCertSettings = null;
        try {
            giftCertSettings = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreFinActSetting,
                            UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()),
                            true);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_Product_Store_FinAccount_settings_for + FinAccountHelper.getGiftCertFinAccountTypeId(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountSetting,
                    UtilMisc.toMap(x.productStoreId, productStoreId,
                            x.finAccountTypeId, FinAccountHelper.getGiftCertFinAccountTypeId()), locale) + x.str_ceca32e9 + e.getMessage());
        }

        // survey information
        String surveyId = giftCertSettings.getString(x.purchaseSurveyId);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            // there should be only one
            surveyResponse = DaoRegistry.getDao(delegator, x.SurveyResponse, UserLoginDao.class)
                    .findFirstByCondition(delegator, x.SurveyResponse,
                            EntityCondition.makeCondition(UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItem.get(x.orderItemSeqId),
                                    x.surveyId, surveyId)),
                            null, UtilMisc.toList(x.responseDate_37a6232b), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurvey, locale));
        }
        if (surveyResponse == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurvey, locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne(x.SurveyQuestion, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        // get the send to email address - key defined in product store settings entity
        String sendToKey = giftCertSettings.getString(x.purchSurveySendTo);
        String sendToEmail = (String) answerMap.get(sendToKey);

        // get the copyMe flag and set the order email address
        String orderEmails = orh.getOrderEmailString();
        String copyMeField = giftCertSettings.getString(x.purchSurveyCopyMe);
        String copyMeResp = copyMeField != null ? (String) answerMap.get(copyMeField) : null;
        boolean copyMe = UtilValidate.isNotEmpty(copyMeField)
                && UtilValidate.isNotEmpty(copyMeResp) && x._true.equalsIgnoreCase(copyMeResp);

        int qtyLoop = quantity.intValue();
        for (int i = 0; i < qtyLoop; i++) {
            // create a gift certificate
            Map<String, Object> createGcCtx = new HashMap<>();
            createGcCtx.put(x.productStoreId, productStoreId);
            createGcCtx.put(x.orderId, orderId);
            createGcCtx.put(x.currency, currency);
            createGcCtx.put(x.partyId, partyId);
            createGcCtx.put(x.initialAmount, amount);
            createGcCtx.put(x.userLogin, userLogin);

            Map<String, Object> createGcResult = null;
            try {
                createGcResult = dispatcher.runSync(x.createGiftCertificate, createGcCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberCreationError, locale) + e.getMessage());
            }
            if (ServiceUtil.isError(createGcResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberCreationError, locale)
                        + ServiceUtil.getErrorMessage(createGcResult));
            }

            // create the fulfillment record
            Map<String, Object> gcFulFill = new HashMap<>();
            gcFulFill.put(x.typeEnumId, x.GC_ACTIVATE);
            gcFulFill.put(x.partyId, partyId);
            gcFulFill.put(x.orderId, orderId);
            gcFulFill.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
            gcFulFill.put(x.surveyResponseId, surveyResponse.get(x.surveyResponseId));
            gcFulFill.put(x.cardNumber, createGcResult.get(x.cardNumber));
            gcFulFill.put(x.pinNumber, createGcResult.get(x.pinNumber));
            gcFulFill.put(x.amount, createGcResult.get(x.initialAmount));
            gcFulFill.put(x.responseCode, createGcResult.get(x.responseCode));
            gcFulFill.put(x.referenceNum, createGcResult.get(x.referenceNum));
            gcFulFill.put(x.userLogin, userLogin);
            try {
                dispatcher.runAsync(x.createGcFulFillmentRecord, gcFulFill, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberCannotStoreFulfillmentInfo,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }

            // add some information to the answerMap for the email
            answerMap.put(x.cardNumber, createGcResult.get(x.cardNumber));
            answerMap.put(x.pinNumber, createGcResult.get(x.pinNumber));
            answerMap.put(x.amount, createGcResult.get(x.initialAmount));

            // get the email setting for this email type
            GenericValue productStoreEmail = null;
            String emailType = x.PRDS_GC_PURCHASE;
            try {
                productStoreEmail = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting, UserLoginDao.class)
                        .findOne(delegator, x.ProductStoreEmailSetting, UtilMisc.toMap(x.productStoreId, productStoreId, x.emailType, emailType),
                                false);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Unable_to_get_product_store_email_setting_for_gift_card_purchase, MODULE);
            }
            if (productStoreEmail == null) {
                Debug.logError(x.No_gift_card_purchase_email_setting_found_for_this_store_cannot_send_gift_card_information, MODULE);
            } else {
                answerMap.put(x.locale, locale);

                // set the bcc address(s)
                String bcc = productStoreEmail.getString(x.bccAddress);
                if (copyMe) {
                    if (UtilValidate.isNotEmpty(bcc)) {
                        bcc = bcc + x.str_5c10b5b2 + orderEmails;
                    } else {
                        bcc = orderEmails;
                    }
                }
                Map<String, Object> emailCtx = new HashMap<>();
                emailCtx.put(x.bodyScreenUri, productStoreEmail.getString(x.bodyScreenLocation));
                emailCtx.put(x.bodyParameters, answerMap);
                emailCtx.put(x.sendTo, sendToEmail);
                emailCtx.put(x.contentType, productStoreEmail.get(x.contentType));
                emailCtx.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
                emailCtx.put(x.sendCc, productStoreEmail.get(x.ccAddress));
                emailCtx.put(x.sendBcc, bcc);
                emailCtx.put(x.subject, productStoreEmail.getString(x.subject));
                emailCtx.put(x.userLogin, userLogin);
                try {
                    dispatcher.runAsync(x.sendMailFromScreen, emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_sending_mail, MODULE);
                    // this is fatal; we will rollback and try again later
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingGiftCertificateNumberCannotSendEmailNotice,
                            UtilMisc.toMap(x.errorString, e.toString()), locale));
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> giftCertificateReload(DispatchContext dctx, GiftCertificateServicesContext context) {
        // this service should always be called via FULFILLMENT_EXTSYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        Locale locale = (Locale) context.get(x.locale);

        // order ID for tracking
        String orderId = orderItem.getString(x.orderId);

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_OrderHeader_from_OrderItem, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.OrderCannotGetOrderHeader, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.AccountingGiftCertificateNumberCannotReload, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, x.GIFT_CARD, null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString(x.paymentPropertiesPath);
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.AccountingGiftCertificateNumberCannotGetPaymentConfiguration, locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString(x.partyId);
        }

        // amount of the gift card reload
        BigDecimal amount = orderItem.getBigDecimal(x.unitPrice);

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_surveyId, delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            // there should be only one
            surveyResponse = DaoRegistry.getDao(delegator, x.SurveyResponse, UserLoginDao.class)
                    .findFirstByCondition(delegator, x.SurveyResponse,
                            EntityCondition.makeCondition(UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItem.get(x.orderItemSeqId),
                                    x.surveyId, surveyId)),
                            null, UtilMisc.toList(x.responseDate_37a6232b), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.AccountingGiftCertificateNumberCannotReload, locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.AccountingGiftCertificateNumberCannotReloadFromSurveyAnswers, locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne(x.SurveyQuestion, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                            x.AccountingGiftCertificateNumberCannotReloadFromSurveyAnswers, locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        String cardNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_survey_cardNumber, delegator);
        String pinNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_survey_pinNumber, delegator);
        String cardNumber = (String) answerMap.get(cardNumberKey);
        String pinNumber = (String) answerMap.get(pinNumberKey);

        // reload the gift card
        Map<String, Object> reloadCtx = new HashMap<>();
        reloadCtx.put(x.productStoreId, productStoreId);
        reloadCtx.put(x.currency, currency);
        reloadCtx.put(x.partyId, partyId);
        reloadCtx.put(x.cardNumber, cardNumber);
        reloadCtx.put(x.pinNumber, pinNumber);
        reloadCtx.put(x.amount, amount);
        reloadCtx.put(x.userLogin, userLogin);

        String errorMessage = null;
        Map<String, Object> reloadGcResult = null;
        try {
            reloadGcResult = dispatcher.runSync(x.addFundsToGiftCertificate, reloadCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            errorMessage = x.Unable_to_call_reload_service;
        }
        if (ServiceUtil.isError(reloadGcResult)) {
            errorMessage = ServiceUtil.getErrorMessage(reloadGcResult);
        }

        // create the fulfillment record
        Map<String, Object> gcFulFill = new HashMap<>();
        gcFulFill.put(x.typeEnumId, x.GC_RELOAD);
        gcFulFill.put(x.userLogin, userLogin);
        gcFulFill.put(x.partyId, partyId);
        gcFulFill.put(x.orderId, orderId);
        gcFulFill.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
        gcFulFill.put(x.surveyResponseId, surveyResponse.get(x.surveyResponseId));
        gcFulFill.put(x.cardNumber, cardNumber);
        gcFulFill.put(x.pinNumber, pinNumber);
        gcFulFill.put(x.amount, amount);
        if (reloadGcResult != null) {
            gcFulFill.put(x.responseCode, reloadGcResult.get(x.responseCode));
            gcFulFill.put(x.referenceNum, reloadGcResult.get(x.referenceNum));
        }
        try {
            dispatcher.runAsync(x.createGcFulFillmentRecord, gcFulFill, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotStoreFulfillmentInfo,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (errorMessage != null) {
            // there was a problem
            Debug.logError(x.Reload_Failed_Need_to_Refund + reloadGcResult, MODULE);

            // process the return
            try {
                Map<String, Object> refundCtx = UtilMisc.toMap(x.orderItem, orderItem,
                        x.partyId, partyId, x.userLogin, userLogin);
                dispatcher.runAsync(x.refundGcPurchase, refundCtx, null, true, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.ERROR_Unable_to_call_create_refund_service_this_failed_reload_will_NOT_be_refunded, MODULE);
            }

            return ServiceUtil.returnError(errorMessage);
        }

        // add some information to the answerMap for the email
        answerMap.put(x.processResult, reloadGcResult.get(x.processResult));
        answerMap.put(x.responseCode, reloadGcResult.get(x.responseCode));
        answerMap.put(x.previousAmount, reloadGcResult.get(x.previousBalance));
        answerMap.put(x.amount, reloadGcResult.get(x.amount));

        // get the email setting for this email type
        GenericValue productStoreEmail = null;
        String emailType = x.PRDS_GC_RELOAD;
        try {
            productStoreEmail = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting, UserLoginDao.class)
                    .findOne(delegator, x.ProductStoreEmailSetting, UtilMisc.toMap(x.productStoreId, productStoreId, x.emailType, emailType),
                            false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_product_store_email_setting_for_gift_card_purchase, MODULE);
        }
        if (productStoreEmail == null) {
            Debug.logError(x.No_gift_card_purchase_email_setting_found_for_this_store_cannot_send_gift_card_information, MODULE);
        } else {
            answerMap.put(x.locale, locale);

            Map<String, Object> emailCtx = new HashMap<>();
            emailCtx.put(x.bodyScreenUri, productStoreEmail.getString(x.bodyScreenLocation));
            emailCtx.put(x.bodyParameters, answerMap);
            emailCtx.put(x.sendTo, orh.getOrderEmailString());
            emailCtx.put(x.contentType, productStoreEmail.get(x.contentType));
            emailCtx.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
            emailCtx.put(x.sendCc, productStoreEmail.get(x.ccAddress));
            emailCtx.put(x.sendBcc, productStoreEmail.get(x.bccAddress));
            emailCtx.put(x.subject, productStoreEmail.getString(x.subject));
            emailCtx.put(x.userLogin, userLogin);

            // send off the email async so we will retry on failed attempts
            try {
                dispatcher.runAsync(x.sendMailFromScreen, emailCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_sending_mail, MODULE);
                // this is fatal; we will rollback and try again later
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberCannotSendEmailNotice,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // Tracking Service
    public static Map<String, Object> createFulfillmentRecord(DispatchContext dctx, GiftCertificateServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        // create the fulfillment record
        GenericValue gcFulFill = delegator.makeValue(x.GiftCardFulfillment);
        gcFulFill.set(x.fulfillmentId, delegator.getNextSeqId(x.GiftCardFulfillment));
        gcFulFill.set(x.typeEnumId, context.get(x.typeEnumId));
        gcFulFill.set(x.merchantId, context.get(x.merchantId));
        gcFulFill.set(x.partyId, context.get(x.partyId));
        gcFulFill.set(x.orderId, context.get(x.orderId));
        gcFulFill.set(x.orderItemSeqId, context.get(x.orderItemSeqId));
        gcFulFill.set(x.surveyResponseId, context.get(x.surveyResponseId));
        gcFulFill.set(x.cardNumber, context.get(x.cardNumber));
        gcFulFill.set(x.pinNumber, context.get(x.pinNumber));
        gcFulFill.set(x.amount, context.get(x.amount));
        gcFulFill.set(x.responseCode, context.get(x.responseCode));
        gcFulFill.set(x.referenceNum, context.get(x.referenceNum));
        gcFulFill.set(x.authCode, context.get(x.authCode));
        gcFulFill.set(x.fulfillmentDate, UtilDateTime.nowTimestamp());
        try {
            delegator.create(gcFulFill);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotStoreFulfillmentInfo,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    // Refund Service
    public static Map<String, Object> refundGcPurchase(DispatchContext dctx, GiftCertificateServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);

        // refresh the item object for status changes
        try {
            orderItem.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        Map<String, Object> returnableInfo = null;
        try {
            returnableInfo = dispatcher.runSync(x.getReturnableQuantity, UtilMisc.toMap(x.orderItem, orderItem,
                    x.userLogin, userLogin));
            if (ServiceUtil.isError(returnableInfo)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(returnableInfo));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.OrderErrorUnableToGetReturnItemInformation, locale));
        }

        if (returnableInfo != null) {
            BigDecimal returnableQuantity = (BigDecimal) returnableInfo.get(x.returnableQuantity);
            BigDecimal returnablePrice = (BigDecimal) returnableInfo.get(x.returnablePrice);
            Debug.logInfo(x.Returnable_INFO + returnableQuantity + x.str_8dc29a72 + returnablePrice + x.str_70621d3f + orderItem, MODULE);

            // create the return header
            Map<String, Object> returnHeaderInfo = new HashMap<>();
            returnHeaderInfo.put(x.fromPartyId, partyId);
            returnHeaderInfo.put(x.userLogin, userLogin);
            Map<String, Object> returnHeaderResp = null;
            try {
                returnHeaderResp = dispatcher.runSync(x.createReturnHeader, returnHeaderInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorUnableToCreateReturnHeader, locale));
            }

            if (ServiceUtil.isError(returnHeaderResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(returnHeaderResp));
            }
            String returnId = (String) returnHeaderResp.get(x.returnId);
            if (UtilValidate.isEmpty(returnId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorCreateReturnHeaderWithoutId, locale));
            }

            // create the return item
            Map<String, Object> returnItemInfo = new HashMap<>();
            returnItemInfo.put(x.returnId, returnId);
            returnItemInfo.put(x.returnReasonId, x.RTN_DIG_FILL_FAIL);
            returnItemInfo.put(x.returnTypeId, x.RTN_REFUND);
            returnItemInfo.put(x.returnItemType, x.ITEM);
            returnItemInfo.put(x.description, orderItem.get(x.itemDescription));
            returnItemInfo.put(x.orderId, orderItem.get(x.orderId));
            returnItemInfo.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
            returnItemInfo.put(x.returnQuantity, returnableQuantity);
            returnItemInfo.put(x.returnPrice, returnablePrice);
            returnItemInfo.put(x.userLogin, userLogin);
            Map<String, Object> returnItemResp = null;
            try {
                returnItemResp = dispatcher.runSync(x.createReturnItem, returnItemInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorUnableToCreateReturnItem, locale));
            }

            if (ServiceUtil.isError(returnItemResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(returnItemResp));
            }

            String returnItemSeqId = (String) returnItemResp.get(x.returnItemSeqId);

            if (returnItemSeqId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorCreateReturnItemWithoutId, locale));
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Created_return_item + returnId + x.str_0d0c4ddd + returnItemSeqId, MODULE);
            }

            // need the system userLogin to "fake" out the update service
            GenericValue admin = null;
            try {
                admin = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class)
                        .findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.system), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorUnableToUpdateReturnHeaderStatusWithoutUserLogin, locale));
            }

            // update the status to received so it can process
            Map<String, Object> updateReturnInfo = new HashMap<>();
            updateReturnInfo.put(x.returnId, returnId);
            updateReturnInfo.put(x.statusId, x.RETURN_RECEIVED);
            updateReturnInfo.put(x.currentStatusId, x.RETURN_REQUESTED);
            updateReturnInfo.put(x.userLogin, admin);
            Map<String, Object> updateReturnResp = null;
            try {
                updateReturnResp = dispatcher.runSync(x.updateReturnHeader, updateReturnInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorUnableToUpdateReturnHeaderStatus, locale));
            }

            if (ServiceUtil.isError(updateReturnResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(updateReturnResp));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // Private worker methods
    private static boolean validatePin(Delegator delegator, String cardNumber, String pinNumber) {
        GenericValue finAccount = null;
        try {
            finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                    .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, cardNumber), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (finAccount != null) {
            String dbPin = finAccount.getString(x.finAccountCode);
            if (Debug.infoOn()) {
                Debug.logInfo(x.GC_Pin_Validation_Sent + pinNumber + x.Actual + dbPin + x.str_4ff447b8, MODULE);
            }
            if (dbPin != null && dbPin.equals(pinNumber)) {
                return true;
            }
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.GC_FinAccount_record_not_found + cardNumber + x.str_e7064f0b, MODULE);
        }
        return false;
    }
    private static String createTransaction(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
                                            BigDecimal amount, String productStoreId, String partyId, String currencyUom, String txType,
                                            String finAccountId, Locale locale) throws GeneralException {
        return createTransaction(delegator, dispatcher, userLogin, amount, productStoreId,
                partyId, currencyUom, txType, finAccountId, locale, null);
    }
    private static String createTransaction(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            BigDecimal amount, String productStoreId, String partyId, String currencyUom, String txType,
            String finAccountId, Locale locale, String orderId) throws GeneralException {
        final String coParty = getPayToPartyId(delegator, productStoreId);
        final String paymentMethodType = x.GIFT_CERTIFICATE;

        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }

        String paymentType = null;
        String partyIdFrom = null;
        String partyIdTo = null;
        if (x.DEPOSIT.equals(txType)) {
            paymentType = x.GC_DEPOSIT;
            partyIdFrom = partyId;
            partyIdTo = coParty;
        } else if (x.WITHDRAWAL.equals(txType)) {
            paymentType = x.GC_WITHDRAWAL;
            partyIdFrom = coParty;
            partyIdTo = partyId;
        } else {
            throw new GeneralException(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountCannotCreateTransaction, locale));
        }

        // create the payment for the transaction
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap(x.paymentTypeId, paymentType);
        paymentCtx.put(x.paymentMethodTypeId, paymentMethodType);
        paymentCtx.put(x.partyIdTo, partyIdTo);
        paymentCtx.put(x.partyIdFrom, partyIdFrom);
        paymentCtx.put(x.statusId, x.PMNT_RECEIVED);
        paymentCtx.put(x.currencyUomId, currencyUom);
        paymentCtx.put(x.amount, amount);
        paymentCtx.put(x.userLogin, userLogin);
        paymentCtx.put(x.paymentRefNum, x.N_A);

        String paymentId = null;
        Map<String, Object> payResult = null;
        try {
            payResult = dispatcher.runSync(x.createPayment, paymentCtx);
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (payResult == null) {
            throw new GeneralException(x.Unknown_error_in_creating_financial_account_transaction);
        }
        if (ServiceUtil.isError(payResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(payResult));
        }

        paymentId = (String) payResult.get(x.paymentId);

        // create the initial transaction
        Map<String, Object> transCtx = UtilMisc.<String, Object>toMap(x.finAccountTransTypeId, txType);
        transCtx.put(x.finAccountId, finAccountId);
        transCtx.put(x.partyId, userLogin.getString(x.partyId));
        transCtx.put(x.userLogin, userLogin);
        transCtx.put(x.paymentId, paymentId);
        transCtx.put(x.orderId, orderId);
        transCtx.put(x.amount, amount);

        Map<String, Object> transResult = null;
        String txId = null;
        try {
            transResult = dispatcher.runSync(x.createFinAccountTrans, transCtx);
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (transResult == null) {
            throw new GeneralException(x.Unknown_error_in_creating_financial_account_transaction);
        }
        if (ServiceUtil.isError(transResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
        } else {
            txId = (String) transResult.get(x.finAccountTransId);
        }

        return txId;
    }

    private static String generateNumber(Delegator delegator, int length, boolean isId) throws GenericEntityException {
        if (length > 19) {
            length = 19;
        }

        boolean isValid = false;
        StringBuilder number = null;
        while (!isValid) {
            number = new StringBuilder(x.emptyString);
            for (int i = 0; i < length; i++) {
                int randInt = SECURE_RANDOM.nextInt(9);
                number.append(randInt);
            }

            if (isId) {
                number.append(UtilValidate.getLuhnCheckDigit(number.toString()));

                // validate the number
                if (checkCardNumber(number.toString())) {
                    // make sure this number doens't already exist
                    isValid = checkNumberInDatabase(delegator, number.toString());
                }
            } else {
                isValid = true;
            }
        }
        return number.toString();
    }

    private static boolean checkNumberInDatabase(Delegator delegator, String number) throws GenericEntityException {
        GenericValue finAccount = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class)
                .findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, number), false);
        return finAccount == null;
    }

    private static boolean checkCardNumber(String number) {
        number = number.replaceAll(x.D, x.emptyString);
        return UtilValidate.sumIsMod10(UtilValidate.getLuhnSum(number));
    }

    private static String getPayToPartyId(Delegator delegator, String productStoreId) {
        String payToPartyId = x.Company; // default value
        GenericValue productStore = null;
        try {
            productStore = DaoRegistry.getDao(delegator, x.ProductStore, UserLoginDao.class)
                    .findOne(delegator, x.ProductStore, UtilMisc.toMap(x.productStoreId, productStoreId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_locate_ProductStore + productStoreId + x.str_e7064f0b, MODULE);
            return null;
        }
        if (productStore != null && productStore.get(x.payToPartyId) != null) {
            payToPartyId = productStore.getString(x.payToPartyId);
        }
        return payToPartyId;
    }
}
