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
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewayEwayDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewayEwayEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EwayServicesContext;
public class EwayServices {

    private static final String MODULE = EwayServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    // eway charge (auth w/ capture)
    public static Map<String, Object> ewayCharge(DispatchContext dctx, EwayServicesContext context) {
        String orderId = (String) context.get(x.orderId);
        String cvv2 = (String) context.get(x.cardSecurityCode);
        String custIp = (String) context.get(x.customerIpAddress);
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        GenericValue cc = (GenericValue) context.get(x.creditCard);
        GenericValue address = (GenericValue) context.get(x.billingAddress);
        GenericValue party = (GenericValue) context.get(x.billToParty);

        GatewayRequest req = initRequest(dctx, context, false);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(processAmount);
        req.setCustomerIPAddress(custIp);

        // bill to party info
        req.setCustomerFirstName(UtilFormatOut.checkNull(party.getString(x.firstName)));
        req.setCustomerLastName(UtilFormatOut.checkNull(party.getString(x.lastName)));

        // card info
        String ccName = cc.getString(x.firstNameOnCard) + x.str_b858cb28 + cc.getString(x.lastNameOnCard);
        req.setCardHoldersName(ccName);
        req.setCardNumber(cc.getString(x.cardNumber));
        if (cc.get(x.expireDate) != null) {
            String[] exp = cc.getString(x.expireDate).split(x.str_9d7df07c);
            req.setCardExpiryMonth(exp[0]);
            req.setCardExpiryYear(exp[1]);
        }

        // security code
        if (UtilValidate.isNotEmpty(cvv2)) {
            req.setCVN(cvv2);
        }

        // billing address
        if (address != null) {
            String street = address.getString(x.address1) + ((UtilValidate.isNotEmpty(address.getString(x.address2))) ? x.str_b858cb28
                    + address.getString(x.address2) : x.emptyString);
            req.setCustomerAddress(street);
            req.setCustomerPostcode(address.getString(x.postalCode));
            req.setCustomerBillingCountry(address.getString(x.countryGeoId));
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
        result.put(x.authResult, authResult);
        result.put(x.authMessage, reply.getTrxnError());
        result.put(x.authCode, reply.getAuthCode());
        result.put(x.authRefNum, reply.getTrxnNumber());
        result.put(x.scoreCode, Double.valueOf(reply.getBeagleScore()).toString());
        result.put(x.processAmount, reply.getTransactionAmount());
        // capture fields
        result.put(x.captureResult, result.get(x.authResult));
        result.put(x.captureMessage, result.get(x.authMessage));
        result.put(x.captureRefNum, result.get(x.authRefNum));
        return result;
    }

    // eway refund
    public static Map<String, Object> ewayRefund(DispatchContext dctx, EwayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get(x.refundAmount);
        Locale locale = (Locale) context.get(x.locale);

        // original charge transaction
        GenericValue chargeTrans = PaymentGatewayServices.getCaptureTransaction(paymentPref);
        if (chargeTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        // credit card used for transaction
        GenericValue cc = null;
        try {
            cc = delegator.getRelatedOne(x.CreditCard, paymentPref, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentUnableToGetCCInfo, locale));
        }

        // orig ref number
        String refNum = chargeTrans.getString(x.referenceNum);
        String orderId = paymentPref.getString(x.orderId);

        GatewayRequest req = initRequest(dctx, context, true);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(refundAmount);
        req.setTrxnNumber(refNum);

        // set the card expire date
        if (cc.get(x.expireDate) != null) {
            String[] exp = cc.getString(x.expireDate).split(x.str_9d7df07c);
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
        result.put(x.refundResult, refundResult);
        result.put(x.refundMessage, reply.getTrxnError());
        result.put(x.refundCode, reply.getAuthCode());
        result.put(x.refundRefNum, reply.getTrxnNumber());
        result.put(x.refundAmount, reply.getTransactionAmount());

        return result;
    }

    // eway release (does a refund)
    public static Map<String, Object> ewayRelease(DispatchContext dctx, EwayServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal releaseAmount = (BigDecimal) context.get(x.releaseAmount);
        Locale locale = (Locale) context.get(x.locale);

        // original charge transaction
        GenericValue chargeTrans = (GenericValue) context.get(x.authTrans);
        if (chargeTrans == null) {
            chargeTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }
        if (chargeTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }
        // credit card used for transaction
        GenericValue cc = null;
        try {
            cc = delegator.getRelatedOne(x.CreditCard, paymentPref, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentUnableToGetCCInfo, locale));
        }

        // orig ref number
        String refNum = chargeTrans.getString(x.referenceNum);
        String orderId = paymentPref.getString(x.orderId);

        GatewayRequest req = initRequest(dctx, context, true);
        req.setCustomerInvoiceRef(orderId);
        req.setTotalAmount(releaseAmount);
        req.setTrxnNumber(refNum);

        // set the card expire date
        if (cc.get(x.expireDate) != null) {
            String[] exp = cc.getString(x.expireDate).split(x.str_9d7df07c);
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
        result.put(x.releaseResult, refundResult);
        result.put(x.releaseMessage, reply.getTrxnError());
        result.put(x.releaseCode, reply.getAuthCode());
        result.put(x.releaseRefNum, reply.getTrxnNumber());
        result.put(x.releaseAmount, reply.getTransactionAmount());
        return result;
    }
    private static GatewayRequest initRequest(DispatchContext dctx, EwayServicesContext context, boolean refund) {
        String pgcId = (String) context.get(x.paymentGatewayConfigId);
        String cfgStr = (String) context.get(x.paymentConfig);
        Delegator delegator = dctx.getDelegator();

        String customerId = getPaymentGatewayConfigValue(delegator, pgcId, x.customerId, cfgStr, x.payment_eway_customerId);
        String refundPwd = getPaymentGatewayConfigValue(delegator, pgcId, x.refundPwd, cfgStr, x.payment_eway_refundPwd);
        boolean testMode = x.Y.equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, x.testMode, cfgStr, x.payment_eway_testMode));
        Boolean beagle = x.Y.equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, x.enableBeagle, cfgStr, x.payment_eway_enableBeagle));
        Boolean cvn = x.Y.equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, pgcId, x.enableCvn, cfgStr, x.payment_eway_enableCvn));

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
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(cfgId)) {
            try {
                PaymentGatewayEwayDao paymentGatewayEwayDao = DaoRegistry.getDao(delegator, x.PaymentGatewayEway, PaymentGatewayEwayDao.class);
                PaymentGatewayEwayEntity gv = paymentGatewayEwayDao.list(com.landawn.abacus.query.Filters.eq(x.paymantGatewayConfigId, cfgId))
                        .stream().findFirst().orElse(null);
                if (gv != null) {
                    Object field = Beans.getPropValue(gv, cfgParamName, true);
                    if (field != null) {
                        returnValue = field.toString().trim();
                    }
                }
            } catch (Exception e) {
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

