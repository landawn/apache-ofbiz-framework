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
    private static final String RESOURCE = x.AccountingUiLabels;

    private static Map<String, String> buildCustomerBillingInfo(SagePayPaymentServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_buildCustomerBillingInfo, MODULE);
        Debug.logInfo(x.SagePay_buildCustomerBillingInfo_context + context, MODULE);

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
                if (x.CREDIT_CARD.equals(opp.getString(x.paymentMethodTypeId))) {

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
                        address2 = x.emptyString;
                    }
                    address = billingAddress.getString(x.address1) + x.str_b858cb28 + address2;

                    //getting card details
                    cardNumber = creditCard.getString(x.cardNumber);
                    String firstName = creditCard.getString(x.firstNameOnCard);
                    String middleName = creditCard.getString(x.middleNameOnCard);
                    String lastName = creditCard.getString(x.lastNameOnCard);
                    if (middleName == null) {
                        middleName = x.emptyString;
                    }
                    nameOnCard = firstName + x.str_b858cb28 + middleName + x.str_b858cb28 + lastName;
                    cardType = creditCard.getString(x.cardType);
                    if (cardType != null) {
                        if (x.CCT_MASTERCARD.equals(cardType)) {
                            cardType = x.MC;
                        }
                        if (x.CCT_VISAELECTRON.equals(cardType)) {
                            cardType = x.UKE;
                        }
                        if (x.CCT_DINERSCLUB.equals(cardType)) {
                            cardType = x.DC;
                        }
                        if (x.CCT_SWITCH.equals(cardType)) {
                            cardType = x.MAESTRO;
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
                    Debug.logWarning(x.Payment_preference + opp + x.is_not_a_credit_card, MODULE);
                }
            }
        } catch (GenericEntityException ex) {
            Debug.logError(x.Cannot_build_customer_information_for + context + x.due_to_error + ex.getMessage(), MODULE);
            return null;
        }

        billingInfo.put(x.orderId, orderId);
        if (processAmount != null) {
            billingInfo.put(x.amount, processAmount.toString());
        } else {
            billingInfo.put(x.amount, x.emptyString);
        }
        billingInfo.put(x.currency, currency);
        billingInfo.put(x.description, orderId);
        billingInfo.put(x.cardNumber, cardNumber);
        billingInfo.put(x.cardHolder, nameOnCard);
        billingInfo.put(x.expiryDate, expireDate);
        billingInfo.put(x.cardType, cardType);
        billingInfo.put(x.cv2, securityCode);
        billingInfo.put(x.billingPostCode, postalCode);
        billingInfo.put(x.billingAddress, address);

        Debug.logInfo(x.SagePay_billingInfo + billingInfo, MODULE);
        Debug.logInfo(x.SagePay_Exiting_buildCustomerBillingInfo, MODULE);

        return billingInfo;
    }

    public static Map<String, Object> ccAuth(DispatchContext dctx, SagePayPaymentServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_ccAuth, MODULE);
        Debug.logInfo(x.SagePay_ccAuth_context + context, MODULE);
        Map<String, Object> response = null;
        String orderId = (String) context.get(x.orderId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        if (orderPaymentPreference == null) {
            response = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayOrderPaymenPreferenceIsNull,
                    UtilMisc.toMap(x.orderId, orderId, x.orderPaymentPreference, null), locale));
        } else {
            response = processCardAuthorisationPayment(dctx, context);
        }
        Debug.logInfo(x.SagePay_ccAuth_response + response, MODULE);
        Debug.logInfo(x.SagePay_Exiting_ccAuth, MODULE);
        return response;
    }
    private static Map<String, Object> processCardAuthorisationPayment(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, String> billingInfo = buildCustomerBillingInfo(context);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        try {
            Map<String, Object> paymentResult = dispatcher.runSync(x.SagePayPaymentAuthentication,
                    UtilMisc.toMap(
                            x.paymentGatewayConfigId, paymentGatewayConfigId,
                            x.vendorTxCode, billingInfo.get(x.orderId),
                            x.cardHolder, billingInfo.get(x.cardHolder),
                            x.cardNumber, billingInfo.get(x.cardNumber),
                            x.expiryDate, billingInfo.get(x.expiryDate),
                            x.cardType, billingInfo.get(x.cardType),
                            x.cv2, billingInfo.get(x.cv2),
                            x.description, billingInfo.get(x.description),
                            x.amount, billingInfo.get(x.amount),
                            x.currency, billingInfo.get(x.currency),
                            x.billingAddress, billingInfo.get(x.billingAddress),
                            x.billingPostCode, billingInfo.get(x.billingPostCode)));

            Debug.logInfo(x.SagePay_SagePayPaymentAuthentication_result + paymentResult, MODULE);

            String transactionType = (String) paymentResult.get(x.transactionType);
            String status = (String) paymentResult.get(x.status);
            String statusDetail = (String) paymentResult.get(x.statusDetail);
            String vpsTxId = (String) paymentResult.get(x.vpsTxId);
            String securityKey = (String) paymentResult.get(x.securityKey);
            String txAuthNo = (String) paymentResult.get(x.txAuthNo);
            String vendorTxCode = (String) paymentResult.get(x.vendorTxCode);
            String amount = (String) paymentResult.get(x.amount);

            if (status != null && x.OK.equals(status)) {
                Debug.logInfo(x.SagePay_Payment_authorized_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.TRUE, txAuthNo, securityKey, new BigDecimal(amount), vpsTxId,
                        vendorTxCode, statusDetail);
                if (x.PAYMENT_6e5eb56a.equals(transactionType)) {
                    Map<String, Object> captureResult = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthNo, securityKey,
                            new BigDecimal(amount), vpsTxId, vendorTxCode, statusDetail);
                    result.putAll(captureResult);
                }
            } else if (status != null && x.INVALID.equals(status)) {
                Debug.logInfo(x.SagePay_Invalid_authorisation_request_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, x.INVALID,
                        vendorTxCode, statusDetail);
            } else if (status != null && x.MALFORMED.equals(status)) {
                Debug.logInfo(x.SagePay_Malformed_authorisation_request_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, x.MALFORMED,
                        vendorTxCode, statusDetail);
            } else if (status != null && x.NOTAUTHED.equals(status)) {
                Debug.logInfo(x.SagePay_NotAuthed_authorisation_request_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, BigDecimal.ZERO, vpsTxId, vendorTxCode,
                        statusDetail);
            } else if (status != null && x.REJECTED.equals(status)) {
                Debug.logInfo(x.SagePay_Rejected_authorisation_request_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, new BigDecimal(amount), vpsTxId,
                        vendorTxCode, statusDetail);
            } else {
                Debug.logInfo(x.SagePay_Invalid_status + status + x.received_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, x.ERROR,
                        vendorTxCode, statusDetail);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_in_calling_SagePayPaymentAuthentication, MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentAuthorisationException,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_ccCapture, MODULE);
        Debug.logInfo(x.SagePay_ccCapture_context + context, MODULE);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        context.put(x.authTransaction, authTransaction);
        Map<String, Object> response = processCardCapturePayment(ctx, context);

        Debug.logInfo(x.SagePay_ccCapture_response + response, MODULE);
        Debug.logInfo(x.SagePay_Exiting_ccCapture, MODULE);

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

            Map<String, Object> paymentResult = dispatcher.runSync(x.SagePayPaymentAuthorisation,
                    UtilMisc.toMap(
                            x.paymentGatewayConfigId, paymentGatewayConfigId,
                            x.vendorTxCode, vendorTxCode,
                            x.vpsTxId, vpsTxId,
                            x.securityKey, securityKey,
                            x.txAuthNo, txAuthCode,
                            x.amount, amount.toString()));
            Debug.logInfo(x.SagePay_SagePayPaymentAuthorisation_result + paymentResult, MODULE);
            String status = (String) paymentResult.get(x.status);
            String statusDetail = (String) paymentResult.get(x.statusDetail);
            if (status != null && x.OK.equals(status)) {
                Debug.logInfo(x.SagePay_Payment_Released_for_Order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode,
                        statusDetail);
            } else {
                Debug.logInfo(x.SagePay_Invalid_status + status + x.received_for_order + vendorTxCode, MODULE);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.FALSE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode,
                        statusDetail);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_in_calling_SagePayPaymentAuthorisation, MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentAuthorisationException,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_ccRefund, MODULE);
        Debug.logInfo(x.SagePay_ccRefund_context + context, MODULE);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue captureTransaction = PaymentGatewayServices.getCaptureTransaction(orderPaymentPreference);
        if (captureTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund,
                    locale));
        }
        Debug.logInfo(x.SagePay_ccRefund_captureTransaction + captureTransaction, MODULE);
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne(x.CreditCard, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_getting_CreditCard_for_OrderPaymentPreference + orderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPaymentUnableToGetCCInfo, locale)
                    + x.str_b858cb28 + orderPaymentPreference);
        }
        context.put(x.creditCard, creditCard);
        context.put(x.captureTransaction, captureTransaction);

        List<GenericValue> authTransactions = PaymentGatewayServices.getAuthTransactions(orderPaymentPreference);

        EntityCondition authCondition = EntityCondition.makeCondition(x.paymentServiceTypeEnumId, x.PRDS_PAY_AUTH);
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
            Debug.logInfo(x.SagePay_Calling_Refund_for_Refund, MODULE);
            response = processCardRefundPayment(ctx, context);
        } else {

            Calendar cal = Calendar.getInstance();
            cal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DATE), 23, 49, 59);

            if (authCal.before(cal)) {
                Debug.logInfo(x.SagePay_Calling_Void_for_Refund, MODULE);
                response = processCardVoidPayment(ctx, context);
            } else {
                Debug.logInfo(x.SagePay_Calling_Refund_for_Refund, MODULE);
                response = processCardRefundPayment(ctx, context);
            }
        }

        Debug.logInfo(x.SagePay_ccRefund_response + response, MODULE);
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
        orderId = x.R + orderId;

        try {

            Map<String, Object> paymentResult = dispatcher.runSync(x.SagePayPaymentRefund,
                    UtilMisc.toMap(
                            x.paymentGatewayConfigId, paymentGatewayConfigId,
                            x.vendorTxCode, orderId,
                            x.amount, amount.toString(),
                            x.currency, x.GBP,
                            x.description, orderId,
                            x.relatedVPSTxId, captureTransaction.get(x.referenceNum),
                            x.relatedVendorTxCode, captureTransaction.get(x.altReference),
                            x.relatedSecurityKey, captureTransaction.get(x.gatewayFlag),
                            x.relatedTxAuthNo, captureTransaction.get(x.gatewayCode)));
            Debug.logInfo(x.SagePay_SagePayPaymentRefund_result + paymentResult, MODULE);

            String status = (String) paymentResult.get(x.status);
            String statusDetail = (String) paymentResult.get(x.statusDetail);
            String vpsTxId = (String) paymentResult.get(x.vpsTxId);
            String txAuthNo = (String) paymentResult.get(x.txAuthNo);

            if (status != null && x.OK.equals(status)) {
                Debug.logInfo(x.SagePay_Payment_Refunded_for_Order + orderId, MODULE);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.TRUE, txAuthNo, amount, vpsTxId, orderId, statusDetail);
            } else {
                Debug.logInfo(x.SagePay_Invalid_status + status + x.received_for_order + orderId, MODULE);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.FALSE, null, BigDecimal.ZERO, status, orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_in_calling_SagePayPaymentRefund, MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentRefundException,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
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
            Map<String, Object> paymentResult = dispatcher.runSync(x.SagePayPaymentVoid,
                    UtilMisc.toMap(
                            x.paymentGatewayConfigId, paymentGatewayConfigId,
                            x.vendorTxCode, captureTransaction.get(x.altReference),
                            x.vpsTxId, captureTransaction.get(x.referenceNum),
                            x.securityKey, captureTransaction.get(x.gatewayFlag),
                            x.txAuthNo, captureTransaction.get(x.gatewayCode)));

            Debug.logInfo(x.SagePay_SagePayPaymentVoid_result + paymentResult, MODULE);

            String status = (String) paymentResult.get(x.status);
            String statusDetail = (String) paymentResult.get(x.statusDetail);

            if (status != null && x.OK.equals(status)) {
                Debug.logInfo(x.SagePay_Payment_Voided_for_Order + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.TRUE, amount, x.SUCCESS, orderId, statusDetail);
            } else if (status != null && x.MALFORMED.equals(status)) {
                Debug.logInfo(x.SagePay_Malformed_void_request_for_order + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, x.MALFORMED, orderId, statusDetail);
            } else if (status != null && x.INVALID.equals(status)) {
                Debug.logInfo(x.SagePay_Invalid_void_request_for_order + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, x.INVALID, orderId, statusDetail);
            } else if (status != null && x.ERROR.equals(status)) {
                Debug.logInfo(x.SagePay_Error_in_void_request_for_order + orderId, MODULE);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, x.ERROR, orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_in_calling_SagePayPaymentVoid, MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentVoidException,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, SagePayPaymentServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_ccRelease, MODULE);
        Debug.logInfo(x.SagePay_ccRelease_context + context, MODULE);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);

        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease,
                    locale));
        }
        context.put(x.authTransaction, authTransaction);

        Map<String, Object> response = processCardReleasePayment(ctx, context);
        Debug.logInfo(x.SagePay_ccRelease_response + response, MODULE);
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
            Map<String, Object> paymentResult = dispatcher.runSync(x.SagePayPaymentRelease,
                    UtilMisc.toMap(
                            x.paymentGatewayConfigId, paymentGatewayConfigId,
                            x.vendorTxCode, orderId,
                            x.releaseAmount, amount.toString(),
                            x.vpsTxId, refNum,
                            x.securityKey, authTransaction.get(x.gatewayFlag),
                            x.txAuthNo, authTransaction.get(x.gatewayCode)));

            Debug.logInfo(x.SagePay_SagePayPaymentRelease_result + paymentResult, MODULE);

            String status = (String) paymentResult.get(x.status);
            String statusDetail = (String) paymentResult.get(x.statusDetail);

            if (status != null && x.OK.equals(status)) {
                Debug.logInfo(x.SagePay_Payment_Released_for_Order + orderId, MODULE);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.TRUE, null, amount, refNum, orderId, statusDetail);
            } else {
                Debug.logInfo(x.SagePay_Invalid_status + status + x.received_for_order + orderId, MODULE);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.FALSE, null, amount, refNum, orderId, statusDetail);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_in_calling_SagePayPaymentRelease, MODULE);
            result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentReleaseException,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        return result;
    }

}
