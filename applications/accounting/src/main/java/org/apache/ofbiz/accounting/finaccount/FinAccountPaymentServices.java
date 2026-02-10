/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
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
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FinAccountPaymentServicesContext;
/**
 * FinAccountPaymentServices - Financial account used as payment method
 */
public class FinAccountPaymentServices {

    private static final String MODULE = FinAccountPaymentServices.class.getName();
    private static final String RES_ERROR = x.AccountingErrorUiLabels;

    // base payment integration services
    public static Map<String, Object> finAccountPreAuth(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String finAccountCode = (String) context.get(x.finAccountCode);
        String finAccountPin = (String) context.get(x.finAccountPin);
        String finAccountId = (String) context.get(x.finAccountId);
        String orderId = (String) context.get(x.orderId);
        BigDecimal amount = (BigDecimal) context.get(x.processAmount);

        if (paymentPref != null) {
            // check for an existing auth trans and cancel it
            GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTrans != null) {
                Map<String, Object> input = UtilMisc.toMap(x.userLogin, userLogin, x.finAccountAuthId,
                        authTrans.get(x.referenceNum));
                try {
                    Map<String, Object> result = dispatcher.runSync(x.expireFinAccountAuth, input);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            if (finAccountId == null) {
                finAccountId = paymentPref.getString(x.finAccountId);
            }
        }

        // obtain the order information
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);

        // NOTE DEJ20070808: this means that we want store related settings for where
        // the item is being purchased,
        // NOT where the account was setup; should this be changed to use settings from
        // the store where the account was setup?
        String productStoreId = orh.getProductStoreId();

        // TODO, NOTE DEJ20070808: why is this setup this way anyway? for the
        // allowAuthToNegative wouldn't that be better setup
        // on the FinAccount and not on the ProductStoreFinActSetting? maybe an override
        // on the FinAccount would be good...

        // get the financial account
        GenericValue finAccount;
        if (finAccountId != null) {
            try {
                UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
                finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            if (finAccountCode != null) {
                try {
                    finAccount = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingFinAccountCannotLocateItFromAccountCode, locale));
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountIdAndFinAccountCodeAreNull, locale));
            }
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountIdInvalid, locale));
        }

        String finAccountTypeId = finAccount.getString(x.finAccountTypeId);
        finAccountId = finAccount.getString(x.finAccountId);
        String statusId = finAccount.getString(x.statusId);

        try {
            // fin the store requires a pin number; validate the PIN with the code
            Map<String, Object> findProductStoreFinActSettingMap = UtilMisc.<String, Object>toMap(x.productStoreId,
                    productStoreId, x.finAccountTypeId, finAccountTypeId);
            UserLoginDao productStoreFinActSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class);
            GenericValue finAccountSettings = productStoreFinActSettingDao.findOne(delegator, x.ProductStoreFinActSetting,
                    findProductStoreFinActSettingMap, true);

            if (finAccountSettings == null) {
                Debug.logWarning(
                        x.In_finAccountPreAuth_could_not_find_ProductStoreFinActSetting_record_values_searched_by
                                + findProductStoreFinActSettingMap, MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.In_finAccountPreAuth_finAccountSettings + finAccountSettings, MODULE);
            }

            BigDecimal minBalance = FinAccountHelper.getZero();
            String allowAuthToNegative = x.N;

            if (finAccountSettings != null) {
                allowAuthToNegative = finAccountSettings.getString(x.allowAuthToNegative);
                minBalance = finAccountSettings.getBigDecimal(x.minBalance);
                if (minBalance == null) {
                    minBalance = FinAccountHelper.getZero();
                }

                // validate the PIN if the store requires it
                if (x.Y.equals(finAccountSettings.getString(x.requirePinCode))) {
                    if (!FinAccountHelper.validatePin(delegator, finAccountCode, finAccountPin)) {
                        Map<String, Object> result = ServiceUtil.returnSuccess();
                        result.put(x.authMessage, UtilProperties.getMessage(RES_ERROR,
                                x.AccountingFinAccountPinCodeCombinatorNotFound, locale));
                        result.put(x.authResult, Boolean.FALSE);
                        result.put(x.processAmount, amount);
                        result.put(x.authFlag, x._0);
                        result.put(x.authCode, x.A);
                        result.put(x.authRefNum, x._0);
                        Debug.logWarning(x.Unable_to_auth_FinAccount + result, MODULE);
                        return result;
                    }
                }
            }

            // check for expiration date
            if ((finAccount.getTimestamp(x.thruDate) != null) && (finAccount.getTimestamp(x.thruDate).before(
                    UtilDateTime.nowTimestamp()))) {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put(x.authMessage, UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountExpired,
                        UtilMisc.toMap(x.thruDate, finAccount.getTimestamp(x.thruDate)), locale));
                result.put(x.authResult, Boolean.FALSE);
                result.put(x.processAmount, amount);
                result.put(x.authFlag, x._0);
                result.put(x.authCode, x.A);
                result.put(x.authRefNum, x._0);
                Debug.logWarning(x.Unable_to_auth_FinAccount + result, MODULE);
                return result;
            }

            // check for account being in bad standing somehow
            if (x.FNACT_NEGPENDREPL.equals(statusId) || x.FNACT_MANFROZEN.equals(statusId) || x.FNACT_CANCELLED.equals(
                    statusId)) {
                // refresh the finaccount
                finAccount.refresh();
                statusId = finAccount.getString(x.statusId);

                if (x.FNACT_NEGPENDREPL.equals(statusId) || x.FNACT_MANFROZEN.equals(statusId) || x.FNACT_CANCELLED
                        .equals(statusId)) {
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    if (x.FNACT_NEGPENDREPL.equals(statusId)) {
                        result.put(x.authMessage, UtilProperties.getMessage(RES_ERROR,
                                x.AccountingFinAccountNegative, locale));
                    } else if (x.FNACT_MANFROZEN.equals(statusId)) {
                        result.put(x.authMessage, UtilProperties.getMessage(RES_ERROR,
                                x.AccountingFinAccountFrozen, locale));
                    } else if (x.FNACT_CANCELLED.equals(statusId)) {
                        result.put(x.authMessage, UtilProperties.getMessage(RES_ERROR,
                                x.AccountingFinAccountCancelled, locale));
                    }
                    result.put(x.authResult, Boolean.FALSE);
                    result.put(x.processAmount, amount);
                    result.put(x.authFlag, x._0);
                    result.put(x.authCode, x.A);
                    result.put(x.authRefNum, x._0);
                    Debug.logWarning(x.Unable_to_auth_FinAccount + result, MODULE);
                    return result;
                }
            }

            // check the amount to authorize against the available balance of fin account,
            // which includes active authorizations as well as transactions
            BigDecimal availableBalance = finAccount.getBigDecimal(x.availableBalance);
            if (availableBalance == null) {
                availableBalance = FinAccountHelper.getZero();
            } else {
                BigDecimal availableBalanceOriginal = availableBalance;
                availableBalance = availableBalance.setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());
                if (availableBalance.compareTo(availableBalanceOriginal) != 0) {
                    Debug.logWarning(x.In_finAccountPreAuth_for_finAccountId + finAccountId + x.availableBalance_6381e7a4
                            + availableBalanceOriginal + x.was_different_after_rounding + availableBalance
                            + x.it_should_never_have_made_it_into_the_database_this_way_so_check_whatever_put_it_there,
                            MODULE);
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            String authMessage = null;
            Boolean processResult;
            String refNum;

            // make sure to round and scale it to the same as availableBalance
            amount = amount.setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());

            Debug.logInfo(x.Allow_auth_to_negative + allowAuthToNegative + x.available + availableBalance + x.comp
                    + minBalance + x.str + availableBalance.compareTo(minBalance) + x.req + amount, MODULE);
            // check the available balance to see if we can auth this tx
            if ((x.Y.equals(allowAuthToNegative) && availableBalance.compareTo(minBalance) > -1)
                    || (availableBalance.compareTo(amount) > -1)) {
                Timestamp thruDate;

                if (finAccountSettings != null && finAccountSettings.getLong(x.authValidDays) != null) {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), finAccountSettings.getLong(x.authValidDays));
                } else {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), 30L); // default 30 days for an auth
                }

                Map<String, Object> tmpResult = dispatcher.runSync(x.createFinAccountAuth, UtilMisc.<String, Object>toMap(x.finAccountId,
                        finAccountId, x.amount, amount, x.thruDate, thruDate, x.userLogin, userLogin));

                if (ServiceUtil.isError(tmpResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                }
                refNum = (String) tmpResult.get(x.finAccountAuthId);
                processResult = Boolean.TRUE;

                // refresh the account
                finAccount.refresh();
            } else {
                Debug.logWarning(x.Attempted_to_authorize + amount + x.against_a_balance_of_only
                        + availableBalance + x.for_finAccountId + finAccountId + x.str_4ff447b8, MODULE);
                refNum = x._0; // a refNum is always required from authorization
                authMessage = x.Insufficient_funds;
                processResult = Boolean.FALSE;
            }

            result.put(x.processAmount, amount);
            result.put(x.authMessage, authMessage);
            result.put(x.authResult, processResult);
            result.put(x.authFlag, x._1);
            result.put(x.authCode, x.A);
            result.put(x.authRefNum, refNum);
            Debug.logInfo(x.FinAccont_Auth + result, MODULE);

            return result;
        } catch (GenericEntityException | GenericServiceException ex) {
            Debug.logError(ex, x.Cannot_authorize_financial_account, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountCannotBeAuthorized,
                    UtilMisc.toMap(x.errorString, ex.getMessage()), locale));
        }
    }

    public static Map<String, Object> finAccountReleaseAuth(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);

        String err = UtilProperties.getMessage(RES_ERROR, x.AccountingFinAccountCannotBeExpired, locale);
        try {

            // expire the related financial authorization transaction
            GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTransaction == null) {
                return ServiceUtil.returnError(err + UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountCannotFindAuthorization, locale));
            }

            Map<String, Object> input = UtilMisc.toMap(x.userLogin, userLogin, x.finAccountAuthId, authTransaction.get(x.referenceNum));
            Map<String, Object> serviceResults = dispatcher.runSync(x.expireFinAccountAuth, input);
            // if there's an error, don't release
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(err + ServiceUtil.getErrorMessage(serviceResults));
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

    public static Map<String, Object> finAccountCapture(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue authTrans = (GenericValue) context.get(x.authTrans);
        BigDecimal amount = (BigDecimal) context.get(x.captureAmount);
        String currency = (String) context.get(x.currency);

        // get the authorization transaction
        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountCannotCapture, locale));
        }

        // get the auth record
        String finAccountAuthId = authTrans.getString(x.referenceNum);
        GenericValue finAccountAuth;
        try {
            UserLoginDao finAccountAuthDao = DaoRegistry.getDao(delegator, x.FinAccountAuth, UserLoginDao.class);
            finAccountAuth = finAccountAuthDao.findOne(delegator, x.FinAccountAuth, UtilMisc.toMap(x.finAccountAuthId, finAccountAuthId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Debug.logInfo(x.Financial_account_capture + finAccountAuth.get(x.finAccountId) + x.for_the_amount_of
                + amount + x.Tx + finAccountAuth.get(x.finAccountAuthId), MODULE);

        // get the financial account
        GenericValue finAccount;
        try {
            finAccount = finAccountAuth.getRelatedOne(x.FinAccount, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure authorization has not expired
        Timestamp authExpiration = finAccountAuth.getTimestamp(x.thruDate);
        if ((authExpiration != null) && (authExpiration.before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountAuthorizationExpired,
                    UtilMisc.toMap(x.paymentGatewayResponseId, authTrans.getString(x.paymentGatewayResponseId),
                            x.authExpiration, authExpiration), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp(x.thruDate) != null) && (finAccount.getTimestamp(x.thruDate).before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountExpired,
                    UtilMisc.toMap(x.thruDate, finAccount.getTimestamp(x.thruDate)), locale));
        }
        String finAccountId = finAccount.getString(x.finAccountId);

        // need the product store ID & party ID
        String orderId = orderPaymentPreference.getString(x.orderId);
        String productStoreId = null;
        String partyId = null;
        if (orderId != null) {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            productStoreId = orh.getProductStoreId();

            GenericValue billToParty = orh.getBillToParty();
            if (billToParty != null) {
                partyId = billToParty.getString(x.partyId);
            }
        }

        // BIG NOTE: make sure the expireFinAccountAuth and finAccountWithdraw services are done in the SAME TRANSACTION
        //(i.e. no require-new-transaction in either of them AND no running async)

        // cancel the authorization before doing the withdraw to avoid problems with way negative available amount on account;
        // should happen in same transaction to avoid conflict problems
        Map<String, Object> releaseResult;
        try {
            releaseResult = dispatcher.runSync(x.expireFinAccountAuth, UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.finAccountAuthId,
                    finAccountAuthId));
            if (ServiceUtil.isError(releaseResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // build the withdraw context
        Map<String, Object> withdrawCtx = new HashMap<>();
        withdrawCtx.put(x.finAccountId, finAccountId);
        withdrawCtx.put(x.productStoreId, productStoreId);
        withdrawCtx.put(x.currency, currency);
        withdrawCtx.put(x.partyId, partyId);
        withdrawCtx.put(x.orderId, orderId);
        withdrawCtx.put(x.amount, amount);
        withdrawCtx.put(x.reasonEnumId, x.FATR_PURCHASE);
        withdrawCtx.put(x.requireBalance, Boolean.FALSE); // for captures; if auth passed, allow
        withdrawCtx.put(x.userLogin, userLogin);

        // call the withdraw service
        Map<String, Object> withdrawResp;
        try {
            withdrawResp = dispatcher.runSync(x.finAccountWithdraw, withdrawCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(withdrawResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(withdrawResp));
        }

        // create the capture response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) withdrawResp.get(x.processResult);
        BigDecimal withdrawAmount = (BigDecimal) withdrawResp.get(x.amount);
        String referenceNum = (String) withdrawResp.get(x.referenceNum);
        result.put(x.captureResult, processResult);
        result.put(x.captureRefNum, referenceNum);
        result.put(x.captureCode, x.C);
        result.put(x.captureFlag, x._1);
        result.put(x.captureAmount, withdrawAmount);

        return result;
    }

    public static Map<String, Object> finAccountRefund(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        BigDecimal amount = (BigDecimal) context.get(x.refundAmount);
        String currency = (String) context.get(x.currency);
        String finAccountId = (String) context.get(x.finAccountId);

        String productStoreId = null;
        String partyId = null;

        String orderId = null;
        if (orderPaymentPreference != null) {
            orderId = orderPaymentPreference.getString(x.orderId);
            if (orderId != null) {
                OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
                productStoreId = orh.getProductStoreId();

                GenericValue billToParty = orh.getBillToParty();
                if (billToParty != null) {
                    partyId = billToParty.getString(x.partyId);
                }
            }
            if (finAccountId == null) {
                finAccountId = orderPaymentPreference.getString(x.finAccountId);
            }
        }

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
        }

        // call the deposit service
        Map<String, Object> depositCtx = new HashMap<>();
        depositCtx.put(x.finAccountId, finAccountId);
        depositCtx.put(x.productStoreId, productStoreId);
        depositCtx.put(x.isRefund, Boolean.TRUE);
        depositCtx.put(x.currency, currency);
        depositCtx.put(x.partyId, partyId);
        depositCtx.put(x.orderId, orderId);
        depositCtx.put(x.amount, amount);
        depositCtx.put(x.reasonEnumId, x.FATR_REFUND);
        depositCtx.put(x.userLogin, userLogin);

        Map<String, Object> depositResp;
        try {
            depositResp = dispatcher.runSync(x.finAccountDeposit, depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depositResp));
        }

        // create the refund response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) depositResp.get(x.processResult);
        BigDecimal depositAmount = (BigDecimal) depositResp.get(x.amount);
        String referenceNum = (String) depositResp.get(x.referenceNum);
        result.put(x.refundResult, processResult);
        result.put(x.refundRefNum, referenceNum);
        result.put(x.refundCode, x.R);
        result.put(x.refundFlag, x._1);
        result.put(x.refundAmount, depositAmount);

        return result;
    }

    // base account transaction services
    public static Map<String, Object> finAccountWithdraw(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String finAccountId = (String) context.get(x.finAccountId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);
        String reasonEnumId = (String) context.get(x.reasonEnumId);
        String orderId = (String) context.get(x.orderId);
        Boolean requireBalance = (Boolean) context.get(x.requireBalance);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        if (requireBalance == null) {
            requireBalance = Boolean.TRUE;
        }

        final String withdrawal = x.WITHDRAWAL;

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

        GenericValue finAccount;
        try {
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp(x.thruDate) != null) && (finAccount.getTimestamp(x.thruDate).before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountExpired,
                    UtilMisc.toMap(x.thruDate, finAccount.getTimestamp(x.thruDate)), locale));
        }

        // check the actual balance (excluding authorized amounts) and create the
        // transaction if it is sufficient
        BigDecimal previousBalance = finAccount.getBigDecimal(x.actualBalance);
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.getZero();
        }

        BigDecimal balance;
        String refNum;
        Boolean procResult;
        if (requireBalance && previousBalance.compareTo(amount) < 0) {
            procResult = Boolean.FALSE;
            balance = previousBalance;
            refNum = x.N_A;
        } else {
            try {
                refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin,
                        amount,
                        productStoreId, partyId, orderId, orderItemSeqId, currencyUom, withdrawal, finAccountId,
                        reasonEnumId);
                finAccount.refresh();
                balance = finAccount.getBigDecimal(x.actualBalance);
                procResult = Boolean.TRUE;
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // make sure balance is not null
        if (balance == null) {
            balance = FinAccountHelper.getZero();
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.previousBalance, previousBalance);
        result.put(x.balance, balance);
        result.put(x.amount, amount);
        result.put(x.processResult, procResult);
        result.put(x.referenceNum, refNum);
        return result;
    }

    // base deposit service
    public static Map<String, Object> finAccountDeposit(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String finAccountId = (String) context.get(x.finAccountId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);
        String reasonEnumId = (String) context.get(x.reasonEnumId);
        String orderId = (String) context.get(x.orderId);
        Boolean isRefund = (Boolean) context.get(x.isRefund);
        BigDecimal amount = (BigDecimal) context.get(x.amount);

        final String deposit = isRefund == null || !isRefund ? x.DEPOSIT : x.ADJUSTMENT;

        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }
        String currencyUom = (String) context.get(x.currency);
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        GenericValue finAccount;
        try {
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, finAccountId), locale));
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp(x.thruDate) != null) && (finAccount.getTimestamp(x.thruDate).before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountExpired,
                    UtilMisc.toMap(x.thruDate, finAccount.getTimestamp(x.thruDate)), locale));
        }
        Debug.logInfo(x.Deposit_into_financial_account + finAccountId + x.str_42cbdb3c + amount + x.str_4ff447b8, MODULE);

        // get the previous balance
        BigDecimal previousBalance = finAccount.getBigDecimal(x.actualBalance);
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.getZero();
        }

        // create the transaction
        BigDecimal actualBalance;
        String refNum;
        try {
            refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin, amount,
                    productStoreId, partyId, orderId, orderItemSeqId, currencyUom, deposit, finAccountId, reasonEnumId);
            finAccount.refresh();
            actualBalance = finAccount.getBigDecimal(x.actualBalance);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure balance is not null
        if (actualBalance == null) {
            actualBalance = FinAccountHelper.getZero();
        } else {
            if (actualBalance.compareTo(BigDecimal.ZERO) < 0) {
                // balance went below zero, set negative pending replenishment status so that no
                // more auths or captures will go through until it is replenished
                try {
                    Map<String, Object> rollbackCtx = UtilMisc.toMap(x.userLogin, userLogin, x.finAccountId,
                            finAccountId, x.statusId, x.FNACT_NEGPENDREPL);
                    dispatcher.addRollbackService(x.updateFinAccount, rollbackCtx, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.previousBalance, previousBalance);
        result.put(x.balance, actualBalance);
        result.put(x.amount, amount);
        result.put(x.processResult, Boolean.TRUE);
        result.put(x.referenceNum, refNum);
        return result;
    }

    // auto-replenish service (deposit)
    public static Map<String, Object> finAccountReplenish(DispatchContext dctx, FinAccountPaymentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String finAccountId = (String) context.get(x.finAccountId);

        // lookup the FinAccount
        GenericValue finAccount;
        try {
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, finAccountId), locale));
        }
        String currency = finAccount.getString(x.currencyUomId);
        String statusId = finAccount.getString(x.statusId);

        // look up the type -- determine auto-replenish is active
        GenericValue finAccountType;
        try {
            finAccountType = finAccount.getRelatedOne(x.FinAccountType, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String replenishEnumId = finAccountType.getString(x.replenishEnumId);
        if (!x.FARP_AUTOMATIC.equals(replenishEnumId)) {
            // type does not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // attempt to lookup the product store from a previous deposit
        if (productStoreId == null) {
            productStoreId = getLastProductStoreId(delegator, finAccountId);
            if (productStoreId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountCannotBeReplenish, locale));
            }
        }

        // get the product store settings
        GenericValue finAccountSettings;
        Map<String, Object> psfasFindMap = UtilMisc.<String, Object>toMap(x.productStoreId, productStoreId,
                x.finAccountTypeId, finAccount.getString(x.finAccountTypeId));
        try {
            UserLoginDao productStoreFinActSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class);
            finAccountSettings = productStoreFinActSettingDao.findOne(delegator, x.ProductStoreFinActSetting, psfasFindMap, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccountSettings == null) {
            Debug.logWarning(x.finAccountReplenish_Warning_not_replenishing_FinAccount + finAccountId
                    + x.because_no_ProductStoreFinActSetting_record_found_for + psfasFindMap, MODULE);
            // no settings; don't replenish
            return ServiceUtil.returnSuccess();
        }

        BigDecimal replenishThreshold = finAccountSettings.getBigDecimal(x.replenishThreshold);
        if (replenishThreshold == null) {
            Debug.logWarning(x.finAccountReplenish_Warning_not_replenishing_FinAccount + finAccountId
                    + x.because_ProductStoreFinActSetting_replenishThreshold_field_was_null_for + psfasFindMap,
                    MODULE);
            return ServiceUtil.returnSuccess();
        }

        BigDecimal replenishLevel = finAccount.getBigDecimal(x.replenishLevel);
        if (replenishLevel == null || replenishLevel.compareTo(BigDecimal.ZERO) == 0) {
            Debug.logWarning(x.finAccountReplenish_Warning_not_replenishing_FinAccount + finAccountId
                    + x.because_FinAccount_replenishLevel_field_was_null_or_0, MODULE);
            // no replenish level set; this account goes not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // get the current balance
        BigDecimal balance = finAccount.getBigDecimal(x.actualBalance);

        // see if we are within the threshold for replenishment
        if (balance.compareTo(replenishThreshold) > -1) {
            Debug.logInfo(x.finAccountReplenish_Info_Not_replenishing_FinAccount + finAccountId
                    + x.because_balance + balance + x.is_greater_than_the_replenishThreshold
                    + replenishThreshold + x.str_4ff447b8, MODULE);
            // not ready
            return ServiceUtil.returnSuccess();
        }

        // configure rollback service to set status to Negative Pending Replenishment
        if (x.FNACT_NEGPENDREPL.equals(statusId)) {
            try {
                Map<String, Object> rollbackCtx = UtilMisc.toMap(x.userLogin, userLogin, x.finAccountId, finAccountId,
                        x.statusId, x.FNACT_NEGPENDREPL);
                dispatcher.addRollbackService(x.updateFinAccount, rollbackCtx, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        String replenishMethod = finAccountSettings.getString(x.replenishMethodEnumId);
        BigDecimal depositAmount;
        if (replenishMethod == null || x.FARP_TOP_OFF.equals(replenishMethod)) {
            // the deposit is level - balance (500 - (-10) = 510 || 500 - (10) = 490)
            depositAmount = replenishLevel.subtract(balance);
        } else if (x.FARP_REPLENISH_LEVEL.equals(replenishMethod)) {
            // the deposit is replenish-level itself
            depositAmount = replenishLevel;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountUnknownReplenishMethod, locale));
        }

        // get the owner party
        String ownerPartyId = finAccount.getString(x.ownerPartyId);
        if (ownerPartyId == null) {
            // no owner cannot replenish; (not fatal, just not supported by this account)
            Debug.logWarning(x.finAccountReplenish_Warning_No_owner_attached_to_financial_account + finAccountId
                    + x.cannot_auto_replenish, MODULE);
            return ServiceUtil.returnSuccess();
        }

        // get the payment method to use to replenish
        String paymentMethodId = finAccount.getString(x.replenishPaymentId);
        if (paymentMethodId == null) {
            Debug.logWarning(
                    x.finAccountReplenish_Warning_No_payment_method_replenishPaymentId_attached_to_financial_account
                            + finAccountId + x.cannot_auto_replenish, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNoPaymentMethodAssociatedWithReplenishAccount, locale));
        }

        GenericValue paymentMethod;
        try {
            UserLoginDao paymentMethodDao = DaoRegistry.getDao(delegator, x.PaymentMethod, UserLoginDao.class);
            paymentMethod = paymentMethodDao.findOne(delegator, x.PaymentMethod, UtilMisc.toMap(x.paymentMethodId, paymentMethodId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (paymentMethod == null) {
            // no payment methods on file; cannot replenish
            Debug.logWarning(x.finAccountReplenish_Warning_No_payment_method_found_for_ID + paymentMethodId
                    + x.for_party + ownerPartyId + x.cannot_auto_replenish, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNoPaymentMethodAssociatedWithReplenishAccount, locale));
        }

        // hit the payment method for the amount to replenish
        Map<String, BigDecimal> orderItemMap = UtilMisc.toMap(x.Auto_Replenishment_FA + finAccountId, depositAmount);
        Map<String, Object> replOrderCtx = new HashMap<>();
        replOrderCtx.put(x.productStoreId, productStoreId);
        replOrderCtx.put(x.paymentMethodId, paymentMethod.getString(x.paymentMethodId));
        replOrderCtx.put(x.currency, currency);
        replOrderCtx.put(x.partyId, ownerPartyId);
        replOrderCtx.put(x.itemMap, orderItemMap);
        replOrderCtx.put(x.userLogin, userLogin);
        Map<String, Object> replResp;
        try {
            replResp = dispatcher.runSync(x.createSimpleNonProductSalesOrder, replOrderCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(replResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(replResp));
        }
        String orderId = (String) replResp.get(x.orderId);

        // create the deposit
        Map<String, Object> depositCtx = new HashMap<>();
        depositCtx.put(x.productStoreId, productStoreId);
        depositCtx.put(x.finAccountId, finAccountId);
        depositCtx.put(x.currency, currency);
        depositCtx.put(x.partyId, ownerPartyId);
        depositCtx.put(x.orderId, orderId);
        depositCtx.put(x.orderItemSeqId, x._00001); // always one item on a replish order
        depositCtx.put(x.amount, depositAmount);
        depositCtx.put(x.reasonEnumId, x.FATR_REPLENISH);
        depositCtx.put(x.userLogin, userLogin);
        try {
            Map<String, Object> depositResp = dispatcher.runSync(x.finAccountDeposit, depositCtx);
            if (ServiceUtil.isError(depositResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depositResp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // say we are in good standing again
        if (x.FNACT_NEGPENDREPL.equals(statusId)) {
            try {
                Map<String, Object> ufaResp = dispatcher.runSync(x.updateFinAccount,
                        UtilMisc.<String, Object>toMap(x.finAccountId, finAccountId, x.statusId, x.FNACT_ACTIVE, x.userLogin, userLogin));
                if (ServiceUtil.isError(ufaResp)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(ufaResp));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    private static String getLastProductStoreId(Delegator delegator, String finAccountId) {
        GenericValue trans = null;
        try {
            UserLoginDao finAccountTransDao = DaoRegistry.getDao(delegator, x.FinAccountTrans, UserLoginDao.class);
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(x.finAccountTransTypeId, EntityOperator.EQUALS, x.DEPOSIT),
                    EntityCondition.makeCondition(x.finAccountId, EntityOperator.EQUALS, finAccountId),
                    EntityCondition.makeCondition(x.orderId, EntityOperator.NOT_EQUAL, null)), EntityOperator.AND);
            trans = finAccountTransDao.findFirstByCondition(delegator, x.FinAccountTrans, condition, null, UtilMisc.toList(x.transactionDate_5a2f7760),
                    false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (trans != null) {
            String orderId = trans.getString(x.orderId);
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            return orh.getProductStoreId();
        }

        // none found; pick one from our set stores
        try {
            UserLoginDao productStoreDao = DaoRegistry.getDao(delegator, x.ProductStore, UserLoginDao.class);
            GenericValue store = productStoreDao.findFirstByCondition(delegator, x.ProductStore, null, null, UtilMisc.toList(x.productStoreId),
                    false);
            if (store != null) {
                return store.getString(x.productStoreId);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return null;
    }

    private static String createFinAcctPaymentTransaction(Delegator delegator, LocalDispatcher dispatcher,
            GenericValue userLogin, BigDecimal amount,
            String productStoreId, String partyId, String orderId, String orderItemSeqId, String currencyUom,
            String txType, String finAccountId, String reasonEnumId) throws GeneralException {

        final String coParty = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
        final String paymentMethodType = x.FIN_ACCOUNT;

        if (UtilValidate.isEmpty(partyId)) {
            partyId = x.NA;
        }

        String paymentType;
        String partyIdFrom;
        String partyIdTo;
        BigDecimal paymentAmount;

        // determine the payment type and which direction the parties should go
        if (x.DEPOSIT.equals(txType)) {
            paymentType = x.RECEIPT;
            partyIdFrom = partyId;
            partyIdTo = coParty;
            paymentAmount = amount;
        } else if (x.WITHDRAWAL.equals(txType)) {
            paymentType = x.DISBURSEMENT;
            partyIdFrom = coParty;
            partyIdTo = partyId;
            paymentAmount = amount;
        } else if (x.ADJUSTMENT.equals(txType)) {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                paymentType = x.DISBURSEMENT;
                partyIdFrom = coParty;
                partyIdTo = partyId;
                paymentAmount = amount.negate(); // must be positive
            } else {
                paymentType = x.RECEIPT;
                partyIdFrom = partyId;
                partyIdTo = coParty;
                paymentAmount = amount;
            }
        } else {
            throw new GeneralException(x.Unable_to_create_financial_account_transaction);
        }

        // payment amount should always be positive; adjustments may
        // create the payment for the transaction
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap(x.paymentTypeId, paymentType);
        paymentCtx.put(x.paymentMethodTypeId, paymentMethodType);
        paymentCtx.put(x.partyIdTo, partyIdTo);
        paymentCtx.put(x.partyIdFrom, partyIdFrom);
        paymentCtx.put(x.statusId, x.PMNT_RECEIVED);
        paymentCtx.put(x.currencyUomId, currencyUom);
        paymentCtx.put(x.amount, paymentAmount);
        paymentCtx.put(x.userLogin, userLogin);
        paymentCtx.put(x.paymentRefNum, Long.toString(UtilDateTime.nowTimestamp().getTime()));

        String paymentId;
        Map<String, Object> payResult;
        try {
            payResult = dispatcher.runSync(x.createPayment, paymentCtx);
            if (ServiceUtil.isError(payResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(payResult));
            }
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
        transCtx.put(x.partyId, partyId);
        transCtx.put(x.orderId, orderId);
        transCtx.put(x.orderItemSeqId, orderItemSeqId);
        transCtx.put(x.reasonEnumId, reasonEnumId);
        transCtx.put(x.amount, amount);
        transCtx.put(x.userLogin, userLogin);
        transCtx.put(x.paymentId, paymentId);

        Map<String, Object> transResult;
        try {
            transResult = dispatcher.runSync(x.createFinAccountTrans, transCtx);
            if (ServiceUtil.isError(transResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
            }
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (transResult == null) {
            throw new GeneralException(x.Unknown_error_in_creating_financial_account_transaction);
        }
        if (ServiceUtil.isError(transResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
        }

        return (String) transResult.get(x.finAccountTransId);
    }
}
