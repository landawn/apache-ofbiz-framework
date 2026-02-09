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
package org.apache.ofbiz.accounting.thirdparty.eway;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class EwayServices {

    private static final String MODULE = EwayServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";

    // eway charge (auth w/ capture)
    public static Map<String, Object> ewayCharge(DispatchContext dctx, Map<String, Object> context) {
        String orderId = (String) context.get(org.apache.ofbiz.persistence.entity.x.orderId);
        String cvv2 = (String) context.get(org.apache.ofbiz.persistence.entity.x.cardSecurityCode);
        String custIp = (String) context.get(org.apache.ofbiz.persistence.entity.x.customerIpAddress);
        BigDecimal processAmount = (BigDecimal) context.get(org.apache.ofbiz.persistence.entity.x.processAmount);
        GenericValue cc = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.creditCard);
        GenericValue address = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.billingAddress);
        GenericValue party = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.billToParty);

        GatewayRequest req = initRequest(dctx, context, false);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(processAmount);
        req.setCustomerIPAddress(custIp);

        // bill to party info
        req.setCustomerFirstName(UtilFormatOut.checkNull(party.getString(org.apache.ofbiz.persistence.entity.x.firstName)));
        req.setCustomerLastName(UtilFormatOut.checkNull(party.getString(org.apache.ofbiz.persistence.entity.x.lastName)));

        // card info
        String ccName = cc.getString(org.apache.ofbiz.persistence.entity.x.firstNameOnCard) + " " + cc.getString(org.apache.ofbiz.persistence.entity.x.lastNameOnCard);
        req.setCardHoldersName(ccName);
        req.setCardNumber(cc.getString(org.apache.ofbiz.persistence.entity.x.cardNumber));
        if (cc.get(org.apache.ofbiz.persistence.entity.x.expireDate) != null) {
            String[] exp = cc.getString(org.apache.ofbiz.persistence.entity.x.expireDate).split("\\/");
            req.setCardExpiryMonth(exp[0]);
            req.setCardExpiryYear(exp[1]);
        }

        // security code
        if (UtilValidate.isNotEmpty(cvv2)) {
            req.setCVN(cvv2);
        }

        // billing address
        if (address != null) {
            String street = address.getString(org.apache.ofbiz.persistence.entity.x.address1) + ((UtilValidate.isNotEmpty(address.getString(org.apache.ofbiz.persistence.entity.x.address2))) ? " "
                    + address.getString(org.apache.ofbiz.persistence.entity.x.address2) : "");
            req.setCustomerAddress(street);
            req.setCustomerPostcode(address.getString(org.apache.ofbiz.persistence.entity.x.postalCode));
            req.setCustomerBillingCountry(address.getString(org.apache.ofbiz.persistence.entity.x.countryGeoId));
        }

        // send the request
        GatewayConnector con = new GatewayConnector();
        GatewayResponse reply;
        try {
            reply = con.sendRequest(req);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        // process the result
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean authResult = reply.getTrxnStatus();
        // auth fields
        result.put("authResult", authResult);
        result.put("authMessage", reply.getTrxnError());
        result.put("authCode", reply.getAuthCode());
        result.put("authRefNum", reply.getTrxnNumber());
        result.put("scoreCode", Double.valueOf(reply.getBeagleScore()).toString());
        result.put("processAmount", reply.getTransactionAmount());
        // capture fields
        result.put("captureResult", result.get("authResult"));
        result.put("captureMessage", result.get("authMessage"));
        result.put("captureRefNum", result.get("authRefNum"));
        return result;
    }

    // eway refund
    public static Map<String, Object> ewayRefund(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get(org.apache.ofbiz.persistence.entity.x.refundAmount);
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        // original charge transaction
        GenericValue chargeTrans = PaymentGatewayServices.getCaptureTransaction(paymentPref);
        if (chargeTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }

        // credit card used for transaction
        GenericValue cc = null;
        try {
            cc = delegator.getRelatedOne("CreditCard", paymentPref, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }

        // orig ref number
        String refNum = chargeTrans.getString(org.apache.ofbiz.persistence.entity.x.referenceNum);
        String orderId = paymentPref.getString(org.apache.ofbiz.persistence.entity.x.orderId);

        GatewayRequest req = initRequest(dctx, context, true);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(refundAmount);
        req.setTrxnNumber(refNum);

        // set the card expire date
        if (cc.get(org.apache.ofbiz.persistence.entity.x.expireDate) != null) {
            String[] exp = cc.getString(org.apache.ofbiz.persistence.entity.x.expireDate).split("\\/");
            req.setCardExpiryMonth(exp[0]);
            req.setCardExpiryYear(exp[1]);
        }

        // send the request
        GatewayConnector con = new GatewayConnector();
        GatewayResponse reply;
        try {
            reply = con.sendRequest(req);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        // process the result
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean refundResult = reply.getTrxnStatus();
        result.put("refundResult", refundResult);
        result.put("refundMessage", reply.getTrxnError());
        result.put("refundCode", reply.getAuthCode());
        result.put("refundRefNum", reply.getTrxnNumber());
        result.put("refundAmount", reply.getTransactionAmount());

        return result;
    }

    // eway release (does a refund)
    public static Map<String, Object> ewayRelease(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.orderPaymentPreference);
        BigDecimal releaseAmount = (BigDecimal) context.get(org.apache.ofbiz.persistence.entity.x.releaseAmount);
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        // original charge transaction
        GenericValue chargeTrans = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.authTrans);
        if (chargeTrans == null) {
            chargeTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }
        if (chargeTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }
        // credit card used for transaction
        GenericValue cc = null;
        try {
            cc = delegator.getRelatedOne("CreditCard", paymentPref, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }

        // orig ref number
        String refNum = chargeTrans.getString(org.apache.ofbiz.persistence.entity.x.referenceNum);
        String orderId = paymentPref.getString(org.apache.ofbiz.persistence.entity.x.orderId);

        GatewayRequest req = initRequest(dctx, context, true);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(releaseAmount);
        req.setTrxnNumber(refNum);

        // set the card expire date
        if (cc.get(org.apache.ofbiz.persistence.entity.x.expireDate) != null) {
            String[] exp = cc.getString(org.apache.ofbiz.persistence.entity.x.expireDate).split("\\/");
            req.setCardExpiryMonth(exp[0]);
            req.setCardExpiryYear(exp[1]);
        }
        // send the request
        GatewayConnector con = new GatewayConnector();
        GatewayResponse reply;
        try {
            reply = con.sendRequest(req);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        // process the result
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean refundResult = reply.getTrxnStatus();
        result.put("releaseResult", refundResult);
        result.put("releaseMessage", reply.getTrxnError());
        result.put("releaseCode", reply.getAuthCode());
        result.put("releaseRefNum", reply.getTrxnNumber());
        result.put("releaseAmount", reply.getTransactionAmount());
        return result;
    }
    private static GatewayRequest initRequest(DispatchContext dctx, Map<String, Object> context, boolean refund) {
        String pgcId = (String) context.get(org.apache.ofbiz.persistence.entity.x.paymentGatewayConfigId);
        String cfgStr = (String) context.get(org.apache.ofbiz.persistence.entity.x.paymentConfig);
        Delegator delegator = dctx.getDelegator();

        String customerId = getPaymentGatewayConfigValue(delegator, pgcId, "customerId", cfgStr, "payment.eway.customerId");
        String refundPwd = getPaymentGatewayConfigValue(delegator, pgcId, "refundPwd", cfgStr, "payment.eway.refundPwd");
        boolean testMode = "Y".equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, "testMode", cfgStr, "payment.eway.testMode"));
        Boolean beagle = "Y".equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, "enableBeagle", cfgStr, "payment.eway.enableBeagle"));
        Boolean cvn = "Y".equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, "enableCvn", cfgStr, "payment.eway.enableCvn"));

        // the request mode
        int requestMode = refund ? GatewayRequest.REQUEST_METHOD_REFUND : beagle ? GatewayRequest.REQUEST_METHOD_BEAGLE : cvn
                ? GatewayRequest.REQUEST_METHOD_CVN : 0;

        // create the request object
        GatewayRequest req = new GatewayRequest(requestMode);
        req.setTestMode(testMode);
        req.setCustomerID(customerId);
        if (refund) {
            req.setRefundPassword(refundPwd);
        }
        return req;
    }
    private static String getPaymentGatewayConfigValue(Delegator delegator, String cfgId, String cfgParamName,
            String resource, String resParamName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(cfgId)) {
            try {
                GenericValue gv = EntityQuery.use(delegator).from("PaymentGatewayEway")
                        .where("paymantGatewayConfigId", cfgId).cache().queryOne();
                if (gv != null) {
                    Object field = gv.get(cfgParamName);
                    if (field != null) {
                        returnValue = field.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, resParamName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }
}
