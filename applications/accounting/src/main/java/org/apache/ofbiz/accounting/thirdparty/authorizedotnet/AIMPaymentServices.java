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

package org.apache.ofbiz.accounting.thirdparty.authorizedotnet;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewayAuthorizeNetDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewayAuthorizeNetEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.AIMPaymentServicesContext;
public class AIMPaymentServices {

    private static final String MODULE = AIMPaymentServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    // The list of refund failure response codes that would cause the ccRefund service
    // to attempt to void the refund's associated authorization transaction.  This list
    // contains the responses where the voiding does not need to be done within a certain
    // time limit
    private static final List<String> VOIDABLE_RESPONSES_NO_TIME_LIMIT = UtilMisc.toList(x._50);

    // A list of refund failure response codes that would cause the ccRefund service
    // to first check whether the refund's associated authorization transaction has occurred
    // within a certain time limit, and if so, cause it to void the transaction
    private static final List<String> VOIDABLE_RESPONSES_TIME_LIMIT = UtilMisc.toList(x._54);

    // The number of days in the time limit when one can safely consider an unsettled
    // transaction to be still valid
    private static final int TIME_LIMIT_VERIFICATION_DAYS = 120;

    private static Properties aimProperties = null;

    // A routine to check whether a given refund failure response code will cause the
    // ccRefund service to attempt to void the refund's associated authorization transaction
    private static boolean isVoidableResponse(String responseCode) {
        return
            VOIDABLE_RESPONSES_NO_TIME_LIMIT.contains(responseCode) || VOIDABLE_RESPONSES_TIME_LIMIT.contains(responseCode);
    }

