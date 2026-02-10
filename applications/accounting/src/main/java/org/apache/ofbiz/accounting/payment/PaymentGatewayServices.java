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
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.accounting.invoice.InvoiceWorker;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PaymentGatewayServicesContext;
/**
 * PaymentGatewayServices
 */
public class PaymentGatewayServices {

    private static final String MODULE = PaymentGatewayServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final String RES_ERROR = x.AccountingErrorUiLabels;
    private static final String RES_ORDER = x.OrderUiLabels;

    private static final String AUTH_SERVICE_TYPE = x.PRDS_PAY_AUTH;
    private static final String REAUTH_SERVICE_TYPE = x.PRDS_PAY_REAUTH;
    private static final String RELEASE_SERVICE_TYPE = x.PRDS_PAY_RELEASE;
    private static final String CAPTURE_SERVICE_TYPE = x.PRDS_PAY_CAPTURE;
    private static final String REFUND_SERVICE_TYPE = x.PRDS_PAY_REFUND;
    private static final String CREDIT_SERVICE_TYPE = x.PRDS_PAY_CREDIT;
    private static final int TX_TIME = 300;

    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.order_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.order_rounding);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Authorizes a single order preference with an option to specify an amount. The result map has the Booleans
     * "errors" and "finished" which notify the user if there were any errors and if the authorization was finished.
     * There is also a List "messages" for the authorization response messages and a BigDecimal, "processAmount" as the
     * amount processed.
     * TODO: it might be nice to return the paymentGatewayResponseId
     */
    public static Map<String, Object> authOrderPaymentPreference(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String orderPaymentPreferenceId = (String) context.get(x.orderPaymentPreferenceId);
        BigDecimal overrideAmount = (BigDecimal) context.get(x.overrideAmount);

        // validate overrideAmount if its available
        if (overrideAmount != null) {
            if (overrideAmount.compareTo(BigDecimal.ZERO) < 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingPaymentAmountIsNegative,
                        UtilMisc.toMap(x.overrideAmount, overrideAmount), locale));
            }
            if (overrideAmount.compareTo(BigDecimal.ZERO) == 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingPaymentAmountIsZero,
                        UtilMisc.toMap(x.overrideAmount, overrideAmount), locale));
            }
        }

        GenericValue orderHeader = null;
        GenericValue orderPaymentPreference = null;
        try {
            orderPaymentPreference = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findOne(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceId), false);
            orderHeader = orderPaymentPreference.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the total remaining
        BigDecimal totalRemaining = orh.getOrderGrandTotal();

        // get the process attempts so far
        Long procAttempt = orderPaymentPreference.getLong(x.processAttempt);
        if (procAttempt == null) {
            procAttempt = 0L;
        }

        // update the process attempt count
        orderPaymentPreference.set(x.processAttempt, procAttempt + 1);
        try {
            orderPaymentPreference.store();
            orderPaymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale));
        }

        // if we are already authorized, then this is a re-auth request
        boolean reAuth = false;
        if (orderPaymentPreference.get(x.statusId) != null && x.PAYMENT_AUTHORIZED.equals(orderPaymentPreference.getString(x.statusId))) {
            reAuth = true;
        }

        // use overrideAmount or maxAmount
        BigDecimal transAmount = null;
        if (overrideAmount != null) {
            transAmount = overrideAmount;
        } else {
            transAmount = orderPaymentPreference.getBigDecimal(x.maxAmount);
        }

        // round this before moving on just in case a funny number made it this far
        transAmount = transAmount.setScale(DECIMALS, ROUNDING);

        // if our transaction amount exists and is zero, there's nothing to process, so return
        if ((transAmount != null) && (transAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put(x.finished, Boolean.TRUE); // finished is true since there is nothing to do
            results.put(x.errors, Boolean.FALSE); // errors is false since no error occurred
            return results;
        }

        try {
            // call the authPayment method
            Map<String, Object> authPaymentResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference, totalRemaining, reAuth,
                    transAmount);

            // handle the response
            if (authPaymentResult != null) {
                // not null result means either an approval or decline; null would mean error
                BigDecimal thisAmount = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(authPaymentResult.get(x.processAmount), x.BigDecimal,
                        null, locale);

                // process the auth results
                try {
                    boolean processResult = processResult(dctx, authPaymentResult, userLogin, orderPaymentPreference, locale);
                    if (processResult) {
                        Map<String, Object> results = ServiceUtil.returnSuccess();
                        results.put(x.messages, authPaymentResult.get(x.customerRespMsgs));
                        results.put(x.processAmount, thisAmount);
                        results.put(x.finished, Boolean.TRUE);
                        results.put(x.errors, Boolean.FALSE);
                        results.put(x.authCode, authPaymentResult.get(x.authCode));
                        return results;
                    } else {
                        boolean needsNsfRetry = needsNsfRetry(orderPaymentPreference, authPaymentResult, delegator);

                        // if we are doing an NSF retry then also...
                        // if (needsNsfRetry) {
                            // TODO: what do we do with this? we need to fail the auth but still allow the order through so it can be fixed later
                            // NOTE: this is called through a different path for auto re-orders, so it should be good to go...
                            // will leave this comment here just in case...
                        //}

                        // if we have a failure at this point and no NSF retry is needed, then try other credit cards on file, if the user has any
                        if (!needsNsfRetry) {
                            // is this an auto-order?
                            if (UtilValidate.isNotEmpty(orderHeader.getString(x.autoOrderShoppingListId))) {
                                GenericValue productStore = orderHeader.getRelatedOne(x.ProductStore, false);
                                // according to the store should we try other cards?
                                if (x.Y.equals(productStore.getString(x.autoOrderCcTryOtherCards))) {
                                    // get other credit cards for the bill to party
                                    List<GenericValue> otherPaymentMethodAndCreditCardList = null;
                                    String billToPartyId = null;
                                    GenericValue billToParty = orh.getBillToParty();
                                    if (billToParty != null) {
                                        billToPartyId = billToParty.getString(x.partyId);
                                    //} else {
                                        // TODO optional: any other ways to find the bill to party? perhaps look at info from OrderPaymentPreference,
                                        //  ie search back from other PaymentMethod...
                                    }

                                    if (UtilValidate.isNotEmpty(billToPartyId)) {
                                        otherPaymentMethodAndCreditCardList = DaoRegistry.getDao(delegator, x.PaymentMethodAndCreditCard,
                                                UserLoginDao.class)
                                                .findByAnd(delegator, x.PaymentMethodAndCreditCard,
                                                        UtilMisc.toMap(x.partyId, billToPartyId, x.paymentMethodTypeId, x.CREDIT_CARD), null, false);
                                        otherPaymentMethodAndCreditCardList = EntityUtil.filterByDate(otherPaymentMethodAndCreditCardList);
                                    }

                                    if (UtilValidate.isNotEmpty(otherPaymentMethodAndCreditCardList)) {
                                        for (GenericValue otherPaymentMethodAndCreditCard : otherPaymentMethodAndCreditCardList) {
                                            // change OrderPaymentPreference in memory only and call auth service
                                            orderPaymentPreference.set(x.paymentMethodId, otherPaymentMethodAndCreditCard
                                                    .getString(x.paymentMethodId));
                                            Map<String, Object> authRetryResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference,
                                                    totalRemaining, reAuth, transAmount);
                                            try {
                                                boolean processRetryResult = processResult(dctx, authPaymentResult, userLogin,
                                                        orderPaymentPreference, locale);

                                                if (processRetryResult) {
                                                    // wow, we got here that means the other card was successful...
                                                    // on success save the OrderPaymentPreference, and then return finished
                                                    // (which will break from loop)
                                                    orderPaymentPreference.store();

                                                    Map<String, Object> results = ServiceUtil.returnSuccess();
                                                    results.put(x.messages, authRetryResult.get(x.customerRespMsgs));
                                                    results.put(x.processAmount, thisAmount);
                                                    results.put(x.finished, Boolean.TRUE);
                                                    results.put(x.errors, Boolean.FALSE);
                                                    return results;
                                                }
                                            } catch (GeneralException e) {
                                                String errMsg = x.Error_saving_and_processing_payment_authorization_results + e.toString();
                                                Debug.logError(e, errMsg + x.authRetryResult + authRetryResult, MODULE);
                                                Map<String, Object> results = ServiceUtil.returnSuccess();
                                                results.put(ModelService.ERROR_MESSAGE, errMsg);
                                                results.put(x.finished, Boolean.FALSE);
                                                results.put(x.errors, Boolean.TRUE);
                                                return results;
                                            }

                                            // if no sucess, fall through to return not finished
                                        }
                                    }
                                }
                            }
                        }

                        Map<String, Object> results = ServiceUtil.returnSuccess();
                        results.put(x.messages, authPaymentResult.get(x.customerRespMsgs));
                        results.put(x.finished, Boolean.FALSE);
                        results.put(x.errors, Boolean.FALSE);
                        return results;
                    }
                } catch (GeneralException e) {
                    String errMsg = x.Error_saving_and_processing_payment_authorization_results + e.toString();
                    Debug.logError(e, errMsg + x.authPaymentResult + authPaymentResult, MODULE);
                    Map<String, Object> results = ServiceUtil.returnSuccess();
                    results.put(ModelService.ERROR_MESSAGE, errMsg);
                    results.put(x.finished, Boolean.FALSE);
                    results.put(x.errors, Boolean.TRUE);
                    return results;
                }
            } else {
                // error with payment processor; will try later
                String errMsg = x.Invalid_Order_Payment_Preference_maxAmount_is_0;
                Debug.logInfo(errMsg, MODULE);
                Map<String, Object> results = ServiceUtil.returnSuccess();
                results.put(x.finished, Boolean.FALSE);
                results.put(x.errors, Boolean.TRUE);
                results.put(ModelService.ERROR_MESSAGE, errMsg);
                orderPaymentPreference.set(x.statusId, x.PAYMENT_CANCELLED);
                try {
                    orderPaymentPreference.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.ERROR_Problem_setting_OrderPaymentPreference_status_to_CANCELLED, MODULE);
                }
                return results;
            }
        } catch (GeneralException e) {
            Debug.logError(e, x.Error_processing_payment_authorization, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingPaymentCannotBeAuthorized,
                    UtilMisc.toMap(x.errroString, e.toString()), locale));
        }
    }

    /**
     * Processes payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return APPROVED|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> authOrderPayments(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get(x.orderId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = new HashMap<>();
        boolean reAuth = false;
        if (context.get(x.reAuth) != null) {
            reAuth = (Boolean) context.get(x.reAuth);
        }
        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List<GenericValue> paymentPrefs = null;

        try {
            // get the OrderHeader
            orderHeader = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class)
                    .findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);

            // get the payments to auth
            Map<String, String> lookupMap = UtilMisc.toMap(x.orderId, orderId, x.statusId, x.PAYMENT_NOT_AUTH);
            List<String> orderList = UtilMisc.toList(x.maxAmount);
            paymentPrefs = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findByAnd(delegator, x.OrderPaymentPreference, lookupMap, orderList, false);
            if (reAuth) {
                lookupMap.put(x.orderId, orderId);
                lookupMap.put(x.statusId, x.PAYMENT_AUTHORIZED);
                paymentPrefs.addAll(DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                        .findByAnd(delegator, x.OrderPaymentPreference, lookupMap, orderList, false));
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, x.Problems_getting_the_order_information, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, x.ERROR_Could_not_get_order_information + gee.toString() + x.str_d4191940);
            return result;
        }

        // make sure we have a OrderHeader
        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // get the order amounts
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        BigDecimal totalRemaining = orh.getOrderGrandTotal();

        // loop through and auth each order payment preference
        int finished = 0;
        int hadError = 0;
        List<String> messages = new LinkedList<>();
        for (GenericValue paymentPref : paymentPrefs) {
            if (reAuth && x.PAYMENT_AUTHORIZED.equals(paymentPref.getString(x.statusId))) {
                String paymentConfig = null;
                // get the payment settings i.e. serviceName and config properties file name
                GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPref, AUTH_SERVICE_TYPE, false);
                if (paymentSettings != null) {
                    paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
                    if (UtilValidate.isEmpty(paymentConfig)) {
                        paymentConfig = x.payment_properties;
                    }
                }
                // check the validity of the authorization; re-auth if necessary
                if (PaymentGatewayServices.checkAuthValidity(paymentPref, paymentConfig)) {
                    finished += 1;
                    continue;
                }
            }
            Map<String, Object> authContext = new HashMap<>();
            authContext.put(x.orderPaymentPreferenceId, paymentPref.getString(x.orderPaymentPreferenceId));
            authContext.put(x.userLogin, context.get(x.userLogin));

            Map<String, Object> results = null;
            try {
                results = dispatcher.runSync(x.authOrderPaymentPreference, authContext);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
            } catch (GenericServiceException se) {
                Debug.logError(se, x.Error_in_calling_authOrderPaymentPreference_from_authOrderPayments, MODULE);
                hadError += 1;
                messages.add(x.Could_not_authorize_OrderPaymentPreference + paymentPref.getString(x.orderPaymentPreferenceId)
                        + x.for_order + orderId + x.str_89222ecc + se.toString());
                continue;
            }

            // add authorization code to the result
            result.put(x.authCode, results.get(x.authCode));

            if (ServiceUtil.isError(results)) {
                hadError += 1;
                messages.add(x.Could_not_authorize_OrderPaymentPreference + paymentPref.getString(x.orderPaymentPreferenceId)
                        + x.for_order + orderId + x.str_89222ecc + results.get(ModelService.ERROR_MESSAGE));
                continue;
            }
            if ((Boolean) results.get(x.finished)) {
                finished += 1;
            }
            if ((Boolean) results.get(x.errors)) {
                hadError += 1;
            }
            if (results.get(x.messages) != null) {
                List<String> message = UtilGenerics.cast(results.get(x.messages));
                messages.addAll(message);
            }
            if (results.get(x.processAmount) != null) {
                totalRemaining = totalRemaining.subtract(((BigDecimal) results.get(x.processAmount)));
            }
        }

        Debug.logInfo(x.Finished_with_auth_s_checking_results, MODULE);

        // add messages to the result
        result.put(x.authResultMsgs, messages);

        if (hadError > 0) {
            Debug.logError(x.Error_s + hadError + x.during_auth_returning_ERROR, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(x.processResult, x.ERROR);
            return result;
        } else if (finished == paymentPrefs.size()) {
            Debug.logInfo(x.All_auth_s_passed_total_remaining + totalRemaining, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(x.processResult, x.APPROVED);
            return result;
        } else {
            Debug.logInfo(x.Only + finished + x.str_42099b4a + paymentPrefs.size() + x.OrderPaymentPreference_authorizations_passed
                    + x.returning_processResult_FAILED_with_no_message_so_that_message_from_ProductStore_will_be_used, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(x.processResult, x.FAILED);
            return result;
        }
    }


    private static Map<String, Object> authPayment(LocalDispatcher dispatcher, GenericValue userLogin, OrderReadHelper orh, GenericValue
            paymentPreference, BigDecimal totalRemaining, boolean reauth, BigDecimal overrideAmount) throws GeneralException {
        String paymentConfig = null;
        String serviceName = null;
        String paymentGatewayConfigId = null;

        // get the payment settings i.e. serviceName and config properties file name
        String serviceType = AUTH_SERVICE_TYPE;
        if (reauth) {
            serviceType = REAUTH_SERVICE_TYPE;
        }

        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPreference, serviceType, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString(x.paymentService);
            }
            paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);
        } else {
            throw new GeneralException(x.Could_not_find_any_valid_payment_settings_for_order_with_ID + orh.getOrderId()
                    + x.and_payment_operation_serviceType + serviceType + x.str_4ff447b8);
        }

        // make sure the service name is not null
        if (serviceName == null) {
            throw new GeneralException(x.Invalid_payment_processor_serviceName_is_null + paymentSettings);
        }

        // make the process context
        Map<String, Object> processContext = new HashMap<>();

        // get the visit record to obtain the client's IP address
        GenericValue orderHeader = orh.getOrderHeader();
        String visitId = orderHeader.getString(x.visitId);
        GenericValue visit = null;
        if (visitId != null) {
            try {
                visit = orderHeader.getDelegator().findOne(x.Visit, UtilMisc.toMap(x.visitId, visitId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        if (visit != null && visit.get(x.clientIpAddress) != null) {
            processContext.put(x.customerIpAddress, visit.getString(x.clientIpAddress));
        }

        GenericValue productStore = orderHeader.getRelatedOne(x.ProductStore, false);

        processContext.put(x.userLogin, userLogin);
        processContext.put(x.orderId, orh.getOrderId());
        processContext.put(x.orderItems, orh.getOrderItems());
        processContext.put(x.shippingAddress, EntityUtil.getFirst(orh.getShippingLocations()));
        // TODO refactor the payment API to handle support all addresses
        processContext.put(x.paymentConfig, paymentConfig);
        processContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
        processContext.put(x.currency, orh.getCurrency());
        processContext.put(x.orderPaymentPreference, paymentPreference);
        if (paymentPreference.get(x.securityCode) != null) {
            processContext.put(x.cardSecurityCode, paymentPreference.get(x.securityCode));
        }

        // get the billing information
        getBillingInformation(orh, paymentPreference, processContext);

        // default charge is totalRemaining
        BigDecimal processAmount = totalRemaining;

        // use override or max amount available
        if (overrideAmount != null) {
            processAmount = overrideAmount;
        } else if (paymentPreference.get(x.maxAmount) != null) {
            processAmount = paymentPreference.getBigDecimal(x.maxAmount);
        }

        // Check if the order is a replacement order
        boolean replacementOrderFlag = isReplacementOrder(orderHeader);

        // don't authorized more then what is required
        if (!replacementOrderFlag && processAmount.compareTo(totalRemaining) > 0) {
            processAmount = totalRemaining;
        }

        // format the decimal
        processAmount = processAmount.setScale(DECIMALS, ROUNDING);

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Charging_amount + processAmount, MODULE);
        }
        processContext.put(x.processAmount, processAmount);

        // invoke the processor
        Map<String, Object> processorResult = null;
        try {
            // invoke the payment processor; allow 5 minute transaction timeout and require a new tx; we'll capture the error and pass back nicely

            GenericValue creditCard = (GenericValue) processContext.get(x.creditCard);

            // only try other exp dates if orderHeader.autoOrderShoppingListId is not empty, productStore.autoOrderCcTryExp=Y
            // and this payment is a creditCard
            boolean tryOtherExpDates = x.Y.equals(productStore.getString(x.autoOrderCcTryExp)) && creditCard != null
                    && UtilValidate.isNotEmpty(orderHeader.getString(x.autoOrderShoppingListId));

            // if we are not trying other expire dates OR if we are and the date is after today, then run the service
            if (!tryOtherExpDates || UtilValidate.isDateAfterToday(creditCard.getString(x.expireDate))) {
                processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                if (ServiceUtil.isError(processorResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(processorResult));
                }
            }

            // try other expire dates if the expireDate is not after today, or if we called the auth service and resultBadExpire = true
            if (tryOtherExpDates && (!UtilValidate.isDateAfterToday(creditCard.getString(x.expireDate)) || (processorResult != null
                    && Boolean.TRUE.equals(processorResult.get(x.resultBadExpire))))) {
                // try adding 2, 3, 4 years later with the same month
                String expireDate = creditCard.getString(x.expireDate);
                int dateSlash1 = expireDate.indexOf(x.str_42099b4a);
                String month = expireDate.substring(0, dateSlash1);
                String year = expireDate.substring(dateSlash1 + 1);

                // start adding 2 years, if comes back with resultBadExpire try again up to twice incrementing one year
                year = StringUtil.addToNumberString(year, 2);
                // note that this is set in memory only for now, not saved to the database unless successful
                creditCard.set(x.expireDate, month + x.str_42099b4a + year);
                // don't need to set back in the processContext, it's already there: processContext.put("creditCard", creditCard);
                processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                if (ServiceUtil.isError(processorResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(processorResult));
                }

                // note that these additional tries will only be done if the service return is not an error, in that case we let it
                // pass through to the normal error handling
                if (ServiceUtil.isSuccess(processorResult) && Boolean.TRUE.equals(processorResult.get(x.resultBadExpire))) {
                    // okay, try one more year...
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set(x.expireDate, month + x.str_42099b4a + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                    if (ServiceUtil.isError(processorResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(processorResult));
                    }
                }

                if (ServiceUtil.isSuccess(processorResult) && Boolean.TRUE.equals(processorResult.get(x.resultBadExpire))) {
                    // okay, try one more year... and this is the last try
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set(x.expireDate, month + x.str_42099b4a + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                    if (ServiceUtil.isError(processorResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(processorResult));
                    }
                }

                // at this point if we have a successful result, let's save the new creditCard expireDate
                if (ServiceUtil.isSuccess(processorResult) && Boolean.TRUE.equals(processorResult.get(x.authResult))) {
                    // TODO: this is bad; we should be expiring the old card and creating a new one instead of editing it
                    creditCard.store();
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_occurred_on + serviceName + x.Order_ID_is + orh.getOrderId() + x.str_4ff447b8, MODULE);
            throw new GeneralException(x.Problems_invoking_payment_processor_Will_retry_later_Order_ID_is + orh.getOrderId() + x.str_4ff447b8, e);
        }

        if (processorResult != null) {
            // check for errors from the processor implementation
            if (ServiceUtil.isError(processorResult)) {
                Debug.logError(x.Processor_failed_will_retry_later + processorResult.get(ModelService.ERROR_MESSAGE), MODULE);
                // log the error message as a gateway response when it fails
                saveError(dispatcher, userLogin, paymentPreference, processorResult, AUTH_SERVICE_TYPE, x.PGT_AUTHORIZE);
                // this is the one place where we want to return null because the calling method will look for this
                return null;
            }

            // pass the payTo partyId to the result processor; we just add it to the result context.
            String payToPartyId = getPayToPartyId(orh.getOrderHeader());
            processorResult.put(x.payToPartyId, payToPartyId);

            // add paymentSettings to result; for use by later processors
            processorResult.put(x.paymentSettings, paymentSettings);

            // and pass on the currencyUomId
            processorResult.put(x.currencyUomId, orh.getCurrency());
        }

        return processorResult;
    }

    private static GenericValue getPaymentSettings(GenericValue orderHeader, GenericValue paymentPreference, String paymentServiceType,
                                                   boolean anyServiceType) {
        Delegator delegator = orderHeader.getDelegator();
        GenericValue paymentSettings = null;
        String paymentMethodTypeId = paymentPreference.getString(x.paymentMethodTypeId);

        if (paymentMethodTypeId != null) {
            String productStoreId = orderHeader.getString(x.productStoreId);
            if (productStoreId != null) {
                paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId,
                        paymentServiceType, anyServiceType);
            }
        }
        return paymentSettings;
    }

    private static String getPayToPartyId(GenericValue orderHeader) {
        String payToPartyId = x.Company; // default value
        GenericValue productStore = null;
        try {
            productStore = orderHeader.getRelatedOne(x.ProductStore, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_ProductStore_from_OrderHeader, MODULE);
            return null;
        }
        if (productStore != null && productStore.get(x.payToPartyId) != null) {
            payToPartyId = productStore.getString(x.payToPartyId);
        } else {
            Debug.logWarning(x.Using_default_value_of_Company_for_payToPartyId_on_order + orderHeader.getString(x.orderId) + x.str_4ff447b8, MODULE);
        }
        return payToPartyId;
    }

    private static String getBillingInformation(OrderReadHelper orh, GenericValue paymentPreference, Map<String, Object> toContext)
            throws GenericEntityException {
        // gather the payment related objects.
        String paymentMethodTypeId = paymentPreference.getString(x.paymentMethodTypeId);
        GenericValue paymentMethod = paymentPreference.getRelatedOne(x.PaymentMethod, false);
        if (paymentMethod != null && x.CREDIT_CARD.equals(paymentMethodTypeId)) {
            // type credit card
            GenericValue creditCard = paymentMethod.getRelatedOne(x.CreditCard, false);
            GenericValue billingAddress = creditCard.getRelatedOne(x.PostalAddress, false);
            toContext.put(x.creditCard, creditCard);
            toContext.put(x.billingAddress, billingAddress);
        } else if (paymentMethod != null && x.EFT_ACCOUNT.equals(paymentMethodTypeId)) {
            // type eft
            GenericValue eftAccount = paymentMethod.getRelatedOne(x.EftAccount, false);
            GenericValue billingAddress = eftAccount.getRelatedOne(x.PostalAddress, false);
            toContext.put(x.eftAccount, eftAccount);
            toContext.put(x.billingAddress, billingAddress);
        } else if (paymentMethod != null && x.GIFT_CARD.equals(paymentMethodTypeId)) {
            // type gift card
            GenericValue giftCard = paymentMethod.getRelatedOne(x.GiftCard, false);
            toContext.put(x.giftCard, giftCard);
            GenericValue orderHeader = paymentPreference.getRelatedOne(x.OrderHeader, false);
            List<GenericValue> orderItems = orderHeader.getRelated(x.OrderItem, null, null, false);
            toContext.put(x.orderId, orderHeader.getString(x.orderId));
            toContext.put(x.orderItems, orderItems);
        } else if (x.FIN_ACCOUNT.equals(paymentMethodTypeId)) {
            toContext.put(x.finAccountId, paymentPreference.getString(x.finAccountId));
        } else if (x.EXT_PAYPAL.equals(paymentMethodTypeId)) {
            GenericValue payPalPaymentMethod = paymentMethod.getRelatedOne(x.PayPalPaymentMethod, false);
            toContext.put(x.payPalPaymentMethod, payPalPaymentMethod);
        } else {
            // add other payment types here; i.e. gift cards, etc.
            // unknown payment type; ignoring.
            Debug.logError(x.ERROR_Unsupported_PaymentMethodType_passed_for_authorization, MODULE);
            return null;
        }

        // get some contact info.
        GenericValue billToPersonOrGroup = orh.getBillToParty();
        GenericValue billToEmail = null;

        Collection<GenericValue> emails = ContactHelper.getContactMech(billToPersonOrGroup.getRelatedOne(x.Party, false),
                x.PRIMARY_EMAIL, x.EMAIL_ADDRESS, false);

        if (UtilValidate.isNotEmpty(emails)) {
            billToEmail = emails.iterator().next();
        }

        toContext.put(x.billToParty, billToPersonOrGroup);
        toContext.put(x.billToEmail, billToEmail);

        return billToPersonOrGroup.getString(x.partyId);
    }

    /**
     * Releases authorizations through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payments.
     */
    public static Map<String, Object> releaseOrderPayments(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderPaymentPreferenceId = (String) context.get(x.orderPaymentPreferenceId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String orderId = x.emptyString;
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            if (orderPaymentPreferenceId != null) {
                paymentPref = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                        .findOne(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceId), false);
                orderId = paymentPref.getString(x.orderId);
            } else {
                orderId = (String) context.get(x.orderId);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }

        // get the payment preferences
        List<GenericValue> paymentPrefs = null;
        try {
            // get the valid payment prefs
            List<EntityExpr> othExpr = UtilMisc.toList(EntityCondition.makeCondition(x.paymentMethodTypeId, EntityOperator.EQUALS, x.EFT_ACCOUNT));
            othExpr.add(EntityCondition.makeCondition(x.paymentMethodTypeId, EntityOperator.EQUALS, x.GIFT_CARD));
            othExpr.add(EntityCondition.makeCondition(x.paymentMethodTypeId, EntityOperator.EQUALS, x.FIN_ACCOUNT));
            EntityCondition con1 = EntityCondition.makeCondition(othExpr, EntityJoinOperator.OR);
            EntityCondition statExpr = EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.PAYMENT_SETTLED);
            EntityCondition con2 = EntityCondition.makeCondition(UtilMisc.toList(con1, statExpr), EntityOperator.AND);
            EntityCondition authExpr = EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.PAYMENT_AUTHORIZED);
            EntityCondition con3 = EntityCondition.makeCondition(UtilMisc.toList(con2, authExpr), EntityOperator.OR);
            EntityExpr orderExpr = EntityCondition.makeCondition(x.orderId, EntityOperator.EQUALS, orderId);
            EntityCondition con4 = EntityCondition.makeCondition(UtilMisc.toList(con3, orderExpr), EntityOperator.AND);
            paymentPrefs = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findByCondition(delegator, x.OrderPaymentPreference, con4, null, null, null, false);
        } catch (GenericEntityException gee) {
            Debug.logError(gee, x.Problems_getting_entity_record_s_see_stack_trace, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, x.ERROR_Could_not_get_order_information + gee.toString() + x.str_d4191940);
            return result;
        }

        // return complete if no payment prefs were found
        if (paymentPrefs.isEmpty()) {
            Debug.logWarning(x.No_OrderPaymentPreference_records_available_for_release, MODULE);
            result.put(x.processResult, x.COMPLETE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result;
        }

        // iterate over the prefs and release each one
        List<GenericValue> finished = new LinkedList<>();
        for (GenericValue pPref : paymentPrefs) {
            Map<String, Object> releaseContext = UtilMisc.toMap(x.userLogin, userLogin, x.orderPaymentPreferenceId,
                    pPref.getString(x.orderPaymentPreferenceId));
            Map<String, Object> releaseResult = null;
            try {
                releaseResult = dispatcher.runSync(x.releaseOrderPaymentPreference, releaseContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_releaseOrderPaymentPreference_service_for_orderPaymentPreferenceId
                        + paymentPref.getString(x.orderPaymentPreferenceId), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale) + x.str_b858cb28
                        + paymentPref.getString(x.orderPaymentPreferenceId));
            }
            if (ServiceUtil.isError(releaseResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(releaseResult), MODULE);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
            } else if (!ServiceUtil.isFailure(releaseResult)) {
                finished.add(paymentPref);
            }
        }
        result = ServiceUtil.returnSuccess();
        if (finished.size() == paymentPrefs.size()) {
            result.put(x.processResult, x.COMPLETE);
        } else {
            result.put(x.processResult, x.FAILED);
        }

        return result;
    }

    public static Map<String, Object> processCreditResult(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String currencyUomId = (String) context.get(x.currencyUomId);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        Boolean creditResponse = (Boolean) context.get(x.creditResult);
        Locale locale = (Locale) context.get(x.locale);
        // create the PaymentGatewayResponse
        String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
        GenericValue pgCredit = delegator.makeValue(x.PaymentGatewayResponse);
        pgCredit.set(x.paymentGatewayResponseId, responseId);
        pgCredit.set(x.paymentServiceTypeEnumId, CREDIT_SERVICE_TYPE);
        pgCredit.set(x.orderPaymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        pgCredit.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        pgCredit.set(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
        pgCredit.set(x.transCodeEnumId, x.PGT_CREDIT);
        // set the credit info
        pgCredit.set(x.amount, context.get(x.creditAmount));
        pgCredit.set(x.referenceNum, context.get(x.creditRefNum));
        pgCredit.set(x.altReference, context.get(x.creditAltRefNum));
        pgCredit.set(x.gatewayCode, context.get(x.creditCode));
        pgCredit.set(x.gatewayFlag, context.get(x.creditFlag));
        pgCredit.set(x.gatewayMessage, context.get(x.creditMessage));
        pgCredit.set(x.transactionDate, UtilDateTime.nowTimestamp());
        pgCredit.set(x.currencyUomId, currencyUomId);
        // create the internal messages
        List<GenericValue> messageEntities = new LinkedList<>();
        List<String> messages = UtilGenerics.cast(context.get(x.internalRespMsgs));
        if (UtilValidate.isNotEmpty(messages)) {
            for (String message : messages) {
                GenericValue respMsg = delegator.makeValue(x.PaymentGatewayRespMsg);
                String respMsgId = delegator.getNextSeqId(x.PaymentGatewayRespMsg);
                respMsg.set(x.paymentGatewayRespMsgId, respMsgId);
                respMsg.set(x.paymentGatewayResponseId, responseId);
                respMsg.set(x.pgrMessage, message);
                // store the messages
                messageEntities.add(respMsg);
            }
        }
        // save the response and respective messages
        savePgrAndMsgs(dctx, pgCredit, messageEntities);

        if (creditResponse != null && creditResponse) {
            paymentPref.set(x.statusId, x.PAYMENT_CANCELLED);
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Problem_storing_updated_payment_preference_authorization_was_credit, MODULE);
            }
            // cancel any payment records
            List<GenericValue> paymentList = null;
            try {
                paymentList = paymentPref.getRelated(x.Payment, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Unable_to_get_Payment_records_from_OrderPaymentPreference + paymentPref, MODULE);
            }
            if (paymentList != null) {
                Iterator<GenericValue> pi = paymentList.iterator();
                while (pi.hasNext()) {
                    GenericValue pay = pi.next();
                    try {
                        Map<String, Object> cancelResults = dispatcher.runSync(x.setPaymentStatus,
                                UtilMisc.toMap(x.userLogin, userLogin, x.paymentId, pay.get(x.paymentId), x.statusId, x.PMNT_CANCELLED));
                        if (ServiceUtil.isError(cancelResults)) {
                            throw new GenericServiceException(ServiceUtil.getErrorMessage(cancelResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Unable_to_cancel_Payment + pay, MODULE);
                    }
                }
            }
        } else {
            Debug.logError(x.Credit_failed_for_pref + paymentPref, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    x.AccountingTroubleCallingCreditOrderPaymentPreferenceService,
                    UtilMisc.toMap(x.paymentPref, paymentPref), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Releases authorization for a single OrderPaymentPreference through service calls to the defined processing service for the
     * ProductStore/PaymentMethodType
     * @return SUCCESS|FAILED|ERROR for complete processing of payment.
     */
    public static Map<String, Object> releaseOrderPaymentPreference(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderPaymentPreferenceId = (String) context.get(x.orderPaymentPreferenceId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            paymentPref = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findOne(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.Problem_getting_OrderPaymentPreference_for_orderPaymentPreferenceId
                    + orderPaymentPreferenceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        // Error if no OrderPaymentPreference was found
        if (paymentPref == null) {
            Debug.logWarning(x.Could_not_find_OrderPaymentPreference_with_orderPaymentPreferenceId
                    + orderPaymentPreferenceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        // Get the OrderHeader
        GenericValue orderHeader = null;
        String orderId = paymentPref.getString(x.orderId);
        try {
            orderHeader = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class)
                    .findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.Problem_getting_OrderHeader_for_orderId + orderId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }
        // Error if no OrderHeader was found
        if (orderHeader == null) {
            Debug.logWarning(x.Could_not_find_OrderHeader_with_orderId
                    + orderId + x.not_processing_payments, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String currency = orh.getCurrency();
        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;
        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orderHeader, paymentPref, RELEASE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString(x.paymentService);
            }
            paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);
            if (serviceName == null) {
                Debug.logWarning(x.No_payment_release_service_for + paymentPref.getString(x.paymentMethodTypeId), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                        x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale) + x.str_b858cb28
                        + paymentPref.getString(x.paymentMethodTypeId));
            }
        } else {
            Debug.logWarning(x.No_payment_release_settings_found_for + paymentPref.getString(x.paymentMethodTypeId), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale) + x.str_b858cb28
                    + paymentPref.getString(x.paymentMethodTypeId));
        }
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
        Map<String, Object> releaseContext = new HashMap<>();
        releaseContext.put(x.orderPaymentPreference, paymentPref);
        releaseContext.put(x.releaseAmount, authTransaction.getBigDecimal(x.amount));
        releaseContext.put(x.currency, currency);
        releaseContext.put(x.paymentConfig, paymentConfig);
        releaseContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
        releaseContext.put(x.userLogin, userLogin);
        // run the defined service
        Map<String, Object> releaseResult = null;
        try {
            releaseResult = dispatcher.runSync(serviceName, releaseContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_releasing_payment, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale));
        }
        // get the release result code
        if (releaseResult != null && ServiceUtil.isSuccess(releaseResult)) {
            Map<String, Object> releaseResRes;
            try {
                ModelService model = dctx.getModelService(x.processReleaseResult);
                releaseResult.put(x.orderPaymentPreference, paymentPref);
                releaseResult.put(x.userLogin, userLogin);
                Map<String, Object> resCtx = model.makeValid(releaseResult, ModelService.IN_PARAM);
                releaseResRes = dispatcher.runSync(model.getName(), resCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Trouble_processing_the_release_results, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                        x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale) + x.str_b858cb28
                        + e.getMessage());
            }
            if (releaseResRes != null && ServiceUtil.isError(releaseResRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResRes));
            }
        } else if (ServiceUtil.isError(releaseResult)) {
            saveError(dispatcher, userLogin, paymentPref, releaseResult, RELEASE_SERVICE_TYPE, x.PGT_RELEASE);
            result = ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
        }
        return result;
    }

    public static Map<String, Object> processReleaseResult(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String currencyUomId = (String) context.get(x.currencyUomId);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        Boolean releaseResponse = (Boolean) context.get(x.releaseResult);
        Locale locale = (Locale) context.get(x.locale);
        // create the PaymentGatewayResponse
        String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
        GenericValue pgResponse = delegator.makeValue(x.PaymentGatewayResponse);
        pgResponse.set(x.paymentGatewayResponseId, responseId);
        pgResponse.set(x.paymentServiceTypeEnumId, RELEASE_SERVICE_TYPE);
        pgResponse.set(x.orderPaymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        pgResponse.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        pgResponse.set(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
        pgResponse.set(x.transCodeEnumId, x.PGT_RELEASE);
        // set the release info
        pgResponse.set(x.amount, context.get(x.releaseAmount));
        pgResponse.set(x.referenceNum, context.get(x.releaseRefNum));
        pgResponse.set(x.altReference, context.get(x.releaseAltRefNum));
        pgResponse.set(x.gatewayCode, context.get(x.releaseCode));
        pgResponse.set(x.gatewayFlag, context.get(x.releaseFlag));
        pgResponse.set(x.gatewayMessage, context.get(x.releaseMessage));
        pgResponse.set(x.transactionDate, UtilDateTime.nowTimestamp());
        pgResponse.set(x.currencyUomId, currencyUomId);
        // store the gateway response
        savePgr(dctx, pgResponse);
        // create the internal messages
        List<String> messages = UtilGenerics.cast(context.get(x.internalRespMsgs));
        if (UtilValidate.isNotEmpty(messages)) {
            Iterator<String> i = messages.iterator();
            while (i.hasNext()) {
                GenericValue respMsg = delegator.makeValue(x.PaymentGatewayRespMsg);
                String respMsgId = delegator.getNextSeqId(x.PaymentGatewayRespMsg);
                String message = i.next();
                respMsg.set(x.paymentGatewayRespMsgId, respMsgId);
                respMsg.set(x.paymentGatewayResponseId, responseId);
                respMsg.set(x.pgrMessage, message);
                // store the messages
                savePgr(dctx, respMsg);
            }
        }

        if (releaseResponse != null && releaseResponse) {
            paymentPref.set(x.statusId, x.PAYMENT_CANCELLED);
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Problem_storing_updated_payment_preference_authorization_was_released, MODULE);
            }
            // cancel any payment records
            List<GenericValue> paymentList = null;
            try {
                paymentList = paymentPref.getRelated(x.Payment, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Unable_to_get_Payment_records_from_OrderPaymentPreference + paymentPref, MODULE);
            }
            if (paymentList != null) {
                Iterator<GenericValue> pi = paymentList.iterator();
                while (pi.hasNext()) {
                    GenericValue pay = pi.next();
                    try {
                        Map<String, Object> cancelResults = dispatcher.runSync(x.setPaymentStatus, UtilMisc.toMap(x.userLogin,
                                userLogin, x.paymentId, pay.get(x.paymentId), x.statusId, x.PMNT_CANCELLED));
                        if (ServiceUtil.isError(cancelResults)) {
                            throw new GenericServiceException(ServiceUtil.getErrorMessage(cancelResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Unable_to_cancel_Payment + pay, MODULE);
                    }
                }
            }
        } else {
            Debug.logError(x.Release_failed_for_pref + paymentPref, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ORDER,
                    x.AccountingTroubleCallingReleaseOrderPaymentPreferenceService, locale) + x.str_b858cb28
                    + paymentPref);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> capturePaymentsByInvoice(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String invoiceId = (String) context.get(x.invoiceId);
        Locale locale = (Locale) context.get(x.locale);

        // lookup the invoice
        GenericValue invoice = null;
        try {
            invoice = DaoRegistry.getDao(delegator, x.Invoice, UserLoginDao.class)
                    .findOne(delegator, x.Invoice, UtilMisc.toMap(x.invoiceId, invoiceId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Trouble_looking_up_Invoice + invoiceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingInvoiceNotFound, UtilMisc.toMap(x.invoiceId, invoiceId), locale));
        }

        if (invoice == null) {
            Debug.logError(x.Could_not_locate_invoice + invoiceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingInvoiceNotFound, UtilMisc.toMap(x.invoiceId, invoiceId), locale));
        }

        // get the OrderItemBilling records for this invoice
        List<GenericValue> orderItemBillings = null;
        try {
            orderItemBillings = invoice.getRelated(x.OrderItemBilling, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(x.Trouble_getting_OrderItemBilling_s_from_Invoice + invoiceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemLookingUpOrderItemBilling,
                    UtilMisc.toMap(x.billFields, invoiceId), locale));
        }

        // check for an associated billing account
        String billingAccountId = invoice.getString(x.billingAccountId);

        // make sure they are all for the same order
        String testOrderId = null;
        boolean allSameOrder = true;
        if (orderItemBillings != null) {
            Iterator<GenericValue> oii = orderItemBillings.iterator();
            while (oii.hasNext()) {
                GenericValue oib = oii.next();
                String orderId = oib.getString(x.orderId);
                if (testOrderId == null) {
                    testOrderId = orderId;
                } else {
                    if (!orderId.equals(testOrderId)) {
                        allSameOrder = false;
                        break;
                    }
                }
            }
        }

        if (testOrderId == null || !allSameOrder) {
            Debug.logWarning(x.Attempt_to_settle_Invoice + invoiceId + x.which_contained_none_multiple_orders, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    x.AccountingInvoiceCannotBeSettle,
                    UtilMisc.toMap(x.invoiceId, invoiceId), locale));
        }

        // get the invoice amount (amount to bill)
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceNotApplied(invoice);
        if (Debug.infoOn()) {
            Debug.logInfo(x.Capture_Invoice + invoiceId + x.total_94a9cff9 + invoiceTotal, MODULE);
        }

        // now capture the order
        Map<String, Object> serviceContext = UtilMisc.toMap(x.userLogin, userLogin, x.orderId, testOrderId, x.invoiceId,
                invoiceId, x.captureAmount, invoiceTotal);
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            serviceContext.put(x.billingAccountId, billingAccountId);
        }
        try {
            Map<String, Object> result = dispatcher.runSync(x.captureOrderPayments, serviceContext);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            return result;
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Trouble_running_captureOrderPayments_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentCannotBeCaptured, locale));
        }
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> captureOrderPayments(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderId = (String) context.get(x.orderId);
        String invoiceId = (String) context.get(x.invoiceId);
        String billingAccountId = (String) context.get(x.billingAccountId);
        BigDecimal amountToCapture = (BigDecimal) context.get(x.captureAmount);
        Locale locale = (Locale) context.get(x.locale);
        amountToCapture = amountToCapture.setScale(DECIMALS, ROUNDING);

        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List<GenericValue> paymentPrefs = null;
        List<GenericValue> paymentPrefsBa = null;

        try {
            orderHeader = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class)
                    .findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);

            // get the payment prefs
            paymentPrefs = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findByAnd(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderId, orderId, x.statusId, x.PAYMENT_AUTHORIZED),
                            UtilMisc.toList(x.maxAmount_7b034ae5), false);

            if (UtilValidate.isNotEmpty(billingAccountId)) {
                paymentPrefsBa = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                        .findByAnd(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderId, orderId, x.paymentMethodTypeId, x.EXT_BILLACT,
                                x.statusId, x.PAYMENT_NOT_RECEIVED), UtilMisc.toList(x.maxAmount_7b034ae5), false);
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, x.Problems_getting_entity_record_s_see_stack_trace, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale) + x.str_b858cb28 + gee.toString());
        }

        // error if no order was found
        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // Check if the outstanding amount for the order is greater than the
        // amount that we are going to capture.
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        BigDecimal orderGrandTotal = orh.getOrderGrandTotal();
        orderGrandTotal = orderGrandTotal.setScale(DECIMALS, ROUNDING);
        BigDecimal totalPayments = PaymentWorker.getPaymentsTotal(orh.getOrderPayments());
        totalPayments = totalPayments.setScale(DECIMALS, ROUNDING);
        BigDecimal remainingTotal = orderGrandTotal.subtract(totalPayments);
        if (Debug.infoOn()) {
            Debug.logInfo(x.The_Remaining_Total_for_order + orderId + x._is + remainingTotal, MODULE);
        }
        // The amount to capture cannot be greater than the remaining total
        amountToCapture = amountToCapture.min(remainingTotal);
        if (Debug.infoOn()) {
            Debug.logInfo(x.Actual_Expected_Capture_Amount + amountToCapture, MODULE);
        }
        // Process billing accounts payments
        if (UtilValidate.isNotEmpty(paymentPrefsBa)) {
            Iterator<GenericValue> paymentsBa = paymentPrefsBa.iterator();
            while (paymentsBa.hasNext()) {
                GenericValue paymentPref = paymentsBa.next();

                BigDecimal authAmount = paymentPref.getBigDecimal(x.maxAmount);
                if (authAmount == null) {
                    authAmount = ZERO;
                }
                authAmount = authAmount.setScale(DECIMALS, ROUNDING);

                if (authAmount.compareTo(ZERO) == 0) {
                    // nothing to capture
                    Debug.logInfo(x.Nothing_to_capture_authAmount_0, MODULE);
                    continue;
                }
                // the amount for *this* capture
                BigDecimal amountThisCapture = amountToCapture.min(authAmount);

                // decrease amount of next payment preference to capture
                amountToCapture = amountToCapture.subtract(amountThisCapture);

                // If we have an invoice, we find unapplied payments associated
                // to the billing account and we apply them to the invoice
                if (UtilValidate.isNotEmpty(invoiceId)) {
                    Map<String, Object> captureResult = null;
                    try {
                        captureResult = dispatcher.runSync(x.captureBillingAccountPayments, UtilMisc.<String, Object>toMap(x.invoiceId, invoiceId,
                                                                                                          x.billingAccountId, billingAccountId,
                                                                                                          x.captureAmount, amountThisCapture,
                                                                                                          x.orderId, orderId,
                                                                                                          x.userLogin, userLogin));
                        if (ServiceUtil.isError(captureResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(captureResult));
                        }
                    } catch (GenericServiceException ex) {
                        return ServiceUtil.returnError(ex.getMessage());
                    }
                    if (captureResult != null) {

                        BigDecimal amountCaptured = BigDecimal.ZERO;
                        try {
                            amountCaptured = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(captureResult.get(x.captureAmount),
                                    x.BigDecimal, null, locale);
                        } catch (GeneralException e) {
                            Debug.logError(e, x.Trouble_processing_the_result_captureResult + captureResult, MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                                    x.AccountingPaymentCannotBeCaptured, locale) + x.str_b858cb28 + captureResult);
                        }
                        if (Debug.infoOn()) {
                            Debug.logInfo(x.Amount_captured_for_order + orderId + x.from_unapplied_payments_associated_to_billing_account
                                    + billingAccountId + x._is_765c3542 + amountCaptured, MODULE);
                        }

                        amountCaptured = amountCaptured.setScale(DECIMALS, ROUNDING);

                        if (amountCaptured.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                        // add the invoiceId to the result for processing
                        captureResult.put(x.invoiceId, invoiceId);
                        captureResult.put(x.captureResult, Boolean.TRUE);
                        captureResult.put(x.orderPaymentPreference, paymentPref);
                        if (context.get(x.captureRefNum) == null) {
                            captureResult.put(x.captureRefNum, x.emptyString);
                            // FIXME: this is an hack to avoid a service validation error for processCaptureResult (captureRefNum is mandatory,
                            //  but it is not used for billing accounts)
                        }

                        // process the capture's results
                        try {
                            // the following method will set on the OrderPaymentPreference:
                            // maxAmount = amountCaptured and
                            // statusId = PAYMENT_RECEIVED
                            processResult(dctx, captureResult, userLogin, paymentPref, locale);
                        } catch (GeneralException e) {
                            Debug.logError(e, x.Trouble_processing_the_result_captureResult + captureResult, MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                                    x.AccountingPaymentCannotBeCaptured, locale) + x.str_b858cb28 + captureResult);
                        }

                        // create any splits which are needed
                        if (authAmount.compareTo(amountCaptured) > 0) {
                            BigDecimal splitAmount = authAmount.subtract(amountCaptured);
                            try {
                                Map<String, Object> splitCtx = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.orderPaymentPreference,
                                        paymentPref, x.splitAmount, splitAmount);
                                dispatcher.addCommitService(x.processCaptureSplitPayment, splitCtx, true);
                            } catch (GenericServiceException e) {
                                Debug.logWarning(e, x.Problem_processing_the_capture_split_payment, MODULE);
                            }
                            if (Debug.infoOn()) {
                                Debug.logInfo(x.Captured + amountThisCapture + x.Remaining_re_auth + splitAmount, MODULE);
                            }
                        }
                    } else {
                        Debug.logError(x.Payment_not_captured_for_order + orderId + x.from_billing_account + billingAccountId + x.str_4ff447b8, MODULE);
                    }
                }
            }
        }

        // iterate over the prefs and capture each one until we meet our total
        if (UtilValidate.isNotEmpty(paymentPrefs)) {
            Iterator<GenericValue> payments = paymentPrefs.iterator();
            while (payments.hasNext()) {
                // DEJ20060708: Do we really want to just log and ignore the errors like this? I've improved a few of these in a review today,
                // but it is being done all over...
                GenericValue paymentPref = payments.next();
                GenericValue authTrans = getAuthTransaction(paymentPref);
                if (authTrans == null) {
                    Debug.logWarning(x.Authorized_OrderPaymentPreference_has_no_corresponding_PaymentGatewayResponse_cannot_capture_payment
                            + paymentPref, MODULE);
                    continue;
                }

                // check for an existing capture
                GenericValue captureTrans = getCaptureTransaction(paymentPref);
                if (captureTrans != null) {
                    Debug.logWarning(x.Attempt_to_capture_and_already_captured_preference + captureTrans, MODULE);
                    continue;
                }

                BigDecimal authAmount = authTrans.getBigDecimal(x.amount);
                if (authAmount == null) {
                    authAmount = ZERO;
                }
                authAmount = authAmount.setScale(DECIMALS, ROUNDING);

                if (authAmount.compareTo(ZERO) == 0) {
                    // nothing to capture
                    Debug.logInfo(x.Nothing_to_capture_authAmount_0, MODULE);
                    continue;
                }

                // the amount for *this* capture
                BigDecimal amountThisCapture;

                // determine how much for *this* capture
                if (isReplacementOrder(orderHeader)) {
                    // if it is a replacement order then just capture the auth amount
                    amountThisCapture = authAmount;
                } else if (authAmount.compareTo(amountToCapture) >= 0) {
                    // if the auth amount is more then expected capture just capture what is expected
                    amountThisCapture = amountToCapture;
                } else if (payments.hasNext()) {
                    // if we have more payments to capture; just capture what was authorized
                    amountThisCapture = authAmount;
                } else {
                    // we need to capture more then what was authorized; re-auth for the new amount
                    // TODO: add what the billing account cannot support to the re-auth amount
                    // TODO: add support for re-auth for additional funds
                    // just in case; we will capture the authorized amount here; until this is implemented
                    Debug.logError(x.The_amount_to_capture_was_more_then_what_was_authorized_we_only_captured_the_authorized_amount
                            + paymentPref, MODULE);
                    amountThisCapture = authAmount;
                }

                Map<String, Object> captureResult = capturePayment(dctx, userLogin, orh, paymentPref, amountThisCapture, locale);
                if (captureResult != null && ServiceUtil.isSuccess(captureResult)) {
                    // credit card processors return captureAmount, but gift certificate processors return processAmount
                    BigDecimal amountCaptured = null;
                    try {
                        amountCaptured = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(captureResult.get(x.captureAmount), x.BigDecimal,
                                null, locale);

                        if (amountCaptured == null) {
                            amountCaptured = (BigDecimal) captureResult.get(x.processAmount);
                        }

                        amountCaptured = amountCaptured.setScale(DECIMALS, ROUNDING);

                        // decrease amount of next payment preference to capture
                        amountToCapture = amountToCapture.subtract(amountCaptured);

                        // add the invoiceId to the result for processing, not for a replacement order
                        if (!isReplacementOrder(orderHeader)) {
                            captureResult.put(x.invoiceId, invoiceId);
                        }

                        // process the capture's results
                        processResult(dctx, captureResult, userLogin, paymentPref, locale);
                    } catch (GeneralException e) {
                        Debug.logError(e, x.Trouble_processing_the_result_captureResult + captureResult, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                                x.AccountingPaymentCannotBeCaptured, locale) + x.str_b858cb28 + captureResult);
                    }

                    // create any splits which are needed
                    if (authAmount.compareTo(amountCaptured) > 0) {
                        BigDecimal splitAmount = authAmount.subtract(amountCaptured);
                        try {
                            Map<String, Object> splitCtx = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.orderPaymentPreference,
                                    paymentPref, x.splitAmount, splitAmount);
                            dispatcher.addCommitService(x.processCaptureSplitPayment, splitCtx, true);
                        } catch (GenericServiceException e) {
                            Debug.logWarning(e, x.Problem_processing_the_capture_split_payment, MODULE);
                        }
                        if (Debug.infoOn()) {
                            Debug.logInfo(x.Captured + amountThisCapture + x.Remaining_re_auth + splitAmount, MODULE);
                        }
                    }
                } else {
                    Debug.logError(x.Payment_not_captured, MODULE);
                }
            }
        }

        if (amountToCapture.compareTo(ZERO) > 0) {
            GenericValue productStore = orh.getProductStore();
            if (UtilValidate.isNotEmpty(productStore)) {
                boolean shipIfCaptureFails = UtilValidate.isEmpty(productStore.get(x.shipIfCaptureFails))
                        || x.Y.equalsIgnoreCase(productStore.getString(x.shipIfCaptureFails));
                if (!shipIfCaptureFails) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                            x.AccountingPaymentCannotBeCaptured, locale));
                } else {
                    Debug.logWarning(x.Payment_capture_failed_shipping_order_anyway_as_per_ProductStore_setting_shipIfCaptureFails, MODULE);
                }
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.processResult, x.FAILED);
            return result;
        } else {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.processResult, x.COMPLETE);
            return result;
        }
    }

    public static Map<String, Object> processCaptureSplitPayment(DispatchContext dctx, PaymentGatewayServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal splitAmount = (BigDecimal) context.get(x.splitAmount);

        String orderId = paymentPref.getString(x.orderId);
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);

        String statusId = x.PAYMENT_NOT_AUTH;
        if (x.EXT_BILLACT.equals(paymentPref.getString(x.paymentMethodTypeId))) {
            statusId = x.PAYMENT_NOT_RECEIVED;
        } else if (x.EXT_PAYPAL.equals(paymentPref.get(x.paymentMethodTypeId))) {
            statusId = x.PAYMENT_AUTHORIZED;
        }
        // create a new payment preference
        Debug.logInfo(x.Creating_payment_preference_split, MODULE);
        String newPrefId = delegator.getNextSeqId(x.OrderPaymentPreference);
        GenericValue newPref = delegator.makeValue(x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, newPrefId));
        newPref.set(x.orderId, paymentPref.get(x.orderId));
        newPref.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        newPref.set(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
        newPref.set(x.maxAmount, splitAmount);
        newPref.set(x.statusId, statusId);
        newPref.set(x.createdDate, UtilDateTime.nowTimestamp());
        if (userLogin != null) {
            newPref.set(x.createdByUserLogin, userLogin.getString(x.userLoginId));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.New_preference + newPref, MODULE);
        }

        Map<String, Object> processorResult = null;
        try {
            // create the new payment preference
            delegator.create(newPref);

            // PayPal requires us to reuse the existing authorization, so we'll
            // fake it and copy the existing auth with the remaining amount
            if (x.EXT_PAYPAL.equals(paymentPref.get(x.paymentMethodTypeId))) {
                String newAuthId = delegator.getNextSeqId(x.PaymentGatewayResponse);
                GenericValue authTrans = getAuthTransaction(paymentPref);
                GenericValue newAuthTrans = delegator.makeValue(x.PaymentGatewayResponse, authTrans);
                newAuthTrans.set(x.paymentGatewayResponseId, newAuthId);
                newAuthTrans.set(x.orderPaymentPreferenceId, newPref.get(x.orderPaymentPreferenceId));
                newAuthTrans.set(x.amount, splitAmount);
                savePgr(dctx, newAuthTrans);
            } else if (x.PAYMENT_NOT_AUTH.equals(statusId)) {
                // authorize the new preference
                processorResult = authPayment(dispatcher, userLogin, orh, newPref, splitAmount, false, null);
                if (processorResult != null) {
                    // process the auth results
                    boolean authResult = processResult(dctx, processorResult, userLogin, newPref, locale);
                    if (!authResult) {
                        Debug.logError(x.Authorization_failed + newPref + x.str_d98411eb + processorResult, MODULE);
                    }
                } else {
                    Debug.logError(x.Payment_not_authorized + newPref + x.no_process_result, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.ERROR_cannot_create_new_payment_preference + newPref, MODULE);
        } catch (GeneralException e) {
            if (processorResult != null) {
                Debug.logError(e, x.Trouble_processing_the_auth_result + newPref + x.str_d98411eb + processorResult, MODULE);
            } else {
                Debug.logError(e, x.Trouble_authorizing_the_payment + newPref, MODULE);
            }
        }
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> captureBillingAccountPayments(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String invoiceId = (String) context.get(x.invoiceId);
        String billingAccountId = (String) context.get(x.billingAccountId);
        BigDecimal captureAmount = (BigDecimal) context.get(x.captureAmount);
        captureAmount = captureAmount.setScale(DECIMALS, ROUNDING);
        BigDecimal capturedAmount = BigDecimal.ZERO;

        try {
            // Select all the unapplied payment applications associated to the billing account
            List<GenericValue> paymentApplications = DaoRegistry.getDao(delegator, x.PaymentApplication, UserLoginDao.class)
                    .findByAnd(delegator, x.PaymentApplication, UtilMisc.toMap(x.billingAccountId, billingAccountId, x.invoiceId, null),
                            UtilMisc.toList(x.amountApplied_18e2a1ba), false);
            if (UtilValidate.isNotEmpty(paymentApplications)) {
                Iterator<GenericValue> paymentApplicationsIt = paymentApplications.iterator();
                while (paymentApplicationsIt.hasNext()) {
                    if (capturedAmount.compareTo(captureAmount) >= 0) {
                        // we have captured all the amount required
                        break;
                    }
                    GenericValue paymentApplication = paymentApplicationsIt.next();
                    GenericValue payment = paymentApplication.getRelatedOne(x.Payment, false);
                    if (payment.getString(x.paymentPreferenceId) != null) {
                        // if the payment is reserved for a specific OrderPaymentPreference,
                        // we don't use it.
                        continue;
                    }
                    // TODO: check the statusId of the payment
                    BigDecimal paymentApplicationAmount = paymentApplication.getBigDecimal(x.amountApplied);
                    BigDecimal amountToCapture = paymentApplicationAmount.min(captureAmount.subtract(capturedAmount));
                    amountToCapture = amountToCapture.setScale(DECIMALS, ROUNDING);
                    if (amountToCapture.compareTo(paymentApplicationAmount) == 0) {
                        // apply the whole payment application to the invoice
                        paymentApplication.set(x.invoiceId, invoiceId);
                        paymentApplication.store();
                    } else {
                        // the amount to capture is lower than the amount available in this payment application:
                        // split the payment application into two records and apply one to the invoice
                        GenericValue newPaymentApplication = delegator.makeValue(x.PaymentApplication, paymentApplication);
                        String paymentApplicationId = delegator.getNextSeqId(x.PaymentApplication);
                        paymentApplication.set(x.invoiceId, invoiceId);
                        paymentApplication.set(x.amountApplied, amountToCapture);
                        paymentApplication.store();
                        newPaymentApplication.set(x.paymentApplicationId, paymentApplicationId);
                        newPaymentApplication.set(x.amountApplied, paymentApplicationAmount.subtract(amountToCapture));
                        newPaymentApplication.create();
                    }
                    capturedAmount = capturedAmount.add(amountToCapture);
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        capturedAmount = capturedAmount.setScale(DECIMALS, ROUNDING);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.captureAmount, capturedAmount);
        return results;
    }

    private static Map<String, Object> capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh,
            GenericValue paymentPref, BigDecimal amount, Locale locale) {
        return capturePayment(dctx, userLogin, orh, paymentPref, amount, null, locale);
    }

    private static Map<String, Object> capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh,
            GenericValue paymentPref, BigDecimal amount, GenericValue authTrans, Locale locale) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;

        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPref, CAPTURE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString(x.paymentService);
            }
            paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);

            if (serviceName == null) {
                Debug.logError(x.Service_name_is_null_for_payment_setting_cannot_process, MODULE);
                return null;
            }
        } else {
            Debug.logError(x.Invalid_payment_settings_entity_no_payment_settings_found, MODULE);
            return null;
        }

        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }

        // check the validity of the authorization; re-auth if necessary
        if (!PaymentGatewayServices.checkAuthValidity(paymentPref, paymentConfig)) {
            try {
                // re-auth required before capture
                Map<String, Object> processorResult = PaymentGatewayServices.authPayment(dispatcher, userLogin, orh, paymentPref, amount,
                        true, null);

                boolean authResult = false;
                if (processorResult != null) {
                    // process the auth results
                    try {
                        authResult = processResult(dctx, processorResult, userLogin, paymentPref, locale);
                        if (!authResult) {
                            Debug.logError(x.Re_Authorization_failed + paymentPref + x.str_d98411eb + processorResult, MODULE);
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, x.Trouble_processing_the_re_auth_result + paymentPref + x.str_d98411eb + processorResult, MODULE);
                    }
                } else {
                    Debug.logError(x.Payment_not_re_authorized + paymentPref + x.no_process_result, MODULE);
                }

                if (!authResult) {
                    // returning null to cancel the capture process.
                    return null;
                }

                // get the new auth transaction
                authTrans = getAuthTransaction(paymentPref);
            } catch (GeneralException e) {
                Debug.logError(e, x.Error_re_authorizing_payment, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPaymentReauthorizingError, locale));
            }
        }

        // prepare the context for the capture service (must follow the ccCaptureInterface
        Map<String, Object> captureContext = new HashMap<>();
        captureContext.put(x.userLogin, userLogin);
        captureContext.put(x.orderPaymentPreference, paymentPref);
        captureContext.put(x.paymentConfig, paymentConfig);
        captureContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
        captureContext.put(x.currency, orh.getCurrency());

        // this is necessary because the ccCaptureInterface uses "captureAmount" but the paymentProcessInterface uses "processAmount"
        try {
            ModelService captureService = dctx.getModelService(serviceName);
            Set<String> inParams = captureService.getInParamNames();
            if (inParams.contains(x.captureAmount)) {
                captureContext.put(x.captureAmount, amount);
            } else if (inParams.contains(x.processAmount)) {
                captureContext.put(x.processAmount, amount);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentServiceMissingAmount,
                        UtilMisc.toMap(x.serviceName, serviceName, x.inParams, inParams), locale));
            }
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentServiceCannotGetModel,
                    UtilMisc.toMap(x.serviceName, serviceName), locale));
        }


        if (authTrans != null) {
            captureContext.put(x.authTrans, authTrans);
        }

        if (Debug.infoOn()) {
            Debug.logInfo(x.Capture + serviceName + x.str_d7231b41 + captureContext, MODULE);
        }
        try {
            String paymentMethodTypeId = paymentPref.getString(x.paymentMethodTypeId);
            if (paymentMethodTypeId != null && x.GIFT_CARD.equals(paymentMethodTypeId)) {
                getBillingInformation(orh, paymentPref, captureContext);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        // now invoke the capture service
        Map<String, Object> captureResult = null;
        try {
            // NOTE DEJ20070819 calling this with a new transaction synchronously caused a deadlock because in this
            //transaction OrderHeader was updated and with this transaction paused and waiting for the new transaction
            //and the new transaction was waiting trying to read the same OrderHeader record; note that this only happens
            //for FinAccounts because they are processed internally whereas others are not
            // NOTE HOW TO FIX: don't call in separate transaction from here; individual services can have require-new-transaction
            //set to true if they want to behave that way (had: [, TX_TIME, true])
            captureResult = dispatcher.runSync(serviceName, captureContext);
            if (ServiceUtil.isError(captureResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(captureResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Could_not_capture_payment_serviceName + serviceName + x.context + captureContext, MODULE);
            return null;
        }

        // pass the payTo partyId to the result processor; we just add it to the result context.
        String payToPartyId = getPayToPartyId(orh.getOrderHeader());
        captureResult.put(x.payToPartyId, payToPartyId);

        // add paymentSettings to result; for use by later processors
        captureResult.put(x.paymentSettings, paymentSettings);

        // pass the currencyUomId as well
        captureResult.put(x.currencyUomId, orh.getCurrency());

        // log the error message as a gateway response when it fails
        if (ServiceUtil.isError(captureResult)) {
            saveError(dispatcher, userLogin, paymentPref, captureResult, CAPTURE_SERVICE_TYPE, x.PGT_CAPTURE);
        }

        return captureResult;
    }

    private static void saveError(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPref, Map<String, Object> result,
                                  String serviceType, String transactionCode) {
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.put(x.paymentServiceTypeEnumId, serviceType);
        serviceContext.put(x.orderPaymentPreference, paymentPref);
        serviceContext.put(x.transCodeEnumId, transactionCode);
        serviceContext.put(x.serviceResultMap, result);
        serviceContext.put(x.userLogin, userLogin);

        try {
            dispatcher.runAsync(x.processPaymentServiceError, serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    public static Map<String, Object> storePaymentErrorMessage(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String serviceType = (String) context.get(x.paymentServiceTypeEnumId);
        String transactionCode = (String) context.get(x.transCodeEnumId);
        Map<String, Object> result = UtilGenerics.cast(context.get(x.serviceResultMap));
        Locale locale = (Locale) context.get(x.locale);
        String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
        GenericValue response = delegator.makeValue(x.PaymentGatewayResponse);
        String message = ServiceUtil.getErrorMessage(result);
        if (message.length() > 255) {
            message = message.substring(0, 255);
        }
        response.set(x.paymentGatewayResponseId, responseId);
        response.set(x.paymentServiceTypeEnumId, serviceType);
        response.set(x.orderPaymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        response.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        response.set(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
        response.set(x.transCodeEnumId, transactionCode);
        response.set(x.referenceNum, x.ERROR);
        response.set(x.gatewayMessage, message);
        response.set(x.transactionDate, UtilDateTime.nowTimestamp());

        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingNoPaymentGatewayResponseCreatedForFailedService, locale));
        }

        Debug.logInfo(x.Created_PaymentGatewayResponse_record_for_returned_error, MODULE);
        return ServiceUtil.returnSuccess();
    }

    private static boolean processResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin,
            GenericValue paymentPreference, Locale locale) throws GeneralException {
        Boolean authResult = (Boolean) result.get(x.authResult);
        Boolean captureResult = (Boolean) result.get(x.captureResult);
        boolean resultPassed = false;
        String initialStatus = paymentPreference.getString(x.statusId);
        String authServiceType = null;

        if (authResult != null) {
            processAuthResult(dctx, result, userLogin, paymentPreference);
            resultPassed = authResult;
            authServiceType = (x.PAYMENT_NOT_AUTH.equals(initialStatus)) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;
        }
        if (captureResult != null) {
            processCaptureResult(dctx, result, userLogin, paymentPreference, authServiceType, locale);
            if (!resultPassed) {
                resultPassed = captureResult;
            }
        }
        return resultPassed;
    }

    private static void processAuthResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, GenericValue paymentPreference)
            throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        result.put(x.userLogin, userLogin);
        result.put(x.orderPaymentPreference, paymentPreference);
        ModelService model = dctx.getModelService(x.processAuthResult);
        ServiceContext context = new ServiceContext(model.makeValid(result, ModelService.IN_PARAM));

        // in case we rollback make sure this service gets called
        dispatcher.addRollbackService(model.getName(), context, true);

        // invoke the service
        Map<String, Object> resResp;
        try {
            resResp = dispatcher.runSync(model.getName(), context);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            throw e;
        }
        if (ServiceUtil.isError(resResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(resResp));
        }
    }

    public static Map<String, Object> processAuthResult(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Boolean authResult = (Boolean) context.get(x.authResult);
        String authType = (String) context.get(x.serviceTypeEnum);
        String currencyUomId = (String) context.get(x.currencyUomId);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        if (authResult == null) {
            Debug.logError(x.No_authentification_result_available_Payment_preference_can_t_be_checked, MODULE);
            return ServiceUtil
                    .returnError(UtilProperties.getMessage(RESOURCE, x.AccountingProcessingAuthResultEmpty, locale));
        }

        // refresh the payment preference
        try {
            orderPaymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // type of auth this was can be determined by the previous status
        if (UtilValidate.isEmpty(authType)) {
            authType = (x.PAYMENT_NOT_AUTH.equals(orderPaymentPreference.getString(x.statusId))) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;
        }

        try {
            String paymentMethodId = orderPaymentPreference.getString(x.paymentMethodId);
            GenericValue paymentMethod = DaoRegistry.getDao(delegator, x.PaymentMethod, UserLoginDao.class)
                    .findOne(delegator, x.PaymentMethod, UtilMisc.toMap(x.paymentMethodId, paymentMethodId), false);
            GenericValue creditCard = null;
            if (paymentMethod != null && x.CREDIT_CARD.equals(paymentMethod.getString(x.paymentMethodTypeId))) {
                creditCard = paymentMethod.getRelatedOne(x.CreditCard, false);
            }

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
            GenericValue response = delegator.makeValue(x.PaymentGatewayResponse);
            response.set(x.paymentGatewayResponseId, responseId);
            response.set(x.paymentServiceTypeEnumId, authType);
            response.set(x.orderPaymentPreferenceId, orderPaymentPreference.get(x.orderPaymentPreferenceId));
            response.set(x.paymentMethodTypeId, orderPaymentPreference.get(x.paymentMethodTypeId));
            response.set(x.paymentMethodId, orderPaymentPreference.get(x.paymentMethodId));
            response.set(x.transCodeEnumId, x.PGT_AUTHORIZE);
            response.set(x.currencyUomId, currencyUomId);

            // set the avs/fraud result
            response.set(x.gatewayAvsResult, context.get(x.avsCode));
            response.set(x.gatewayCvResult, context.get(x.cvCode));
            response.set(x.gatewayScoreResult, context.get(x.scoreCode));

            // set the auth info
            BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
            response.set(x.amount, processAmount);
            response.set(x.referenceNum, context.get(x.authRefNum));
            response.set(x.altReference, context.get(x.authAltRefNum));
            response.set(x.gatewayCode, context.get(x.authCode));
            response.set(x.gatewayFlag, context.get(x.authFlag));
            response.set(x.gatewayMessage, context.get(x.authMessage));
            response.set(x.transactionDate, UtilDateTime.nowTimestamp());

            if (Boolean.TRUE.equals(context.get(x.resultDeclined))) {
                response.set(x.resultDeclined, x.Y);
            }
            if (Boolean.TRUE.equals(context.get(x.resultNsf))) {
                response.set(x.resultNsf, x.Y);
            }
            if (Boolean.TRUE.equals(context.get(x.resultBadExpire))) {
                response.set(x.resultBadExpire, x.Y);
            }
            if (Boolean.TRUE.equals(context.get(x.resultBadCardNumber))) {
                response.set(x.resultBadCardNumber, x.Y);
            }

            // create the internal messages
            List<GenericValue> messageEntities = new LinkedList<>();
            List<String> messages = UtilGenerics.cast(context.get(x.internalRespMsgs));
            if (UtilValidate.isNotEmpty(messages)) {
                Iterator<String> i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue(x.PaymentGatewayRespMsg);
                    String respMsgId = delegator.getNextSeqId(x.PaymentGatewayRespMsg);
                    String message = i.next();
                    respMsg.set(x.paymentGatewayRespMsgId, respMsgId);
                    respMsg.set(x.paymentGatewayResponseId, responseId);
                    respMsg.set(x.pgrMessage, message);
                    messageEntities.add(respMsg);
                }
            }

            // save the response and respective messages
            savePgrAndMsgs(dctx, response, messageEntities);

            if (response.getBigDecimal(x.amount).compareTo((BigDecimal) context.get(x.processAmount)) != 0) {
                Debug.logWarning(x.The_authorized_amount_does_not_match_the_max_amount_Response + response + x.result_78289973
                        + context, MODULE);
            }

            // set the status of the OrderPaymentPreference
            boolean authResultOk = authResult;

            if (authResultOk) {
                orderPaymentPreference.set(x.statusId, x.PAYMENT_AUTHORIZED);
            } else {
                orderPaymentPreference.set(x.statusId, x.PAYMENT_DECLINED);
            }

            // remove sensitive credit card data regardless of outcome
            orderPaymentPreference.set(x.securityCode, null);
            orderPaymentPreference.set(x.track2, null);

            boolean needsNsfRetry = needsNsfRetry(orderPaymentPreference, context, delegator);
            if (needsNsfRetry) {
                orderPaymentPreference.set(x.needsNsfRetry, x.Y);
            } else {
                orderPaymentPreference.set(x.needsNsfRetry, x.N);
            }

            orderPaymentPreference.store();

            // if the payment was declined and this is a CreditCard, save that information on the CreditCard entity
            if (!authResultOk) {
                if (creditCard != null) {
                    Long consecutiveFailedAuths = creditCard.getLong(x.consecutiveFailedAuths);
                    if (consecutiveFailedAuths == null) {
                        creditCard.set(x.consecutiveFailedAuths, 1L);
                    } else {
                        creditCard.set(x.consecutiveFailedAuths, consecutiveFailedAuths + 1);
                    }
                    creditCard.set(x.lastFailedAuthDate, nowTimestamp);

                    if (Boolean.TRUE.equals(context.get(x.resultNsf))) {
                        Long consecutiveFailedNsf = creditCard.getLong(x.consecutiveFailedNsf);
                        if (consecutiveFailedNsf == null) {
                            creditCard.set(x.consecutiveFailedNsf, 1L);
                        } else {
                            creditCard.set(x.consecutiveFailedNsf, consecutiveFailedNsf + 1);
                        }
                        creditCard.set(x.lastFailedNsfDate, nowTimestamp);
                    }
                    creditCard.store();
                }
            }

            // auth was successful, to clear out any failed auth or nsf info
            if (authResultOk) {
                if ((creditCard != null) && (creditCard.get(x.lastFailedAuthDate) != null)) {
                    creditCard.set(x.consecutiveFailedAuths, 0L);
                    creditCard.set(x.lastFailedAuthDate, null);
                    creditCard.set(x.consecutiveFailedNsf, 0L);
                    creditCard.set(x.lastFailedNsfDate, null);
                    creditCard.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_updating_payment_status_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPaymentStatusUpdatingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    private static boolean needsNsfRetry(GenericValue orderPaymentPreference, Map<String, ? extends Object> processContext,
                                         Delegator delegator) throws GenericEntityException {
        boolean needsNsfRetry = false;
        if (Boolean.TRUE.equals(processContext.get(x.resultNsf))) {
            // only track this for auto-orders, since we will only not fail and re-try on those
            GenericValue orderHeader = orderPaymentPreference.getRelatedOne(x.OrderHeader, false);
            if (UtilValidate.isNotEmpty(orderHeader.getString(x.autoOrderShoppingListId))) {
                GenericValue productStore = orderHeader.getRelatedOne(x.ProductStore, false);
                if (x.Y.equals(productStore.getString(x.autoOrderCcTryLaterNsf))) {
                    // one last condition: make sure there have been less than ProductStore.autoOrderCcTryLaterMax
                    //   PaymentGatewayResponse records with the same orderPaymentPreferenceId and paymentMethodId (just in case it has changed)
                    //   and that have resultNsf = Y, ie only consider other NSF responses
                    Long autoOrderCcTryLaterMax = productStore.getLong(x.autoOrderCcTryLaterMax);
                    if (autoOrderCcTryLaterMax != null) {
                        long failedTries = DaoRegistry.getDao(delegator, x.PaymentGatewayResponse, UserLoginDao.class)
                                .findByAnd(delegator, x.PaymentGatewayResponse,
                                        UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreference.get(x.orderPaymentPreferenceId),
                                                x.paymentMethodId, orderPaymentPreference.get(x.paymentMethodId), x.resultNsf, x.Y),
                                        null, false)
                                .size();
                        if (failedTries < autoOrderCcTryLaterMax) {
                            needsNsfRetry = true;
                        }
                    } else {
                        needsNsfRetry = true;
                    }
                }
            }
        }
        return needsNsfRetry;
    }

    private static GenericValue processAuthRetryResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin,
                                                       GenericValue paymentPreference) throws GeneralException {
        processAuthResult(dctx, result, userLogin, paymentPreference);
        return getAuthTransaction(paymentPreference);
    }

    private static void processCaptureResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin,
            GenericValue paymentPreference, Locale locale) throws GeneralException {
        processCaptureResult(dctx, result, userLogin, paymentPreference, null, locale);
    }

    private static void processCaptureResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin,
            GenericValue paymentPreference, String authServiceType, Locale locale) throws GeneralException {
        if (result == null) {
            throw new GeneralException(x.Null_capture_result_sent_to_processCaptureResult_fatal_error);
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Boolean captureResult = (Boolean) result.get(x.captureResult);
        BigDecimal amount = null;
        if (result.get(x.captureAmount) != null) {
            amount = (BigDecimal) result.get(x.captureAmount);
        } else if (result.get(x.processAmount) != null) {
            amount = (BigDecimal) result.get(x.processAmount);
            result.put(x.captureAmount, amount);
        }

        if (amount == null) {
            throw new GeneralException(x.Unable_to_process_null_capture_amount);
        }

        // setup the amount big decimal
        amount = amount.setScale(DECIMALS, ROUNDING);

        result.put(x.orderPaymentPreference, paymentPreference);
        result.put(x.userLogin, userLogin);
        result.put(x.serviceTypeEnum, authServiceType);

        ModelService model = dctx.getModelService(x.processCaptureResult);
        ServiceContext context = new ServiceContext(model.makeValid(result, ModelService.IN_PARAM));
        Map<String, Object> capRes;
        try {
            capRes = dispatcher.runSync(x.processCaptureResult, context);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            throw e;
        }
        if (capRes != null && ServiceUtil.isError(capRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(capRes));
        }
        if (!captureResult) {
            // capture returned false (error)
            try {
                processReAuthFromCaptureFailure(dctx, result, amount, userLogin, paymentPreference, locale);
            } catch (GeneralException e) {
                // just log this for now (same as previous implementation)
                Debug.logError(e, MODULE);
            }
        }
    }

    private static void processReAuthFromCaptureFailure(DispatchContext dctx, Map<String, Object> result, BigDecimal amount,
            GenericValue userLogin, GenericValue paymentPreference, Locale locale) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        // lookup the order header
        OrderReadHelper orh = null;
        try {
            GenericValue orderHeader = paymentPreference.getRelatedOne(x.OrderHeader, false);
            if (orderHeader != null) {
                orh = new OrderReadHelper(orderHeader);
            }
        } catch (GenericEntityException e) {
            throw new GeneralException(x.Problems_getting_OrderHeader_cannot_re_auth_the_payment, e);
        }

        // make sure the order exists
        if (orh == null) {
            throw new GeneralException(x.No_order_found_for_payment_preference + paymentPreference.get(x.orderPaymentPreferenceId));
        }

        // set the re-auth amount
        if (amount == null) {
            amount = ZERO;
        }
        if (amount.compareTo(ZERO) == 0) {
            amount = paymentPreference.getBigDecimal(x.maxAmount);
            Debug.logInfo(x.resetting_payment_amount_from_0_00_to_correctMax_amount, MODULE);
        }
        Debug.logInfo(x.reauth_with_amount + amount, MODULE);

        // first re-auth the card
        Map<String, Object> authPayRes = authPayment(dispatcher, userLogin, orh, paymentPreference, amount, true, null);
        if (authPayRes == null) {
            throw new GeneralException(x.Null_result_returned_from_payment_re_authorization);
        }

        // check the auth-response
        Boolean authResp = (Boolean) authPayRes.get(x.authResult);
        Boolean capResp = (Boolean) authPayRes.get(x.captureResult);
        if (authResp != null && Boolean.TRUE.equals(authResp)) {
            GenericValue authTrans = processAuthRetryResult(dctx, authPayRes, userLogin, paymentPreference);
            // check if auto-capture was enabled; process if so
            if (capResp != null && capResp) {
                processCaptureResult(dctx, result, userLogin, paymentPreference, locale);
            } else {
                // no auto-capture; do manual capture now
                Map<String, Object> capPayRes = capturePayment(dctx, userLogin, orh, paymentPreference, amount, authTrans, locale);
                if (capPayRes == null) {
                    throw new GeneralException(x.Problems_trying_to_capture_payment_null_result);
                }

                // process the capture result
                Boolean capPayResp = (Boolean) capPayRes.get(x.captureResult);
                if (capPayResp != null && capPayResp) {
                    // process the capture result
                    processCaptureResult(dctx, capPayRes, userLogin, paymentPreference, locale);
                } else {
                    throw new GeneralException(x.Capture_of_authorized_payment_failed);
                }
            }
        } else {
            throw new GeneralException(x.Payment_re_authorization_failed);
        }
    }

    public static Map<String, Object> processCaptureResult(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue paymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String invoiceId = (String) context.get(x.invoiceId);
        String payTo = (String) context.get(x.payToPartyId);
        BigDecimal amount = (BigDecimal) context.get(x.captureAmount);
        String serviceType = (String) context.get(x.serviceTypeEnum);
        String currencyUomId = (String) context.get(x.currencyUomId);
        boolean captureSuccessful = (Boolean) context.get(x.captureResult);

        String paymentMethodTypeId = paymentPreference.getString(x.paymentMethodTypeId);

        if (UtilValidate.isEmpty(serviceType)) {
            serviceType = CAPTURE_SERVICE_TYPE;
        }

        // refresh the payment preference
        try {
            paymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the status and maxAmount
        String prefStatusId;
        if (captureSuccessful) {
            prefStatusId = x.EXT_BILLACT.equals(paymentMethodTypeId) ? x.PAYMENT_RECEIVED : x.PAYMENT_SETTLED;
        } else {
            prefStatusId = x.PAYMENT_DECLINED;
        }
        paymentPreference.set(x.statusId, prefStatusId);
        paymentPreference.set(x.maxAmount, amount);
        try {
            paymentPreference.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (!x.EXT_BILLACT.equals(paymentMethodTypeId)) {
            // create the PaymentGatewayResponse record
            String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
            GenericValue response = delegator.makeValue(x.PaymentGatewayResponse);
            response.set(x.paymentGatewayResponseId, responseId);
            response.set(x.paymentServiceTypeEnumId, serviceType);
            response.set(x.orderPaymentPreferenceId, paymentPreference.get(x.orderPaymentPreferenceId));
            response.set(x.paymentMethodTypeId, paymentMethodTypeId);
            response.set(x.paymentMethodId, paymentPreference.get(x.paymentMethodId));
            response.set(x.transCodeEnumId, x.PGT_CAPTURE);
            response.set(x.currencyUomId, currencyUomId);
            if (context.get(x.authRefNum) != null) {
                response.set(x.subReference, context.get(x.authRefNum));
                response.set(x.altReference, context.get(x.authAltRefNum));
            } else {
                response.set(x.altReference, context.get(x.captureAltRefNum));
            }

            // set the capture info
            response.set(x.amount, amount);
            response.set(x.referenceNum, context.get(x.captureRefNum));
            response.set(x.gatewayCode, context.get(x.captureCode));
            response.set(x.gatewayFlag, context.get(x.captureFlag));
            response.set(x.gatewayMessage, context.get(x.captureMessage));
            response.set(x.transactionDate, UtilDateTime.nowTimestamp());

            // save the response
            savePgr(dctx, response);

            // create the internal messages
            List<String> messages = UtilGenerics.cast(context.get(x.internalRespMsgs));
            if (UtilValidate.isNotEmpty(messages)) {
                Iterator<String> i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue(x.PaymentGatewayRespMsg);
                    String respMsgId = delegator.getNextSeqId(x.PaymentGatewayRespMsg);
                    String message = i.next();
                    respMsg.set(x.paymentGatewayRespMsgId, respMsgId);
                    respMsg.set(x.paymentGatewayResponseId, responseId);
                    respMsg.set(x.pgrMessage, message);

                    // save the message
                    savePgr(dctx, respMsg);
                }
            }

            // get the invoice
            GenericValue invoice = null;
            if (invoiceId != null) {
                try {
                    invoice = DaoRegistry.getDao(delegator, x.Invoice, UserLoginDao.class)
                            .findOne(delegator, x.Invoice, UtilMisc.toMap(x.invoiceId, invoiceId), false);
                } catch (GenericEntityException e) {
                    String message = UtilProperties.getMessage(RES_ERROR, x.AccountingFailedToProcessCaptureResult,
                            UtilMisc.toMap(x.invoiceId, invoiceId, x.errorString, e.getMessage()), locale);
                    Debug.logError(e, message, MODULE);
                    return ServiceUtil.returnError(message);
                }
            }

            // determine the partyIdFrom for the payment, which is who made the payment
            String partyIdFrom = null;
            if (invoice != null) {
                // get the party from the invoice, which is the bill-to party (partyId)
                partyIdFrom = invoice.getString(x.partyId);
            } else {
                // otherwise get the party from the order's OrderRole
                String orderId = paymentPreference.getString(x.orderId);
                GenericValue orderRole = null;
                try {
                    orderRole = DaoRegistry.getDao(delegator, x.OrderRole, UserLoginDao.class)
                            .findFirstByCondition(delegator, x.OrderRole,
                                    EntityCondition.makeCondition(UtilMisc.toMap(x.orderId, orderId, x.roleTypeId, x.BILL_TO_CUSTOMER)), null, null,
                                    false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (orderRole != null) {
                    partyIdFrom = orderRole.getString(x.partyId);
                }
            }

            // get the partyIdTo for the payment, which is who is receiving it
            String partyIdTo = null;
            if (UtilValidate.isNotEmpty(payTo)) {
                // use input pay to party
                partyIdTo = payTo;
            } else if (invoice != null) {
                // use the invoice partyIdFrom as the pay to party (which is who supplied the invoice)
                partyIdTo = invoice.getString(x.partyIdFrom);
            } else {
                // otherwise default to Company and print a big warning about this
                partyIdTo = EntityUtilProperties.getPropertyValue(x.general, x.ORGANIZATION_PARTY, x.Company, delegator);
                Debug.logWarning(x.Using_default_value_of + partyIdTo + x.for_payTo_on_invoice + invoiceId + x.and_orderPaymentPreference
                        + paymentPreference.getString(x.orderPaymentPreferenceId) + x.str_4ff447b8, MODULE);
            }


            Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap(x.paymentTypeId, x.CUSTOMER_PAYMENT);
            paymentCtx.put(x.paymentMethodTypeId, paymentPreference.get(x.paymentMethodTypeId));
            paymentCtx.put(x.paymentMethodId, paymentPreference.get(x.paymentMethodId));
            paymentCtx.put(x.paymentGatewayResponseId, responseId);
            paymentCtx.put(x.partyIdTo, partyIdTo);
            paymentCtx.put(x.partyIdFrom, partyIdFrom);
            paymentCtx.put(x.statusId, x.PMNT_RECEIVED);
            paymentCtx.put(x.paymentPreferenceId, paymentPreference.get(x.orderPaymentPreferenceId));
            paymentCtx.put(x.amount, amount);
            paymentCtx.put(x.currencyUomId, currencyUomId);
            paymentCtx.put(x.userLogin, userLogin);
            paymentCtx.put(x.paymentRefNum, context.get(x.captureRefNum));

            Map<String, Object> payRes;
            try {
                payRes = dispatcher.runSync(x.createPayment, paymentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreationError, locale));
            }
            if (ServiceUtil.isError(payRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(payRes));
            }

            String paymentId = (String) payRes.get(x.paymentId);

            // create the PaymentApplication if invoiceId is available
            if (invoiceId != null) {
                Debug.logInfo(x.Processing_Invoice + invoiceId, MODULE);
                Map<String, Object> paCtx = UtilMisc.<String, Object>toMap(x.paymentId, paymentId, x.invoiceId, invoiceId);
                paCtx.put(x.amountApplied, context.get(x.captureAmount));
                paCtx.put(x.userLogin, userLogin);
                Map<String, Object> paRes;
                try {
                    paRes = dispatcher.runSync(x.createPaymentApplication, paCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.AccountingInvoiceApplicationCreationError, locale));
                }
                if (paRes != null && ServiceUtil.isError(paRes)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(paRes));
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> refundOrderPaymentPreference(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderPaymentPreferenceId = (String) context.get(x.orderPaymentPreferenceId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = null;
        try {
            orderPaymentPreference = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findOne(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        // call the service refundPayment
        Map<String, Object> refundResponse = null;
        try {
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.put(x.orderPaymentPreference, orderPaymentPreference);
            serviceContext.put(x.refundAmount, amount);
            serviceContext.put(x.userLogin, userLogin);
            refundResponse = dispatcher.runSync(x.refundPayment, serviceContext, TX_TIME, true);
            if (ServiceUtil.isError(refundResponse)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(refundResponse));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_refunding_payment_through_processor, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentRefundError, locale));
        }
        refundResponse.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR, x.AccountingPaymentRefundedSuccessfully,
                UtilMisc.toMap(x.paymentId, refundResponse.get(x.paymentId), x.refundAmount, refundResponse.get(x.refundAmount)), locale)));
        return refundResponse;
    }

    public static Map<String, Object> refundPayment(DispatchContext dctx, PaymentGatewayServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get(x.refundAmount);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Cannot_get_OrderHeader_from_OrderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + e.toString());
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        GenericValue paymentSettings = null;
        if (orderHeader != null) {
            paymentSettings = getPaymentSettings(orderHeader, paymentPref, REFUND_SERVICE_TYPE, false);
        }

        String serviceName = null;
        String paymentGatewayConfigId = null;

        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString(x.paymentService);
            }
            String paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);

            if (serviceName != null) {
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put(x.orderPaymentPreference, paymentPref);
                serviceContext.put(x.paymentConfig, paymentConfig);
                serviceContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
                serviceContext.put(x.currency, orh.getCurrency());

                // get the creditCard/address/email
                String payToPartyId = orh.getBillToParty().getString(x.partyId);

                BigDecimal processAmount = refundAmount.setScale(DECIMALS, ROUNDING);
                serviceContext.put(x.refundAmount, processAmount);
                serviceContext.put(x.userLogin, userLogin);

                // call the service
                Map<String, Object> refundResponse = null;
                try {
                    refundResponse = dispatcher.runSync(serviceName, serviceContext, TX_TIME, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_refunding_payment_through_processor, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.AccountingPaymentRefundError, locale));
                }
                if (ServiceUtil.isError(refundResponse)) {
                    saveError(dispatcher, userLogin, paymentPref, refundResponse, REFUND_SERVICE_TYPE, x.PGT_REFUND);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(refundResponse));
                }

                // get the pay to party ID for the order (will be the payFrom)
                String payFromPartyId = getPayToPartyId(orderHeader);

                // process the refund result
                Map<String, Object> refundResRes;
                try {
                    ModelService model = dctx.getModelService(x.processRefundResult);
                    Map<String, Object> refundResCtx = model.makeValid(context, ModelService.IN_PARAM);
                    refundResCtx.put(x.currencyUomId, orh.getCurrency());
                    refundResCtx.put(x.payToPartyId, payToPartyId);
                    refundResCtx.put(x.payFromPartyId, payFromPartyId);
                    refundResCtx.put(x.refundRefNum, refundResponse.get(x.refundRefNum));
                    refundResCtx.put(x.refundAltRefNum, refundResponse.get(x.refundAltRefNum));
                    refundResCtx.put(x.refundMessage, refundResponse.get(x.refundMessage));
                    refundResCtx.put(x.refundResult, refundResponse.get(x.refundResult));

                    // The refund amount could be different from what we tell the payment gateway due to issues
                    // such as having to void the entire original auth amount and re-authorize the new order total.
                    BigDecimal actualRefundAmount = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(refundResponse.get(x.refundAmount),
                            x.BigDecimal, null, locale);
                    if (actualRefundAmount != null && actualRefundAmount.compareTo(processAmount) != 0) {
                        refundResCtx.put(x.refundAmount, refundResponse.get(x.refundAmount));
                    }
                    refundResRes = dispatcher.runSync(model.getName(), refundResCtx);
                    if (ServiceUtil.isError(refundResRes)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(refundResRes));
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.AccountingPaymentRefundError, locale) + x.str_b858cb28 + e.getMessage());
                }
                return refundResRes;
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentRefundServiceNotDefined, locale));
            }
        } else {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentSettingNotFound,
                    UtilMisc.toMap(x.productStoreId, orderHeader.getString(x.productStoreId),
                            x.transactionType, REFUND_SERVICE_TYPE), locale));
        }
    }

    public static Map<String, Object> processRefundResult(DispatchContext dctx, PaymentGatewayServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String currencyUomId = (String) context.get(x.currencyUomId);
        String payToPartyId = (String) context.get(x.payToPartyId);
        String payFromPartyId = (String) context.get(x.payFromPartyId);

        // create the PaymentGatewayResponse record
        String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
        GenericValue response = delegator.makeValue(x.PaymentGatewayResponse);
        response.set(x.paymentGatewayResponseId, responseId);
        response.set(x.paymentServiceTypeEnumId, REFUND_SERVICE_TYPE);
        response.set(x.orderPaymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        response.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        response.set(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
        response.set(x.transCodeEnumId, x.PGT_REFUND);

        // set the capture info
        response.set(x.amount, context.get(x.refundAmount));
        response.set(x.currencyUomId, currencyUomId);
        response.set(x.referenceNum, context.get(x.refundRefNum));
        response.set(x.altReference, context.get(x.refundAltRefNum));
        response.set(x.gatewayCode, context.get(x.refundCode));
        response.set(x.gatewayFlag, context.get(x.refundFlag));
        response.set(x.gatewayMessage, context.get(x.refundMessage));
        response.set(x.transactionDate, UtilDateTime.nowTimestamp());

        // save the response
        savePgr(dctx, response);

        // create the internal messages
        List<String> messages = UtilGenerics.cast(context.get(x.internalRespMsgs));
        if (UtilValidate.isNotEmpty(messages)) {
            Iterator<String> i = messages.iterator();
            while (i.hasNext()) {
                GenericValue respMsg = delegator.makeValue(x.PaymentGatewayRespMsg);
                String respMsgId = delegator.getNextSeqId(x.PaymentGatewayRespMsg);
                String message = i.next();
                respMsg.set(x.paymentGatewayRespMsgId, respMsgId);
                respMsg.set(x.paymentGatewayResponseId, responseId);
                respMsg.set(x.pgrMessage, message);

                // save the message
                savePgr(dctx, respMsg);
            }
        }

        Boolean refundResult = (Boolean) context.get(x.refundResult);
        if (refundResult != null && refundResult) {

            // mark the preference as refunded
            paymentPref.set(x.statusId, x.PAYMENT_REFUNDED);
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            // handle the (reverse) payment
            Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap(x.paymentTypeId, x.CUSTOMER_REFUND);
            paymentCtx.put(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
            paymentCtx.put(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
            paymentCtx.put(x.paymentGatewayResponseId, responseId);
            paymentCtx.put(x.partyIdTo, payToPartyId);
            paymentCtx.put(x.partyIdFrom, payFromPartyId);
            paymentCtx.put(x.statusId, x.PMNT_SENT);
            paymentCtx.put(x.paymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
            paymentCtx.put(x.currencyUomId, currencyUomId);
            paymentCtx.put(x.amount, context.get(x.refundAmount));
            paymentCtx.put(x.userLogin, userLogin);
            paymentCtx.put(x.paymentRefNum, context.get(x.refundRefNum));
            paymentCtx.put(x.comments, x.Refund);

            String paymentId = null;
            try {
                Map<String, Object> payRes = dispatcher.runSync(x.createPayment, paymentCtx);
                if (ServiceUtil.isError(payRes)) {
                    return ServiceUtil.returnError((String) payRes.get(ModelService.ERROR_MESSAGE));
                } else {
                    paymentId = (String) payRes.get(x.paymentId);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_creating_Payment, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreationError, locale));
            }

            if (paymentId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreationError, locale));
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.paymentId, paymentId);
            result.put(x.refundAmount, context.get(x.refundAmount));
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentRefundError, locale));
        }
    }

    public static Map<String, Object> retryFailedOrderAuth(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get(x.orderId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class)
                    .findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }

        // make sure we have a valid order record
        if (orderHeader == null || orderHeader.get(x.statusId) == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // check the current order status
        if (!x.ORDER_CREATED.equals(orderHeader.getString(x.statusId))) {
            // if we are out of the created status; then we were either cancelled, rejected or approved
            Debug.logWarning(x.Was_re_trying_a_failed_auth_for_orderId + orderId + x.but_it_is_not_in_the_ORDER_CREATED_status_so_skipping,
                    MODULE);
            return ServiceUtil.returnSuccess();
        }

        // run the auth service and check for failure(s)
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.authOrderPayments, UtilMisc.<String, Object>toMap(x.orderId, orderId, x.userLogin, userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
        }

        // check to see if there was a processor failure
        String authResp = (String) serviceResult.get(x.processResult);
        if (authResp == null) {
            authResp = x.ERROR;
        }

        if (x.ERROR.equals(authResp)) {
            Debug.logWarning(x.The_payment_processor_had_a_failure_in_processing_will_not_modify_any_status, MODULE);
        } else {
            if (x.FAILED.equals(authResp)) {
                // declined; update the order status
                OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
            } else if (x.APPROVED.equals(authResp)) {
                // approved; update the order status
                OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.processResult, authResp);

        return result;
    }


    public static Map<String, Object> retryFailedAuths(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        try (EntityListIterator eli = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                .findIteratorByCondition(
                        delegator,
                        x.OrderPaymentPreference,
                        EntityCondition.makeCondition(
                                EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.PAYMENT_NOT_AUTH),
                                EntityCondition.makeCondition(x.processAttempt, EntityOperator.GREATER_THAN, 0L)),
                        null,
                        UtilMisc.toList(x.orderId),
                        null)) {
            List<String> processList = new LinkedList<>();
            if (eli != null) {
                Debug.logInfo(x.Processing_failed_order_re_auth_s, MODULE);
                GenericValue value = null;
                while (((value = eli.next()) != null)) {
                    String orderId = value.getString(x.orderId);
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync(x.retryFailedOrderAuth, UtilMisc.<String, Object>toMap(x.orderId, orderId, x.userLogin, userLogin));
                            processList.add(orderId);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> retryFailedAuthNsfs(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        // get the date/time for one week before now since we'll only retry once a week for NSFs
        Calendar calcCal = Calendar.getInstance();
        calcCal.setTimeInMillis(System.currentTimeMillis());
        calcCal.add(Calendar.WEEK_OF_YEAR, -1);
        Timestamp oneWeekAgo = new Timestamp(calcCal.getTimeInMillis());

        try (EntityListIterator eli = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                .findIteratorByCondition(
                        delegator,
                        x.OrderPaymentPreference,
                        EntityCondition.makeCondition(
                                EntityCondition.makeCondition(x.needsNsfRetry, EntityOperator.EQUALS, x.Y),
                                EntityCondition.makeCondition(ModelEntity.STAMP_FIELD, EntityOperator.LESS_THAN_EQUAL_TO, oneWeekAgo)),
                        null,
                        UtilMisc.toList(x.orderId),
                        null)) {
            List<String> processList = new LinkedList<>();
            if (eli != null) {
                Debug.logInfo(x.Processing_failed_order_re_auth_s, MODULE);
                GenericValue value = null;
                while (((value = eli.next()) != null)) {
                    String orderId = value.getString(x.orderId);
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync(x.retryFailedOrderAuth, UtilMisc.<String, Object>toMap(x.orderId, orderId, x.userLogin, userLogin));
                            processList.add(orderId);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static GenericValue getCaptureTransaction(GenericValue orderPaymentPreference) {
        GenericValue capTrans = null;
        try {
            List<String> order = UtilMisc.toList(x.transactionDate_5a2f7760);
            List<GenericValue> transactions = orderPaymentPreference.getRelated(x.PaymentGatewayResponse, null, order, false);
            List<EntityExpr> exprs = UtilMisc.toList(
                    EntityCondition.makeCondition(x.paymentServiceTypeEnumId, EntityOperator.EQUALS, CAPTURE_SERVICE_TYPE),
                    EntityCondition.makeCondition(EntityFunction.upperField(x.referenceNum), EntityComparisonOperator.NOT_EQUAL,
                            EntityFunction.upper(x.ERROR)));
            List<GenericValue> capTransactions = EntityUtil.filterByAnd(transactions, exprs);
            capTrans = EntityUtil.getFirst(capTransactions);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.ERROR_Problem_getting_capture_information_from_PaymentGatewayResponse, MODULE);
        }
        return capTrans;
    }

    /**
     * Gets the chronologically latest PaymentGatewayResponse from an OrderPaymentPreference which is either a PRDS_PAY_AUTH
     * or PRDS_PAY_REAUTH.  Used for capturing.
     * @param orderPaymentPreference GenericValue object of the order payment preference
     * @return return the first authorization of the order payment preference
     */
    public static GenericValue getAuthTransaction(GenericValue orderPaymentPreference) {
        return EntityUtil.getFirst(getAuthTransactions(orderPaymentPreference));
    }

    /**
     * Gets a chronologically ordered list of PaymentGatewayResponses from an OrderPaymentPreference which is either a PRDS_PAY_AUTH
     * or PRDS_PAY_REAUTH.
     * @param orderPaymentPreference GenericValue object of the order payment preference
     * @return return the authorizations of the order payment preference
     */
    public static List<GenericValue> getAuthTransactions(GenericValue orderPaymentPreference) {
        List<GenericValue> authTransactions = null;
        try {
            List<String> order = UtilMisc.toList(x.transactionDate_5a2f7760);
            List<GenericValue> transactions = orderPaymentPreference.getRelated(x.PaymentGatewayResponse, null, order, false);
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition(x.paymentServiceTypeEnumId, EntityOperator.EQUALS,
                    AUTH_SERVICE_TYPE),
                    EntityCondition.makeCondition(x.paymentServiceTypeEnumId, EntityOperator.EQUALS, REAUTH_SERVICE_TYPE));
            authTransactions = EntityUtil.filterByOr(transactions, exprs);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.ERROR_Problem_getting_authorization_information_from_PaymentGatewayResponse, MODULE);
        }
        return authTransactions;
    }

    public static Timestamp getAuthTime(GenericValue orderPaymentPreference) {
        GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        Timestamp authTime = null;

        if (authTrans != null) {
            authTime = authTrans.getTimestamp(x.transactionDate);
        }

        return authTime;
    }

    public static boolean checkAuthValidity(GenericValue orderPaymentPreference, String paymentConfig) {
        Delegator delegator = orderPaymentPreference.getDelegator();
        Timestamp authTime = PaymentGatewayServices.getAuthTime(orderPaymentPreference);
        if (authTime == null) {
            return false;
        }

        String reauthDays = null;

        GenericValue paymentMethod = null;
        try {
            paymentMethod = orderPaymentPreference.getRelatedOne(x.PaymentMethod, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (paymentMethod != null && x.CREDIT_CARD.equals(paymentMethod.getString(x.paymentMethodTypeId))) {
            GenericValue creditCard = null;
            try {
                creditCard = paymentMethod.getRelatedOne(x.CreditCard, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (creditCard != null) {
                String cardType = creditCard.getString(x.cardType);
                // add more types as necessary -- maybe we should create seed data for credit card types??
                if (x.CCT_DISCOVER.equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_disc_days, x._90, delegator);
                } else if (x.CCT_AMERICANEXPRESS.equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_amex_days, x._30, delegator);
                } else if (x.CCT_MASTERCARD.equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_mc_days, x._30, delegator);
                } else if (x.CCT_VISA.equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_visa_days, x._7, delegator);
                } else {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_other_days, x._7, delegator);
                }

            }
        } else if (paymentMethod != null && x.EXT_PAYPAL.equals(paymentMethod.get(x.paymentMethodTypeId))) {
            reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_general_reauth_paypal_days, x._3, delegator);
        }

        if (reauthDays != null) {
            int days = 0;
            try {
                days = Integer.parseInt(reauthDays);
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }

            if (days > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(authTime.getTime());
                cal.add(Calendar.DAY_OF_YEAR, days);
                Timestamp validTime = new Timestamp(cal.getTimeInMillis());
                Timestamp nowTime = UtilDateTime.nowTimestamp();
                if (nowTime.after(validTime)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Saves either a PaymentGatewayResponse or PaymentGatewayRespMsg value and ensures that the value
     * is persisted even in the event of a rollback.
     * @param dctx
     * @param pgr Either a PaymentGatewayResponse or PaymentGatewayRespMsg GenericValue
     */
    private static void savePgr(DispatchContext dctx, GenericValue pgr) {
        Map<String, GenericValue> context = UtilMisc.<String, GenericValue>toMap(x.paymentGatewayResponse, pgr);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            dispatcher.addRollbackService(x.savePaymentGatewayResponse, context, true);
            delegator.create(pgr);
        } catch (GenericEntityException | GenericServiceException ge) {
            Debug.logError(ge, MODULE);
        }
    }

    private static void savePgrAndMsgs(DispatchContext dctx, GenericValue pgr, List<GenericValue> messages) {
        Map<String, GenericValue> context = UtilMisc.<String, GenericValue>toMap(x.paymentGatewayResponse, pgr, x.messages, messages);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            dispatcher.addRollbackService(x.savePaymentGatewayResponseAndMessages, context, true);
            delegator.create(pgr);
            for (GenericValue message : messages) {
                delegator.create(message);
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            Debug.logError(ge, MODULE);
        }
    }

    public static Map<String, Object> savePaymentGatewayResponse(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue pgr = (GenericValue) context.get(x.paymentGatewayResponse);
        if (x.PaymentGatewayResponse.equals(pgr.getEntityName())) {
            String message = pgr.getString(x.gatewayMessage);
            if (UtilValidate.isNotEmpty(message) && message.length() > 255) {
                pgr.set(x.gatewayMessage, message.substring(0, 255));
            }
        }

        try {
            delegator.create(pgr);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> savePaymentGatewayResponseAndMessages(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue pgr = (GenericValue) context.get(x.paymentGatewayResponse);
        String gatewayMessage = pgr.getString(x.gatewayMessage);
        if (UtilValidate.isNotEmpty(gatewayMessage) && gatewayMessage.length() > 255) {
            pgr.set(x.gatewayMessage, gatewayMessage.substring(0, 255));
        }
        @SuppressWarnings(x.unchecked)
        List<GenericValue> messages = (List<GenericValue>) context.get(x.messages);

        try {
            delegator.create(pgr);
            for (GenericValue message : messages) {
                delegator.create(message);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    // manual auth service
    public static Map<String, Object> processManualCcAuth(DispatchContext dctx, PaymentGatewayServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        // security check
        if (!security.hasEntityPermission(x.MANUAL, x.PAYMENT, userLogin) && !security.hasEntityPermission(x.ACCOUNTING, x.CREATE, userLogin)) {
            Debug.logWarning(x.Security + (new Date()).toString() + x.str_89222ecc + userLogin.get(x.userLoginId)
                    + x.attempt_to_run_manual_payment_transaction, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionNotAuthorized, locale));
        }

        String paymentMethodId = (String) context.get(x.paymentMethodId);
        String productStoreId = (String) context.get(x.productStoreId);
        String securityCode = (String) context.get(x.securityCode);
        BigDecimal amount = (BigDecimal) context.get(x.amount);

        // check the payment method; verify type
        GenericValue paymentMethod;
        try {
            paymentMethod = DaoRegistry.getDao(delegator, x.PaymentMethod, UserLoginDao.class)
                    .findOne(delegator, x.PaymentMethod, UtilMisc.toMap(x.paymentMethodId, paymentMethodId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (paymentMethod == null || !x.CREDIT_CARD.equals(paymentMethod.getString(x.paymentMethodTypeId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentManualAuthOnlyForCreditCard, locale));
        }

        // get the billToParty object
        GenericValue billToParty;
        try {
            billToParty = paymentMethod.getRelatedOne(x.Party, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // get the credit card object
        GenericValue creditCard;
        try {
            creditCard = DaoRegistry.getDao(delegator, x.CreditCard, UserLoginDao.class)
                    .findOne(delegator, x.CreditCard, UtilMisc.toMap(x.paymentMethodId, paymentMethodId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(creditCard)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentCreditCardNotFound,
                    UtilMisc.toMap(x.paymentMethodId, paymentMethodId), locale));
        }

        // get the transaction settings
        String paymentService = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;

        GenericValue paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, x.CREDIT_CARD,
                x.PRDS_PAY_AUTH, false);
        if (paymentSettings == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentSettingNotFound,
                    UtilMisc.toMap(x.productStoreId, productStoreId, x.transactionType, x.emptyString), locale));
        } else {
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                paymentService = getPaymentCustomMethod(delegator, customMethodId);
            }
            if (UtilValidate.isEmpty(paymentService)) {
                paymentService = paymentSettings.getString(x.paymentService);
            }
            paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);
            if (UtilValidate.isEmpty(paymentConfig)) {
                paymentConfig = x.payment_properties;
            }
        }

        // prepare the order payment preference (facade)
        GenericValue orderPaymentPref = delegator.makeValue(x.OrderPaymentPreference, new HashMap<>());
        orderPaymentPref.set(x.orderPaymentPreferenceId, x.NA);
        orderPaymentPref.set(x.orderId, x.NA);
        orderPaymentPref.set(x.presentFlag, x.N);
        orderPaymentPref.set(x.overflowFlag, x.Y);
        orderPaymentPref.set(x.paymentMethodTypeId, x.CREDIT_CARD);
        orderPaymentPref.set(x.paymentMethodId, paymentMethodId);
        if (UtilValidate.isNotEmpty(securityCode)) {
            orderPaymentPref.set(x.securityCode, securityCode);
        }
        // this record is not to be stored, just passed to the service for use

        // get the default currency
        String currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);

        // prepare the auth context
        Map<String, Object> authContext = new HashMap<>();
        authContext.put(x.orderId, x.NA);
        authContext.put(x.orderItems, new LinkedList<>());
        authContext.put(x.orderPaymentPreference, orderPaymentPref);
        authContext.put(x.creditCard, creditCard);
        authContext.put(x.billToParty, billToParty);
        authContext.put(x.currency, currency);
        authContext.put(x.paymentConfig, paymentConfig);
        authContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
        authContext.put(x.processAmount, amount);
        authContext.put(x.userLogin, userLogin);

        // call the auth service
        Map<String, Object> response;
        try {
            Debug.logInfo(x.Running_authorization_service + paymentService, MODULE);
            response = dispatcher.runSync(paymentService, authContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentServiceError,
                    UtilMisc.toMap(x.paymentService, paymentService, x.authContext, authContext),
                    locale));
        }
        if (ServiceUtil.isError(response)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(response));
        }

        Boolean authResult = (Boolean) response.get(x.authResult);
        Debug.logInfo(x.Authorization_service_returned + authResult, MODULE);
        if (authResult != null && authResult) {
            return ServiceUtil.returnSuccess();
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentAuthorizationFailed, locale));
        }
    }

    // manual processing service
    public static Map<String, Object> processManualCcTx(DispatchContext dctx, PaymentGatewayServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        // security check
        if (!security.hasEntityPermission(x.MANUAL, x.PAYMENT, userLogin) && !security.hasEntityPermission(x.ACCOUNTING, x.CREATE, userLogin)) {
            Debug.logWarning(x.Security + (new Date()).toString() + x.str_89222ecc + userLogin.get(x.userLoginId)
                    + x.attempt_to_run_manual_payment_transaction, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionNotAuthorized, locale));
        }
        String orderPaymentPreferenceId = (String) context.get(x.orderPaymentPreferenceId);
        String paymentMethodTypeId = (String) context.get(x.paymentMethodTypeId);
        String productStoreId = (String) context.get(x.productStoreId);
        String transactionType = (String) context.get(x.transactionType);
        String referenceCode = (String) context.get(x.referenceCode);
        if (referenceCode == null) {
            referenceCode = Long.valueOf(System.currentTimeMillis()).toString();
        }
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            paymentPref = DaoRegistry.getDao(delegator, x.OrderPaymentPreference, UserLoginDao.class)
                    .findOne(delegator, x.OrderPaymentPreference, UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.Problem_getting_OrderPaymentPreference_for_orderPaymentPreferenceId
                    + orderPaymentPreferenceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        // Error if no OrderPaymentPreference was found
        if (paymentPref == null) {
            Debug.logWarning(x.Could_not_find_OrderPaymentPreference_with_orderPaymentPreferenceId
                    + orderPaymentPreferenceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingProblemGettingOrderPaymentPreferences, locale) + x.str_b858cb28
                    + orderPaymentPreferenceId);
        }
        // Get the OrderHeader
        GenericValue orderHeader = null;
        String orderId = paymentPref.getString(x.orderId);
        try {
            orderHeader = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class)
                    .findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.Problem_getting_OrderHeader_for_orderId + orderId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }
        // Error if no OrderHeader was found
        if (orderHeader == null) {
            Debug.logWarning(x.Could_not_find_OrderHeader_with_orderId + orderId + x.not_processing_payments, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        // check valid implemented types
        if (!transactionType.equals(CREDIT_SERVICE_TYPE)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionNotYetSupported, locale));
        }
        // transaction request context
        Map<String, Object> requestContext = new HashMap<>();
        String paymentService = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;
        // get the transaction settings
        GenericValue paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId,
                transactionType, false);
        if (paymentSettings == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentSettingNotFound,
                    UtilMisc.toMap(x.productStoreId, productStoreId, x.transactionType, transactionType), locale));
        } else {
            paymentGatewayConfigId = paymentSettings.getString(x.paymentGatewayConfigId);
            String customMethodId = paymentSettings.getString(x.paymentCustomMethodId);
            if (UtilValidate.isNotEmpty(customMethodId)) {
                paymentService = getPaymentCustomMethod(delegator, customMethodId);
            }
            if (UtilValidate.isEmpty(paymentService)) {
                paymentService = paymentSettings.getString(x.paymentService);
            }
            paymentConfig = paymentSettings.getString(x.paymentPropertiesPath);
            if (paymentConfig == null) {
                paymentConfig = x.payment_properties;
            }
            requestContext.put(x.paymentConfig, paymentConfig);
            requestContext.put(x.paymentGatewayConfigId, paymentGatewayConfigId);
        }
        // check the service name
        if (paymentService == null || paymentGatewayConfigId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentSettingNotValid, locale));
        }

        if (x.CREDIT_CARD.equals(paymentMethodTypeId)) {
            GenericValue creditCard = delegator.makeValue(x.CreditCard);
            creditCard.setAllFields(context, true, null, null);
            if (creditCard.get(x.firstNameOnCard) == null || creditCard.get(x.lastNameOnCard) == null || creditCard.get(x.cardType) == null
                    || creditCard.get(x.cardNumber) == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreditCardMissingMandatoryFields, locale));
            }
            String expMonth = (String) context.get(x.expMonth);
            String expYear = (String) context.get(x.expYear);
            String expDate = expMonth + x.str_42099b4a + expYear;
            creditCard.set(x.expireDate, expDate);
            requestContext.put(x.creditCard, creditCard);
            requestContext.put(x.cardSecurityCode, context.get(x.cardSecurityCode));
            GenericValue billingAddress = delegator.makeValue(x.PostalAddress);
            billingAddress.setAllFields(context, true, null, null);
            if (billingAddress.get(x.address1) == null || billingAddress.get(x.city) == null || billingAddress.get(x.postalCode) == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreditCardBillingAddressMssingMandatoryFields, locale));
            }
            requestContext.put(x.billingAddress, billingAddress);
            GenericValue billToEmail = delegator.makeValue(x.ContactMech);
            billToEmail.set(x.infoString, context.get(x.infoString));
            if (billToEmail.get(x.infoString) == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreditCardEmailAddressCannotBeEmpty, locale));
            }
            requestContext.put(x.billToParty, orh.getBillToParty());
            requestContext.put(x.billToEmail, billToEmail);
            requestContext.put(x.referenceCode, referenceCode);
            String currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
            requestContext.put(x.currency, currency);
            requestContext.put(x.creditAmount, context.get(x.amount));
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionNotYetSupported, locale) + x.str_b858cb28 + paymentMethodTypeId);
        }
        // process the transaction
        Map<String, Object> response = null;
        try {
            response = dispatcher.runSync(paymentService, requestContext, TX_TIME, true);
            if (ServiceUtil.isError(response)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(response));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentServiceError,
                    UtilMisc.toMap(x.paymentService, paymentService, x.authContext, requestContext),
                    locale));
        }
        // get the response result code
        if (response != null && ServiceUtil.isSuccess(response)) {
            Map<String, Object> responseRes;
            try {
                ModelService model = dctx.getModelService(x.processCreditResult);
                response.put(x.orderPaymentPreference, paymentPref);
                response.put(x.userLogin, userLogin);
                Map<String, Object> resCtx = model.makeValid(response, ModelService.IN_PARAM);
                responseRes = dispatcher.runSync(model.getName(), resCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingPaymentCreditError,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
            if (ServiceUtil.isError(responseRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(responseRes));
            }
        } else if (ServiceUtil.isError(response)) {
            saveError(dispatcher, userLogin, paymentPref, response, CREDIT_SERVICE_TYPE, x.PGT_CREDIT);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(response));
        }
        // check for errors
        if (ServiceUtil.isError(response)) {
            return ServiceUtil.returnError(ServiceUtil.makeErrorMessage(response, null, null, null, null));
        }
        // get the reference number
        String refNum = (String) response.get(x.creditRefNum);
        String code = (String) response.get(x.creditCode);
        String msg = (String) response.get(x.creditMessage);
        Map<String, Object> returnResults = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTransactionManualResult,
                UtilMisc.toMap(x.msg, msg, x.code, code, x.refNum, refNum), locale));
        returnResults.put(x.referenceNum, refNum);
        return returnResults;
    }

    // Verify Credit Card (Manually) Service
    public static Map<String, Object> verifyCreditCard(DispatchContext dctx, PaymentGatewayServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get(x.productStoreId);
        String mode = (String) context.get(x.mode);
        String paymentMethodId = (String) context.get(x.paymentMethodId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        if (Debug.infoOn()) {
            Debug.logInfo(x.Running_verifyCreditCard + paymentMethodId + x.for_store + productStoreId, MODULE);
        }

        GenericValue productStore = null;
        productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);

        String productStorePaymentProperties = x.payment_properties;
        if (productStore != null) {
            productStorePaymentProperties = ProductStoreWorker.getProductStorePaymentProperties(delegator, productStoreId, x.CREDIT_CARD,
                    x.PRDS_PAY_AUTH, false);
        }

        String amount = null;
        if (x.CREATE_cabc2219.equalsIgnoreCase(mode)) {
            amount = EntityUtilProperties.getPropertyValue(productStorePaymentProperties, x.payment_general_cc_create_auth, delegator);
        } else if (x.UPDATE.equalsIgnoreCase(mode)) {
            amount = EntityUtilProperties.getPropertyValue(productStorePaymentProperties, x.payment_general_cc_update_auth, delegator);
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.Running_credit_card_verification + paymentMethodId + x.str_6ed2c097 + amount + x.str_64b59c07 + productStorePaymentProperties
                    + x.str_d98411eb + mode, MODULE);
        }

        if (UtilValidate.isNotEmpty(amount)) {
            BigDecimal authAmount = new BigDecimal(amount);
            if (authAmount.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> ccAuthContext = new HashMap<>();
                ccAuthContext.put(x.paymentMethodId, paymentMethodId);
                ccAuthContext.put(x.productStoreId, productStoreId);
                ccAuthContext.put(x.amount, authAmount);
                ccAuthContext.put(x.userLogin, userLogin);

                Map<String, Object> results;
                try {
                    results = dispatcher.runSync(x.manualForcedCcAuthTransaction, ccAuthContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingCreditCardManualAuthFailedError, locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // ****************************************************
    // Test Services
    // ****************************************************


    /**
     * Simple test processor; declines all orders &lt; 100.00; approves all orders &gt;= 100.00
     */
    public static Map<String, Object> testProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = new HashMap<>();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);

        if (processAmount != null && processAmount.compareTo(new BigDecimal(x._100_00)) >= 0) {
            result.put(x.authResult, Boolean.TRUE);
        }
        if (processAmount != null && processAmount.compareTo(new BigDecimal(x._100_00)) < 0) {
            result.put(x.authResult, Boolean.FALSE);
        }
        result.put(x.customerRespMsgs, UtilMisc.toList(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessorMinimumPurchase, locale)));
        if (processAmount == null) {
            result.put(x.authResult, null);
        }

        String refNum = UtilDateTime.nowAsString();

        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authFlag, x.X);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestProcessor, locale));
        result.put(x.internalRespMsgs, UtilMisc.toList(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessor, locale)));
        return result;
    }


    /**
     * Simple test processor; declines all orders &lt; 100.00; approves all orders &gt; 100.00
     */
    public static Map<String, Object> testProcessorWithCapture(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = new HashMap<>();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);

        if (processAmount != null && processAmount.compareTo(new BigDecimal(x._100_00)) >= 0) {
            result.put(x.authResult, Boolean.TRUE);
        }
        result.put(x.captureResult, Boolean.TRUE);
        if (processAmount != null && processAmount.compareTo(new BigDecimal(x._100_00)) < 0) {
            result.put(x.authResult, Boolean.FALSE);
        }
        result.put(x.captureResult, Boolean.FALSE);
        result.put(x.customerRespMsgs, UtilMisc.toList(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessorMinimumPurchase, locale)));
        if (processAmount == null) {
            result.put(x.authResult, null);
        }

        String refNum = UtilDateTime.nowAsString();

        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.captureRefNum, refNum);
        result.put(x.captureAltRefNum, refNum);
        result.put(x.authCode, x._100);
        result.put(x.captureCode, x._200);
        result.put(x.authFlag, x.X);
        result.put(x.authMessage, UtilMisc.toList(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestCapture, locale)));
        result.put(x.internalRespMsgs, UtilMisc.toList(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestCapture, locale)));
        return result;
    }

    /**
     *  Test authorize - does random declines
     */
    public static Map<String, Object> testRandomAuthorize(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String refNum = UtilDateTime.nowAsString();
        int i = SECURE_RANDOM.nextInt(9);
        if (i < 5 || i % 2 == 0) {
            result.put(x.authResult, Boolean.TRUE);
            result.put(x.authFlag, x.A);
        } else {
            result.put(x.authResult, Boolean.FALSE);
            result.put(x.authFlag, x.D_50c9e8d5);
        }

        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authCode, x._100);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestCapture, locale));

        return result;
    }

    /**
     * Always approve processor.
     */
    public static Map<String, Object> alwaysApproveProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = new HashMap<>();
        Debug.logInfo(x.Test_Processor_Approving_Credit_Card, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.authResult, Boolean.TRUE);
        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authCode, x._100);
        result.put(x.authFlag, x.A);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessor, locale));
        return result;
    }

    public static Map<String, Object> alwaysApproveWithCapture(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = new HashMap<>();
        String refNum = UtilDateTime.nowAsString();
        Debug.logInfo(x.Test_Processor_Approving_Credit_Card_with_Capture, MODULE);

        result.put(x.authResult, Boolean.TRUE);
        result.put(x.captureResult, Boolean.TRUE);
        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.captureRefNum, refNum);
        result.put(x.captureAltRefNum, refNum);
        result.put(x.authCode, x._100);
        result.put(x.captureCode, x._200);
        result.put(x.authFlag, x.A);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestCapture, locale));
        return result;
    }


    /**
     * Always decline processor
     */
    public static Map<String, Object> alwaysDeclineProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        Debug.logInfo(x.Test_Processor_Declining_Credit_Card, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.authResult, Boolean.FALSE);
        result.put(x.processAmount, processAmount);
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authFlag, x.D_50c9e8d5);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessorDeclined, locale));
        return result;
    }

    /**
     * Always NSF (not sufficient funds) processor
     */
    public static Map<String, Object> alwaysNsfProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        Debug.logInfo(x.Test_Processor_NSF_Credit_Card, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.authResult, Boolean.FALSE);
        result.put(x.resultNsf, Boolean.TRUE);
        result.put(x.processAmount, processAmount);
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authFlag, x.N);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessor, locale));
        return result;
    }

    /**
     * Always fail/bad expire date processor
     */
    public static Map<String, Object> alwaysBadExpireProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        Debug.logInfo(x.Test_Processor_Bad_Expire_Date_Credit_Card, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.authResult, Boolean.FALSE);
        result.put(x.resultBadExpire, Boolean.TRUE);
        result.put(x.processAmount, processAmount);
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authFlag, x.E);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestProcessor, locale));
        return result;
    }

    /**
     * Fail/bad expire date when year is even processor
     */
    public static Map<String, Object> badExpireEvenProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        String expireDate = creditCard.getString(x.expireDate);
        String lastNumberStr = expireDate.substring(expireDate.length() - 1);
        int lastNumber = Integer.parseInt(lastNumberStr);

        if (lastNumber % 2.0 == 0.0) {
            return alwaysBadExpireProcessor(dctx, context);
        } else {
            return alwaysApproveProcessor(dctx, context);
        }
    }

    /**
     * Always bad card number processor
     */
    public static Map<String, Object> alwaysBadCardNumberProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        Debug.logInfo(x.Test_Processor_Bad_Card_Number_Credit_Card, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.authResult, Boolean.FALSE);
        result.put(x.resultBadCardNumber, Boolean.TRUE);
        result.put(x.processAmount, processAmount);
        result.put(x.authRefNum, refNum);
        result.put(x.authAltRefNum, refNum);
        result.put(x.authFlag, x.N);
        result.put(x.authMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestBadCardNumber, locale));
        return result;
    }

    /**
     * Always fail (error) processor
     */
    public static Map<String, Object> alwaysFailProcessor(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                x.AccountingPaymentTestAuthorizationAlwaysFailed, locale));
    }

    public static Map<String, Object> testRelease(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String refNum = UtilDateTime.nowAsString();

        result.put(x.releaseResult, Boolean.TRUE);
        result.put(x.releaseAmount, context.get(x.releaseAmount));
        result.put(x.releaseRefNum, refNum);
        result.put(x.releaseAltRefNum, refNum);
        result.put(x.releaseFlag, x.U);
        result.put(x.releaseMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestRelease, locale));
        return result;
    }

    /**
     * Test capture service (returns true)
     */
    public static Map<String, Object> testCapture(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo(x.Test_Capture_Process, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.captureResult, Boolean.TRUE);
        result.put(x.captureAmount, context.get(x.captureAmount));
        result.put(x.captureRefNum, refNum);
        result.put(x.captureAltRefNum, refNum);
        result.put(x.captureFlag, x.C);
        result.put(x.captureMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestCapture, locale));
        return result;
    }

    /**
     * Always decline processor
     */
    public static Map<String, Object> testCCProcessorCaptureAlwaysDecline(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get(x.captureAmount);
        Debug.logInfo(x.Test_Processor_Declining_Credit_Card_capture, MODULE);

        String refNum = UtilDateTime.nowAsString();

        result.put(x.captureResult, Boolean.FALSE);
        result.put(x.captureAmount, processAmount);
        result.put(x.captureRefNum, refNum);
        result.put(x.captureAltRefNum, refNum);
        result.put(x.captureFlag, x.D_50c9e8d5);
        result.put(x.captureMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestCaptureDeclined, locale));
        return result;
    }

    public static Map<String, Object> testCaptureWithReAuth(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        Debug.logInfo(x.Test_Capture_with_2_minute_delay_failure_re_auth_process, MODULE);

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentCannotBeCaptured, locale));
        }
        Timestamp txStamp = authTransaction.getTimestamp(x.transactionDate);
        Timestamp nowStamp = UtilDateTime.nowTimestamp();

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.captureAmount, context.get(x.captureAmount));
        result.put(x.captureRefNum, UtilDateTime.nowAsString());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(txStamp.getTime());
        cal.add(Calendar.MINUTE, 2);
        Timestamp twoMinAfter = new Timestamp(cal.getTimeInMillis());
        if (Debug.infoOn()) {
            Debug.logInfo(x.Re_Auth_Capture_Test_Tx_Date + txStamp + x._2_Min + twoMinAfter + x.Now + nowStamp, MODULE);
        }

        if (nowStamp.after(twoMinAfter)) {
            result.put(x.captureResult, Boolean.FALSE);
        } else {
            result.put(x.captureResult, Boolean.TRUE);
            result.put(x.captureFlag, x.C);
            result.put(x.captureMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestCaptureWithReauth, locale));
        }

        return result;
    }

    /**
     * Test refund service (returns true)
     */
    public static Map<String, Object> testRefund(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo(x.Test_Refund_Process, MODULE);

        result.put(x.refundResult, Boolean.TRUE);
        result.put(x.refundAmount, context.get(x.refundAmount));
        result.put(x.refundRefNum, UtilDateTime.nowAsString());
        result.put(x.refundFlag, x.R);
        result.put(x.refundMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestRefund, locale));
        return result;
    }

    public static Map<String, Object> testRefundFailure(DispatchContext dctx, PaymentGatewayServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo(x.Test_Refund_Process, MODULE);

        result.put(x.refundResult, Boolean.FALSE);
        result.put(x.refundAmount, context.get(x.refundAmount));
        result.put(x.refundRefNum, UtilDateTime.nowAsString());
        result.put(x.refundFlag, x.R);
        result.put(x.refundMessage, UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTestRefundFailure, locale));
        return result;
    }

    private static String getPaymentCustomMethod(Delegator delegator, String customMethodId) {
        String serviceName = null;
        GenericValue customMethod = null;
        try {
            customMethod = DaoRegistry.getDao(delegator, x.CustomMethod, UserLoginDao.class)
                    .findOne(delegator, x.CustomMethod, UtilMisc.toMap(x.customMethodId, customMethodId), false);
            if (UtilValidate.isNotEmpty(customMethod)) {
                serviceName = customMethod.getString(x.customMethodName);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return serviceName;
    }

    private static boolean isReplacementOrder(GenericValue orderHeader) {
        boolean replacementOrderFlag = false;

        List<GenericValue> returnItemResponses = new LinkedList<>();
        try {
            returnItemResponses = orderHeader.getRelated(x.ReplacementReturnItemResponse, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return replacementOrderFlag;
        }
        if (UtilValidate.isNotEmpty(returnItemResponses)) {
            replacementOrderFlag = true;
        }

        return replacementOrderFlag;
    }
}
