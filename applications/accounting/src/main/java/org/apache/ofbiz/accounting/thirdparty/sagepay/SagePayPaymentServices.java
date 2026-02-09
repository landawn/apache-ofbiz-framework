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

package org.apache.ofbiz.accounting.thirdparty.sagepay;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.SagePayPaymentServicesContext;
public class SagePayPaymentServices {

    private static final String MODULE = SagePayPaymentServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";

    private static Map<String, String> buildCustomerBillingInfo(SagePayPaymentServicesContext context) {
        Debug.logInfo("SagePay - Entered buildCustomerBillingInfo", MODULE);
        Debug.logInfo("SagePay buildCustomerBillingInfo context : " + context, MODULE);

        Map<String, String> billingInfo = new HashMap<>();

        String orderId = null;
        BigDecimal processAmount = null;
        String currency = null;
        String cardNumber = null;
        String cardType = null;
        String nameOnCard = null;
        String expireDate = null;
        String securityCode = null;
        String postalCode = null;
        String address = null;

        try {

            GenericValue opp = (GenericValue) context.get(x.orderPaymentPreference);
            if (opp != null) {
                if ("CREDIT_CARD".equals(opp.getString(x.paymentMethodTypeId))) {

                    GenericValue creditCard = (GenericValue) context.get(x.creditCard);
                    if (creditCard == null || !(opp.get(x.paymentMethodId).equals(creditCard.get(x.paymentMethodId)))) {
                        creditCard = opp.getRelatedOne(x.CreditCard, false);
                    }

                    securityCode = opp.getString(x.securityCode);

                    //getting billing address
                    GenericValue billingAddress = (GenericValue) context.get(x.billingAddress);
                    postalCode = billingAddress.getString(x.postalCode);
                    String address2 = billingAddress.getString(x.address2);
                    if (address2 == null) {
                        address2 = "";
                    }
                    address = billingAddress.getString(x.address1) + " " + address2;

                    //getting card details
                    cardNumber = creditCard.getString(x.cardNumber);
                    String firstName = creditCard.getString(x.firstNameOnCard);
                    String middleName = creditCard.getString(x.middleNameOnCard);
                    String lastName = creditCard.getString(x.lastNameOnCard);
                    if (middleName == null) {
                        middleName = "";
                    }
                    nameOnCard = firstName + " " + middleName + " " + lastName;
                    cardType = creditCard.getString(x.cardType);
                    if (cardType != null) {
                        if ("CCT_MASTERCARD".equals(cardType)) {
                            cardType = "MC";
                        }
                        if ("CCT_VISAELECTRON".equals(cardType)) {
                            cardType = "UKE";
                        }
                        if ("CCT_DINERSCLUB".equals(cardType)) {
                            cardType = "DC";
                        }
                        if ("CCT_SWITCH".equals(cardType)) {
                            cardType = "MAESTRO";
                        }
                    }
                    expireDate = creditCard.getString(x.expireDate);
                    String month = expireDate.substring(0, 2);
                    String year = expireDate.substring(5);
                    expireDate = month + year;

                    //getting order details
                    orderId = UtilFormatOut.checkNull((String) context.get(x.orderId));
                    processAmount = (BigDecimal) context.get(x.processAmount);
                    currency = (String) context.get(x.currency);

                } else {
                    Debug.logWarning("Payment preference " + opp + " is not a credit card", MODULE);
                }
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Cannot build customer information for " + context + " due to error: " + ex.getMessage(), MODULE);
            return null;
        }

        billingInfo.put("orderId", orderId);
        if (processAmount != null) {
            billingInfo.put("amount", processAmount.toString());
        } else {
            billingInfo.put("amount", "");
        }
        billingInfo.put("currency", currency);
        billingInfo.put("description", orderId);
        billingInfo.put("cardNumber", cardNumber);
        billingInfo.put("cardHolder", nameOnCard);
        billingInfo.put("expiryDate", expireDate);
        billingInfo.put("cardType", cardType);
        billingInfo.put("cv2", securityCode);
        billingInfo.put("billingPostCode", postalCode);
        billingInfo.put("billingAddress", address);

        Debug.logInfo("SagePay billingInfo : " + billingInfo, MODULE);
        Debug.logInfo("SagePay - Exiting buildCustomerBillingInfo", MODULE);

        return billingInfo;
    }

    public static Map<String, Object> ccAuth(DispatchContext dctx, SagePayPaymentServicesContext context) {
        Debug.logInfo("SagePay - Entered ccAuth", MODULE);
        Debug.logInfo("SagePay ccAuth context : " + context, MODULE);
        Map<String, Object> response = null;
        String orderId = (String) context.get(x.orderId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        if (orderPaymentPreference == null) {
            response = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayOrderPaymenPreferenceIsNull",
                    UtilMisc.toMap("orderId", orderId, "orderPaymentPreference", null), locale));
        } else {
            response = processCardAuthorisationPayment(dctx, context);
        }
        Debug.logInfo("SagePay ccAuth response : " + response, MODULE);
        Debug.logInfo("SagePay - Exiting ccAuth", MODULE);
        return response;
    }
    private static Map<String, Object> processCardAuthorisationPayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, String> billingInfo = buildCustomerBillingInfo(context);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        try {
            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentAuthentication",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", billingInfo.get("orderId"),
                            "cardHolder", billingInfo.get("cardHolder"),
                            "cardNumber", billingInfo.get("cardNumber"),
                            "expiryDate", billingInfo.get("expiryDate"),
                            "cardType", billingInfo.get("cardType"),
                            "cv2", billingInfo.get("cv2"),
                            "description", billingInfo.get("description"),
                            "amount", billingInfo.get("amount"),
                            "currency", billingInfo.get("currency"),
                            "billingAddress", billingInfo.get("billingAddress"),
                            "billingPostCode", billingInfo.get("billingPostCode")));

            Debug.logInfo("SagePay - SagePayPaymentAuthentication result : " + paymentResult, MODULE);

            String transactionType = (String) paymentResult.get("transactionType");
            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            String vpsTxId = (String) paymentResult.get("vpsTxId");
            String securityKey = (String) paymentResult.get("securityKey");
            String txAuthNo = (String) paymentResult.get("txAuthNo");
            String vendorTxCode = (String) paymentResult.get("vendorTxCode");
            String amount = (String) paymentResult.get("amount");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay - Payment authorized for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.TRUE, txAuthNo, securityKey, new BigDecimal(amount), vpsTxId,
                        vendorTxCode, statusDetail);
                if ("PAYMENT".equals(transactionType)) {
                    Map<String, Object> captureResult = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthNo, securityKey,
                            new BigDecimal(amount), vpsTxId, vendorTxCode, statusDetail);
                    result.putAll(captureResult);
                }
            } else if (status != null && "INVALID".equals(status)) {
                Debug.logInfo("SagePay - Invalid authorisation request for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "INVALID",
                        vendorTxCode, statusDetail);
            } else if (status != null && "MALFORMED".equals(status)) {
                Debug.logInfo("SagePay - Malformed authorisation request for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "MALFORMED",
                        vendorTxCode, statusDetail);
            } else if (status != null && "NOTAUTHED".equals(status)) {
                Debug.logInfo("SagePay - NotAuthed authorisation request for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, BigDecimal.ZERO, vpsTxId, vendorTxCode,
                        statusDetail);
            } else if (status != null && "REJECTED".equals(status)) {
                Debug.logInfo("SagePay - Rejected authorisation request for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, new BigDecimal(amount), vpsTxId,
                        vendorTxCode, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "ERROR",
                        vendorTxCode, statusDetail);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentAuthentication", MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentAuthorisationException",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo("SagePay - Entered ccCapture", MODULE);
        Debug.logInfo("SagePay ccCapture context : " + context, MODULE);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        context.put(x.authTransaction, authTransaction);
        Map<String, Object> response = processCardCapturePayment(ctx, context);

        Debug.logInfo("SagePay ccCapture response : " + response, MODULE);
        Debug.logInfo("SagePay - Exiting ccCapture", MODULE);

        return response;
    }

    private static Map<String, Object> processCardCapturePayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        GenericValue authTransaction = (GenericValue) context.get(x.authTransaction);
        BigDecimal amount = (BigDecimal) context.get(x.captureAmount);
        String vendorTxCode = (String) authTransaction.get(x.altReference);
        String vpsTxId = (String) authTransaction.get(x.referenceNum);
        String securityKey = (String) authTransaction.get(x.gatewayFlag);
        String txAuthCode = (String) authTransaction.get(x.gatewayCode);

        try {

            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentAuthorisation",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", vendorTxCode,
                            "vpsTxId", vpsTxId,
                            "securityKey", securityKey,
                            "txAuthNo", txAuthCode,
                            "amount", amount.toString()));
            Debug.logInfo("SagePay - SagePayPaymentAuthorisation result : " + paymentResult, MODULE);
            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Released for Order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode,
                        statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.FALSE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode,
                        statusDetail);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentAuthorisation", MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentAuthorisationException",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo("SagePay - Entered ccRefund", MODULE);
        Debug.logInfo("SagePay ccRefund context : " + context, MODULE);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue captureTransaction = PaymentGatewayServices.getCaptureTransaction(orderPaymentPreference);
        if (captureTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund",
                    locale));
        }
        Debug.logInfo("SagePay ccRefund captureTransaction : " + captureTransaction, MODULE);
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne(x.CreditCard, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting CreditCard for OrderPaymentPreference : " + orderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingPaymentUnableToGetCCInfo", locale)
                    + " " + orderPaymentPreference);
        }
        context.put(x.creditCard, creditCard);
        context.put(x.captureTransaction, captureTransaction);

        List<GenericValue> authTransactions = PaymentGatewayServices.getAuthTransactions(orderPaymentPreference);

        EntityCondition authCondition = EntityCondition.makeCondition("paymentServiceTypeEnumId", "PRDS_PAY_AUTH");
        List<GenericValue> authTransactions1 = EntityUtil.filterByCondition(authTransactions, authCondition);

        GenericValue authTransaction = EntityUtil.getFirst(authTransactions1);

        Timestamp authTime = authTransaction.getTimestamp(x.transactionDate);
        Calendar authCal = Calendar.getInstance();
        authCal.setTimeInMillis(authTime.getTime());

        Timestamp nowTime = UtilDateTime.nowTimestamp();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(nowTime.getTime());

        Calendar yesterday = Calendar.getInstance();
        yesterday.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DATE), 23, 59, 59);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Map<String, Object> response = null;

        if (authCal.before(yesterday)) {
            Debug.logInfo("SagePay - Calling Refund for Refund", MODULE);
            response = processCardRefundPayment(ctx, context);
        } else {

            Calendar cal = Calendar.getInstance();
            cal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DATE), 23, 49, 59);

            if (authCal.before(cal)) {
                Debug.logInfo("SagePay - Calling Void for Refund", MODULE);
                response = processCardVoidPayment(ctx, context);
            } else {
                Debug.logInfo("SagePay - Calling Refund for Refund", MODULE);
                response = processCardRefundPayment(ctx, context);
            }
        }

        Debug.logInfo("SagePay ccRefund response : " + response, MODULE);
        return response;
    }

    private static Map<String, Object> processCardRefundPayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        GenericValue captureTransaction = (GenericValue) context.get(x.captureTransaction);
        BigDecimal amount = (BigDecimal) context.get(x.refundAmount);

        String orderId = (String) captureTransaction.get(x.altReference);
        orderId = "R" + orderId;

        try {

            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentRefund",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", orderId,
                            "amount", amount.toString(),
                            "currency", "GBP",
                            "description", orderId,
                            "relatedVPSTxId", captureTransaction.get(x.referenceNum),
                            "relatedVendorTxCode", captureTransaction.get(x.altReference),
                            "relatedSecurityKey", captureTransaction.get(x.gatewayFlag),
                            "relatedTxAuthNo", captureTransaction.get(x.gatewayCode)));
            Debug.logInfo("SagePay - SagePayPaymentRefund result : " + paymentResult, MODULE);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            String vpsTxId = (String) paymentResult.get("vpsTxId");
            String txAuthNo = (String) paymentResult.get("txAuthNo");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Refunded for Order : " + orderId, MODULE);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.TRUE, txAuthNo, amount, vpsTxId, orderId, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + orderId, MODULE);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.FALSE, null, BigDecimal.ZERO, status, orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentRefund", MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentRefundException",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return result;
    }

    private static Map<String, Object> processCardVoidPayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        GenericValue captureTransaction = (GenericValue) context.get(x.captureTransaction);
        BigDecimal amount = (BigDecimal) context.get(x.refundAmount);
        String orderId = (String) captureTransaction.get(x.altReference);

        try {
            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentVoid",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", captureTransaction.get(x.altReference),
                            "vpsTxId", captureTransaction.get(x.referenceNum),
                            "securityKey", captureTransaction.get(x.gatewayFlag),
                            "txAuthNo", captureTransaction.get(x.gatewayCode)));

            Debug.logInfo("SagePay - SagePayPaymentVoid result : " + paymentResult, MODULE);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Voided for Order : " + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.TRUE, amount, "SUCCESS", orderId, statusDetail);
            } else if (status != null && "MALFORMED".equals(status)) {
                Debug.logInfo("SagePay - Malformed void request for order : " + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "MALFORMED", orderId, statusDetail);
            } else if (status != null && "INVALID".equals(status)) {
                Debug.logInfo("SagePay - Invalid void request for order : " + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "INVALID", orderId, statusDetail);
            } else if (status != null && "ERROR".equals(status)) {
                Debug.logInfo("SagePay - Error in void request for order : " + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "ERROR", orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentVoid", MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentVoidException",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo("SagePay - Entered ccRelease", MODULE);
        Debug.logInfo("SagePay ccRelease context : " + context, MODULE);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);

        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease",
                    locale));
        }
        context.put(x.authTransaction, authTransaction);

        Map<String, Object> response = processCardReleasePayment(ctx, context);
        Debug.logInfo("SagePay ccRelease response : " + response, MODULE);
        return response;
    }

    private static Map<String, Object> processCardReleasePayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = ctx.getDispatcher();

        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        BigDecimal amount = (BigDecimal) context.get(x.releaseAmount);

        GenericValue authTransaction = (GenericValue) context.get(x.authTransaction);
        String orderId = (String) authTransaction.get(x.altReference);
        String refNum = (String) authTransaction.get(x.referenceNum);

        try {
            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentRelease",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", orderId,
                            "releaseAmount", amount.toString(),
                            "vpsTxId", refNum,
                            "securityKey", authTransaction.get(x.gatewayFlag),
                            "txAuthNo", authTransaction.get(x.gatewayCode)));

            Debug.logInfo("SagePay - SagePayPaymentRelease result : " + paymentResult, MODULE);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Released for Order : " + orderId, MODULE);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.TRUE, null, amount, refNum, orderId, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + orderId, MODULE);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.FALSE, null, amount, refNum, orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentRelease", MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentReleaseException",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return result;
    }

}