    public static Map<String, Object> ccAuth(DispatchContext ctx, AIMPaymentServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put(x.transType, x.AUTH_ONLY);
        buildAuthTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingValidationFailedInvalidValues, locale));
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        //now we need to process the result
        processAuthTransResult(request, reply, results);
        return results;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, AIMPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne(x.CreditCard, orderPaymentPreference, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentUnableToGetCCInfo, locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotCapture, locale));
        }
        context.put(x.creditCard, creditCard);
        context.put(x.authTransaction, authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        request.put(x.x_Invoice_Num, x.Order + orderPaymentPreference.getString(x.orderId));
        // PRIOR_AUTH_CAPTURE is the right one to use, since we already have an authorization from the authTransaction.
        // CAPTURE_ONLY is a "force" transaction to be used if there is no prior authorization
        props.put(x.transType, x.PRIOR_AUTH_CAPTURE);
        props.put(x.cardtype, creditCard.get(x.cardType));
        buildCaptureTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingValidationFailedInvalidValues, locale));
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        processCaptureTransResult(request, reply, results);
        // if there is no captureRefNum, then the capture failed
        if (results.get(x.captureRefNum) == null) {
            return ServiceUtil.returnError((String) results.get(x.captureMessage));
        }
        return results;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, AIMPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne(x.CreditCard, orderPaymentPreference, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentUnableToGetCCInfo, locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }
        context.put(x.creditCard, creditCard);
        context.put(x.authTransaction, authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put(x.transType, x.CREDIT);
        props.put(x.cardtype, creditCard.get(x.cardType));
        buildRefundTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingValidationFailedInvalidValues, locale));
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        results.putAll(processRefundTransResult(request, reply));
        boolean refundResult = (Boolean) results.get(x.refundResult);
        String refundFlag = (String) results.get(x.refundFlag);
        // Since the refund failed, we are going to void the previous authorization against
        // which ccRefunds attempted to issue the refund.  This happens because Authorize.NET requires
        // that settled transactions need to be voided the same day.  unfortunately they provide no method for
        // determining what transactions can be voided and what can be refunded, so we'll have to try it with timestamps
        if (!refundResult && isVoidableResponse(refundFlag)) {
            boolean canDoVoid = false;
            if (VOIDABLE_RESPONSES_TIME_LIMIT.contains(refundFlag)) {
                // We are calculating the timestamp that is at the beginning of a time limit,
                // since we can safely assume that, within this time limit, an unsettled transaction
                // can still be considered valid
                Calendar startCalendar = UtilDateTime.toCalendar(UtilDateTime.nowTimestamp());
                startCalendar.add(Calendar.DATE, -TIME_LIMIT_VERIFICATION_DAYS);
                Timestamp startTimestamp = new java.sql.Timestamp(startCalendar.getTime().getTime());
                Timestamp authTimestamp = authTransaction.getTimestamp(x.transactionDate);
                if (startTimestamp.before(authTimestamp)) {
                    canDoVoid = true;
                }
            } else {
                // Since there's no time limit to check, the voiding of the transaction will go
                // through as usual
                canDoVoid = true;
            }
            if (canDoVoid) {
                Debug.logWarning(x.Refund_was_unsuccessful_will_now_attempt_a_VOID_transaction, MODULE);
                BigDecimal authAmountObj = authTransaction.getBigDecimal(x.amount);
                BigDecimal refundAmountObj = (BigDecimal) context.get(x.refundAmount);
                BigDecimal authAmount = authAmountObj != null ? authAmountObj : BigDecimal.ZERO;
                BigDecimal refundAmount = refundAmountObj != null ? refundAmountObj : BigDecimal.ZERO;
                if (authAmount.compareTo(refundAmount) == 0) {
                    reply = voidTransaction(authTransaction, context, delegator);
                    if (ServiceUtil.isError(reply)) {
                        return reply;
                    }
                    results = ServiceUtil.returnSuccess();
                    results.putAll(processRefundTransResult(request, reply));
                    return results;
                } else {
                    // TODO: Modify the code to (a) do a void of the whole transaction, and (b)
                    // create a new auth-capture of the difference.
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.AccountingAuthorizeNetCannotPerformVoidTransaction,
                            UtilMisc.toMap(x.authAmount, authAmount, x.refundAmount, refundAmount), locale));
                }
            }
        }
        return results;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, AIMPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }
        Map<String, Object> reply = voidTransaction(authTransaction, context, delegator);
        if (ServiceUtil.isError(reply)) {
            return reply;
        }
        Map<String, Object> results = ServiceUtil.returnSuccess();
        context.put(x.x_Amount, ((BigDecimal) context.get(x.releaseAmount)).toPlainString()); // hack for releaseAmount
        results.putAll(processReleaseTransResult(context, reply));
        return results;
    }

    private static Map<String, Object> voidTransaction(GenericValue authTransaction, AIMPaymentServicesContext context, Delegator delegator) {
        Locale locale = (Locale) context.get(x.locale);
        context.put(x.authTransaction, authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildEmailSettings(context, props, request);
        props.put(x.transType, x.VOID);
        buildVoidTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingValidationFailedInvalidValues, locale));
            return results;
        }
        return processCard(request, props, locale);
    }

    public static Map<String, Object> ccCredit(DispatchContext ctx, AIMPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> results = new HashMap<>();
        results.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingAuthorizeNetccCreditUnsupported, locale));
        return results;
    }

    public static Map<String, Object> ccAuthCapture(DispatchContext ctx, AIMPaymentServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put(x.transType, x.AUTH_CAPTURE);
        buildAuthTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RESOURCE, x.AccountingValidationFailedInvalidValues, locale));
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        //now we need to process the result
        processAuthCaptureTransResult(request, reply, results);
        // if there is no captureRefNum, then the capture failed
        if (results.get(x.captureRefNum) == null) {
            return ServiceUtil.returnError((String) results.get(x.captureMessage));
        }
        return results;
    }

    private static Map<String, Object> processCard(Map<String, Object> request, Properties props, Locale locale) {
        Map<String, Object> result = new HashMap<>();
        String url = props.getProperty(x.url);
        if (UtilValidate.isEmpty(url)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingAuthorizeNetTransactionUrlNotFound, locale));
        }
        if (isTestMode()) {
            Debug.logInfo(x.TEST_Authorize_net_using_url + url + x.str_4ff447b8, MODULE);
            Debug.logInfo(x.TEST_Authorize_net_request_string + request.toString(), MODULE);
            Debug.logInfo(x.TEST_Authorize_net_properties_string + props.toString(), MODULE);
        }

        // card present has a different layout from standard AIM; this determines how to parse the response
        int apiType = UtilValidate.isEmpty(props.get(x.cpMarketType)) ? AuthorizeResponse.AIM_RESPONSE : AuthorizeResponse.CP_RESPONSE;

        try {
            HttpClient httpClient = new HttpClient(url, request);
            String certificateAlias = props.getProperty(x.certificateAlias);
            httpClient.setClientCertificateAlias(certificateAlias);
            String httpResponse = httpClient.post();
            Debug.logInfo(x.transaction_response + httpResponse, MODULE);
            AuthorizeResponse ar = new AuthorizeResponse(httpResponse, apiType);
            if (ar.isApproved()) {
                result.put(x.authResult, Boolean.TRUE);
            } else if (x.VOID.equals(props.get(x.transType)) && x._16.equals(ar.getReasonCode())) {
                // When the transaction is already expired in Authorize.net, then the response is an error message with reason code 16
                // (i.e. "The transaction cannot be found");
                // in this case we proceed without generating an error in order to void/cancel the transaction record in OFBiz as well.
                // This else if block takes care of the expired transaction.
                result.put(x.authResult, Boolean.TRUE);
            } else {
                result.put(x.authResult, Boolean.FALSE);
                if (Debug.infoOn()) {
                    Debug.logInfo(x.transactionId_d5d0bf74 + ar.getTransactionId(), MODULE);
                    Debug.logInfo(x.responseCode_e85821b4 + ar.getResponseCode(), MODULE);
                    Debug.logInfo(x.responseReason + ar.getReasonCode(), MODULE);
                    Debug.logInfo(x.reasonText + ar.getReasonText(), MODULE);
                }
            }
            result.put(x.httpResponse, httpResponse);
            result.put(x.authorizeResponse, ar);
        } catch (HttpClientException e) {
            Debug.logInfo(e, x.Could_not_complete_Authorize_Net_transaction + e.toString(), MODULE);
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    private static boolean isTestMode() {
        return x._true.equalsIgnoreCase((String) aimProperties.get(x.testReq));
    }

    private static Properties buildAIMProperties(AIMPaymentServicesContext context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configStr = (String) context.get(x.paymentConfig);
        if (configStr == null) {
            configStr = x.payment_properties;
        }
        GenericValue cc = (GenericValue) context.get(x.creditCard);
        String url = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.transactionUrl, configStr,
                x.payment_authorizedotnet_url);
        String certificateAlias = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.certificateAlias, configStr,
                x.payment_authorizedotnet_certificateAlias);
        String ver = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.apiVersion, configStr,
                x.payment_authorizedotnet_version);
        String delimited = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.delimitedData, configStr,
                x.payment_authorizedotnet_delimited);
        String delimiter = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.delimiterChar, configStr,
                x.payment_authorizedotnet_delimiter);
        String cpVersion = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.cpVersion, configStr,
                x.payment_authorizedotnet_cpVersion);
        String cpMarketType = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.cpMarketType, configStr,
                x.payment_authorizedotnet_cpMarketType);
        String cpDeviceType = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.cpDeviceType, configStr,
                x.payment_authorizedotnet_cpDeviceType);
        String method = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.method, configStr, x.payment_authorizedotnet_method);
        String emailCustomer = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.emailCustomer, configStr,
                x.payment_authorizedotnet_emailcustomer);
        String emailMerchant = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.emailMerchant, configStr,
                x.payment_authorizedotnet_emailmerchant);
        String testReq = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.testMode, configStr, x.payment_authorizedotnet_test);
        String relay = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.relayResponse, configStr, x.payment_authorizedotnet_relay);
        String tranKey = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.tranKey, configStr, x.payment_authorizedotnet_trankey);
        String login = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.userId, configStr, x.payment_authorizedotnet_login);
        String password = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.pwd, configStr, x.payment_authorizedotnet_password);
        String transDescription = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.transDescription, configStr,
                x.payment_authorizedotnet_transdescription);
        String duplicateWindow = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.duplicateWindow, configStr,
                x.payment_authorizedotnet_duplicateWindow);
        if (UtilValidate.isEmpty(ver)) {
            ver = x._3_0;
        }
        if (UtilValidate.isEmpty(login)) {
            Debug.logInfo(x.the_login_property_in + configStr + x.is_not_configured, MODULE);
        }
        if (UtilValidate.isEmpty(password) && !(x._3_1.equals(ver))) {
            Debug.logInfo(x.The_password_property_in + configStr + x.is_not_configured, MODULE);
        }
        if (x._3_1.equals(ver)) {
            if (UtilValidate.isEmpty(tranKey)) {
                Debug.logInfo(x.Trankey_property_required_for_version_3_1_reverting_to_3_0, MODULE);
                ver = x._3_0;
            }
        }
        if (UtilValidate.isNotEmpty(cpMarketType) && UtilValidate.isEmpty(cpVersion)) {
            cpVersion = x._1_0;
        }

        Properties props = new Properties();
        props.put(x.url, url);
        props.put(x.certificateAlias, certificateAlias);
        props.put(x.ver, ver);
        props.put(x.delimited, delimited);
        props.put(x.delimiter, delimiter);
        props.put(x.method, method);
        props.put(x.cpVersion, cpVersion);
        props.put(x.cpMarketType, cpMarketType);
        props.put(x.cpDeviceType, cpDeviceType);
        props.put(x.emailCustomer, emailCustomer);
        props.put(x.emailMerchant, emailMerchant);
        props.put(x.testReq, testReq);
        props.put(x.relay, relay);
        props.put(x.transDescription, transDescription);
        props.put(x.login, login);
        props.put(x.password, password);
        props.put(x.trankey, tranKey);
        props.put(x.duplicateWindow, duplicateWindow);
        if (cc != null) {
            props.put(x.cardtype, cc.get(x.cardType));
        }
        if (aimProperties == null) {
            aimProperties = props;
        }
        if (isTestMode()) {
            Debug.logInfo(x.Created_Authorize_Net_properties_file + props.toString(), MODULE);
        }
        return props;
    }

    private static void buildMerchantInfo(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        aimRequest.put(x.x_Login, props.getProperty(x.login));
        String trankey = props.getProperty(x.trankey);
        if (UtilValidate.isNotEmpty(trankey)) {
            aimRequest.put(x.x_Tran_Key, props.getProperty(x.trankey));
        } else {
            // only send password if no tran key
            aimRequest.put(x.x_Password, props.getProperty(x.password));
        }
        // api version (non Card Present)
        String apiVersion = props.getProperty(x.ver);
        if (UtilValidate.isNotEmpty(apiVersion)) {
            aimRequest.put(x.x_Version, props.getProperty(x.ver));
        }
        // CP version
        String cpVersion = props.getProperty(x.cpver);
        if (UtilValidate.isNotEmpty(cpVersion)) {
            aimRequest.put(x.x_cpversion, cpVersion);
        }

        // Check duplicateWindow time frame. If same transaction happens in the predefined time frame then return error.
        String duplicateWindow = props.getProperty(x.duplicateWindow);
        if (UtilValidate.isNotEmpty(duplicateWindow)) {
            aimRequest.put(x.x_duplicate_window, props.getProperty(x.duplicateWindow));
        }
        // CP market type
        String cpMarketType = props.getProperty(x.cpMarketType);
        if (UtilValidate.isNotEmpty(cpMarketType)) {
            aimRequest.put(x.x_market_type, cpMarketType);
            // CP test mode
            if (x._true.equalsIgnoreCase(props.getProperty(x.testReq))) {
                aimRequest.put(x.x_test_request, props.getProperty(x.testReq));
            }
        }
        // CP device typ
        String cpDeviceType = props.getProperty(x.cpDeviceType);
        if (UtilValidate.isNotEmpty(cpDeviceType)) {
            aimRequest.put(x.x_device_type, cpDeviceType);
        }
    }

    private static void buildGatewayResponeConfig(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        if (aimRequest.get(x.x_market_type) != null) {
            // card present transaction
            aimRequest.put(x.x_response_format, x._true.equalsIgnoreCase(props.getProperty(x.delimited)) ? x._1 : x._0);
        } else {
            aimRequest.put(x.x_Delim_Data, props.getProperty(x.delimited));
        }
        aimRequest.put(x.x_Delim_Char, props.getProperty(x.delimiter));
    }

    private static void buildCustomerBillingInfo(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        try {
            // this would be used in the case of a capture, where one of the parameters is an OrderPaymentPreference
            if (params.get(x.orderPaymentPreference) != null) {
                GenericValue opp = (GenericValue) params.get(x.orderPaymentPreference);
                if (x.CREDIT_CARD.equals(opp.getString(x.paymentMethodTypeId))) {
                    // sometimes the ccAuthCapture interface is used, in which case the creditCard is passed directly
                    GenericValue creditCard = (GenericValue) params.get(x.creditCard);
                    if (creditCard == null || !(opp.get(x.paymentMethodId).equals(creditCard.get(x.paymentMethodId)))) {
                        creditCard = opp.getRelatedOne(x.CreditCard, false);
                    }
                    aimRequest.put(x.x_First_Name, UtilFormatOut.checkNull(creditCard.getString(x.firstNameOnCard)));
                    aimRequest.put(x.x_Last_Name, UtilFormatOut.checkNull(creditCard.getString(x.lastNameOnCard)));
                    aimRequest.put(x.x_Company, UtilFormatOut.checkNull(creditCard.getString(x.companyNameOnCard)));
                    if (UtilValidate.isNotEmpty(creditCard.getString(x.contactMechId))) {
                        GenericValue address = creditCard.getRelatedOne(x.PostalAddress, false);
                        if (address != null) {
                            aimRequest.put(x.x_Address, UtilFormatOut.checkNull(address.getString(x.address1)));
                            aimRequest.put(x.x_City, UtilFormatOut.checkNull(address.getString(x.city)));
                            aimRequest.put(x.x_State, UtilFormatOut.checkNull(address.getString(x.stateProvinceGeoId)));
                            aimRequest.put(x.x_Zip, UtilFormatOut.checkNull(address.getString(x.postalCode)));
                            aimRequest.put(x.x_Country, UtilFormatOut.checkNull(address.getString(x.countryGeoId)));
                        }
                    }
                } else {
                    Debug.logWarning(x.Payment_preference + opp + x.is_not_a_credit_card, MODULE);
                }
            } else {
                // this would be the case for an authorization
                GenericValue cp = (GenericValue) params.get(x.billToParty);
                GenericValue ba = (GenericValue) params.get(x.billingAddress);
                aimRequest.put(x.x_First_Name, UtilFormatOut.checkNull(cp.getString(x.firstName)));
                aimRequest.put(x.x_Last_Name, UtilFormatOut.checkNull(cp.getString(x.lastName)));
                aimRequest.put(x.x_Address, UtilFormatOut.checkNull(ba.getString(x.address1)));
                aimRequest.put(x.x_City, UtilFormatOut.checkNull(ba.getString(x.city)));
                aimRequest.put(x.x_State, UtilFormatOut.checkNull(ba.getString(x.stateProvinceGeoId)));
                aimRequest.put(x.x_Zip, UtilFormatOut.checkNull(ba.getString(x.postalCode)));
                aimRequest.put(x.x_Country, UtilFormatOut.checkNull(ba.getString(x.countryGeoId)));
            }
            return;
        } catch (GenericEntityException ex) {
            Debug.logError(x.Cannot_build_customer_information_for + params + x.due_to_error + ex.getMessage(), MODULE);
            return;
        }
    }

    private static void buildEmailSettings(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        GenericValue ea = (GenericValue) params.get(x.billToEmail);
        aimRequest.put(x.x_Email_Customer, props.getProperty(x.emailCustomer));
        aimRequest.put(x.x_Email_Merchant, props.getProperty(x.emailMerchant));
        if (ea != null) {
            aimRequest.put(x.x_Email, UtilFormatOut.checkNull(ea.getString(x.infoString)));
        }
    }

    private static void buildInvoiceInfo(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        String description = UtilFormatOut.checkNull(props.getProperty(x.transDescription));
        String orderId = UtilFormatOut.checkNull((String) params.get(x.orderId));
        if (UtilValidate.isEmpty(orderId)) {
            GenericValue orderPaymentPreference = (GenericValue) params.get(x.orderPaymentPreference);
            if (orderPaymentPreference != null) {
                orderId = (String) orderPaymentPreference.get(x.orderId);
            }
        }
        aimRequest.put(x.x_Invoice_Num, x.Order + orderId);
        aimRequest.put(x.x_Description, description);
    }

    private static void buildAuthTransaction(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        GenericValue cc = (GenericValue) params.get(x.creditCard);
        String currency = (String) params.get(x.currency);
        String amount = ((BigDecimal) params.get(x.processAmount)).toString();
        String number = UtilFormatOut.checkNull(cc.getString(x.cardNumber));
        String expDate = UtilFormatOut.checkNull(cc.getString(x.expireDate));
        String cardSecurityCode = (String) params.get(x.cardSecurityCode);
        aimRequest.put(x.x_Amount, amount);
        aimRequest.put(x.x_Currency_Code, currency);
        aimRequest.put(x.x_Method, props.getProperty(x.method));
        aimRequest.put(x.x_Type, props.getProperty(x.transType));
        aimRequest.put(x.x_Card_Num, number);
        aimRequest.put(x.x_Exp_Date, expDate);
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            aimRequest.put(x.x_card_code, cardSecurityCode);
        }
        if (aimRequest.get(x.x_market_type) != null) {
            aimRequest.put(x.x_card_type, getCardType(UtilFormatOut.checkNull(cc.getString(x.cardType))));
        }
    }

    private static void buildCaptureTransaction(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        GenericValue at = (GenericValue) params.get(x.authTransaction);
        GenericValue cc = (GenericValue) params.get(x.creditCard);
        String currency = (String) params.get(x.currency);
        String amount = ((BigDecimal) params.get(x.captureAmount)).toString();
        String number = UtilFormatOut.checkNull(cc.getString(x.cardNumber));
        String expDate = UtilFormatOut.checkNull(cc.getString(x.expireDate));
        aimRequest.put(x.x_Amount, amount);
        aimRequest.put(x.x_Currency_Code, currency);
        aimRequest.put(x.x_Method, props.getProperty(x.method));
        aimRequest.put(x.x_Type, props.getProperty(x.transType));
        aimRequest.put(x.x_Card_Num, number);
        aimRequest.put(x.x_Exp_Date, expDate);
        aimRequest.put(x.x_Trans_ID, at.get(x.referenceNum));
        aimRequest.put(x.x_ref_trans_id, at.get(x.referenceNum));
        aimRequest.put(x.x_Auth_Code, at.get(x.gatewayCode));
        if (aimRequest.get(x.x_market_type) != null) {
            aimRequest.put(x.x_card_type, getCardType(UtilFormatOut.checkNull(cc.getString(x.cardType))));
        }
    }

    private static void buildRefundTransaction(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        GenericValue at = (GenericValue) params.get(x.authTransaction);
        GenericValue cc = (GenericValue) params.get(x.creditCard);
        String currency = (String) params.get(x.currency);
        String amount = ((BigDecimal) params.get(x.refundAmount)).toString();
        String number = UtilFormatOut.checkNull(cc.getString(x.cardNumber));
        String expDate = UtilFormatOut.checkNull(cc.getString(x.expireDate));
        aimRequest.put(x.x_Amount, amount);
        aimRequest.put(x.x_Currency_Code, currency);
        aimRequest.put(x.x_Method, props.getProperty(x.method));
        aimRequest.put(x.x_Type, props.getProperty(x.transType));
        aimRequest.put(x.x_Card_Num, number);
        aimRequest.put(x.x_Exp_Date, expDate);
        aimRequest.put(x.x_Trans_ID, at.get(x.referenceNum));
        aimRequest.put(x.x_Auth_Code, at.get(x.gatewayCode));
        aimRequest.put(x.x_ref_trans_id, at.get(x.referenceNum));
        if (aimRequest.get(x.x_market_type) != null) {
            aimRequest.put(x.x_card_type, getCardType(UtilFormatOut.checkNull(cc.getString(x.cardType))));
        }
        Debug.logInfo(x.buildCaptureTransaction + at.toString(), MODULE);
    }

    private static void buildVoidTransaction(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        GenericValue at = (GenericValue) params.get(x.authTransaction);
        String currency = (String) params.get(x.currency);
        aimRequest.put(x.x_Currency_Code, currency);
        aimRequest.put(x.x_Method, props.getProperty(x.method));
        aimRequest.put(x.x_Type, props.getProperty(x.transType));
        aimRequest.put(x.x_ref_trans_id, at.get(x.referenceNum));
        aimRequest.put(x.x_Trans_ID, at.get(x.referenceNum));
        aimRequest.put(x.x_Auth_Code, at.get(x.gatewayCode));
        Debug.logInfo(x.buildVoidTransaction + at.toString(), MODULE);
    }

    private static Map<String, Object> validateRequest(Map<String, Object> params, Properties props, Map<String, Object> aimRequest) {
        Map<String, Object> result = new HashMap<>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    private static void processAuthTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get(x.authorizeResponse);
        try {
            Boolean authResult = (Boolean) reply.get(x.authResult);
            results.put(x.authResult, authResult);
            results.put(x.authFlag, ar.getReasonCode());
            results.put(x.authMessage, ar.getReasonText());
            if (authResult) { //passed
                results.put(x.authCode, ar.getAuthorizationCode());
                results.put(x.authRefNum, ar.getTransactionId());
                results.put(x.cvCode, ar.getCvResult());
                results.put(x.avsCode, ar.getAvsResult());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put(x.processAmount, getXAmount(request));
                } else {
                    results.put(x.processAmount, ar.getAmount());
                }
            } else {
                results.put(x.authCode, ar.getResponseCode());
                results.put(x.processAmount, BigDecimal.ZERO);
                results.put(x.authRefNum, AuthorizeResponse.ERROR);
            }
        } catch (Exception ex) {
            Debug.logError(ex, MODULE);
            results.put(x.authCode, ar.getResponseCode());
            results.put(x.processAmount, BigDecimal.ZERO);
            results.put(x.authRefNum, AuthorizeResponse.ERROR);
        }
        Debug.logInfo(x.processAuthTransResult + results.toString(), MODULE);
    }

    private static void processCaptureTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get(x.authorizeResponse);
        try {
            Boolean captureResult = (Boolean) reply.get(x.authResult);
            results.put(x.captureResult, captureResult);
            results.put(x.captureFlag, ar.getReasonCode());
            results.put(x.captureMessage, ar.getReasonText());
            results.put(x.captureRefNum, ar.getTransactionId());
            if (captureResult) { //passed
                results.put(x.captureCode, ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put(x.captureAmount, getXAmount(request));
                } else {
                    results.put(x.captureAmount, ar.getAmount());
                }
            } else {
                results.put(x.captureAmount, BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, MODULE);
            results.put(x.captureAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.captureRefNum_b3cf1435 + results.toString(), MODULE);
    }

    private static Map<String, Object> processRefundTransResult(Map<String, Object> request, Map<String, Object> reply) {
        Map<String, Object> results = new HashMap<>();
        AuthorizeResponse ar = (AuthorizeResponse) reply.get(x.authorizeResponse);
        try {
            Boolean captureResult = (Boolean) reply.get(x.authResult);
            results.put(x.refundResult, captureResult);
            results.put(x.refundFlag, ar.getReasonCode());
            results.put(x.refundMessage, ar.getReasonText());
            results.put(x.refundRefNum, ar.getTransactionId());
            if (captureResult) { //passed
                results.put(x.refundCode, ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put(x.refundAmount, getXAmount(request));
                } else {
                    results.put(x.refundAmount, ar.getAmount());
                }
            } else {
                results.put(x.refundAmount, BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, MODULE);
            results.put(x.refundAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.processRefundTransResult + results.toString(), MODULE);
        return results;
    }

    private static Map<String, Object> processReleaseTransResult(Map<String, Object> request, Map<String, Object> reply) {
        Map<String, Object> results = new HashMap<>();
        AuthorizeResponse ar = (AuthorizeResponse) reply.get(x.authorizeResponse);
        try {
            Boolean captureResult = (Boolean) reply.get(x.authResult);
            results.put(x.releaseResult, captureResult);
            results.put(x.releaseFlag, ar.getReasonCode());
            results.put(x.releaseMessage, ar.getReasonText());
            results.put(x.releaseRefNum, ar.getTransactionId());
            if (captureResult) { //passed
                results.put(x.releaseCode, ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put(x.releaseAmount, getXAmount(request));
                } else {
                    results.put(x.releaseAmount, ar.getAmount());
                }
            } else {
                results.put(x.releaseAmount, BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, MODULE);
            results.put(x.releaseAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.processReleaseTransResult + results.toString(), MODULE);
        return results;
    }

    private static void processAuthCaptureTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get(x.authorizeResponse);
        try {
            Boolean authResult = (Boolean) reply.get(x.authResult);
            results.put(x.authResult, authResult);
            results.put(x.authFlag, ar.getReasonCode());
            results.put(x.authMessage, ar.getReasonText());
            results.put(x.captureResult, authResult);
            results.put(x.captureFlag, ar.getReasonCode());
            results.put(x.captureMessage, ar.getReasonText());
            results.put(x.captureRefNum, ar.getTransactionId());
            if (authResult) { //passed
                results.put(x.authCode, ar.getAuthorizationCode());
                results.put(x.authRefNum, ar.getTransactionId());
                results.put(x.cvCode, ar.getCvResult());
                results.put(x.avsCode, ar.getAvsResult());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put(x.processAmount, getXAmount(request));
                } else {
                    results.put(x.processAmount, ar.getAmount());
                }
            } else {
                results.put(x.authCode, ar.getResponseCode());
                results.put(x.processAmount, BigDecimal.ZERO);
                results.put(x.authRefNum, AuthorizeResponse.ERROR);
            }
        } catch (Exception ex) {
            Debug.logError(ex, MODULE);
            results.put(x.authCode, ar.getResponseCode());
            results.put(x.processAmount, BigDecimal.ZERO);
            results.put(x.authRefNum, AuthorizeResponse.ERROR);
        }
        Debug.logInfo(x.processAuthTransResult + results.toString(), MODULE);
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                PaymentGatewayAuthorizeNetDao paymentGatewayAuthorizeNetDao = DaoRegistry.getDao(delegator, x.PaymentGatewayAuthorizeNet,
                        PaymentGatewayAuthorizeNetDao.class);
                PaymentGatewayAuthorizeNetEntity payflowPro = paymentGatewayAuthorizeNetDao.get(paymentGatewayConfigId).orElse(null);
                if (payflowPro != null) {
                    Object payflowProField = Beans.getPropValue(payflowPro, paymentGatewayConfigParameterName, true);
                    if (payflowProField != null) {
                        returnValue = payflowProField.toString().trim();
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, parameterName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }
    private static String getCardType(String cardType) {
        if ((x.CCT_VISA.equalsIgnoreCase(cardType))) return x.V;
        if ((x.CCT_MASTERCARD.equalsIgnoreCase(cardType))) return x.M;
        if (((x.CCT_AMERICANEXPRESS.equalsIgnoreCase(cardType)) || (x.CCT_AMEX.equalsIgnoreCase(cardType)))) return x.A;
        if ((x.CCT_DISCOVER.equalsIgnoreCase(cardType))) return x.D_50c9e8d5;
        if ((x.CCT_JCB.equalsIgnoreCase(cardType))) return x.J;
        if (((x.CCT_DINERSCLUB.equalsIgnoreCase(cardType)))) return x.C;
        return x.emptyString;
    }
    private static BigDecimal getXAmount(Map<String, Object> request) {
        BigDecimal amt = BigDecimal.ZERO;
        if (request.get(x.x_Amount) != null) {
            try {
                BigDecimal amount = new BigDecimal((String) request.get(x.x_Amount));
                amt = amount;
            } catch (NumberFormatException e) {
                Debug.logWarning(e, e.getMessage(), MODULE);
            }
        }
        return amt;
    }
}

