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
package org.apache.ofbiz.accounting.thirdparty.gosoftware;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PcChargeServicesContext;
public class PcChargeServices {

    private static final String MODULE = PcChargeServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING_MODE = UtilNumber.getRoundingMode(x.invoice_rounding);

    public static Map<String, Object> ccAuth(DispatchContext dctx, PcChargeServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPcChargeErrorGettingPaymentGatewayConfig, locale));
        }

        try {
            PcChargeServices.setCreditCardInfo(api, context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // basic tx info
        api.set(PcChargeApi.TRANS_AMOUNT, getAmountString(context, x.processAmount));
        api.set(PcChargeApi.TICKET_NUM, context.get(x.orderId));
        api.set(PcChargeApi.MANUAL_FLAG, x._0);
        api.set(PcChargeApi.PRESENT_FLAG, x._1);

        // command setting
        if (x._1.equals(props.getProperty(x.autoBill))) {
            // sale
            api.set(PcChargeApi.COMMAND, x._1);
        } else {
            // pre-auth
            api.set(PcChargeApi.COMMAND, x._4);
        }

        // send the transaction
        PcChargeApi out = null;
        try {
            out = api.send();
        } catch (IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(PcChargeApi.RESULT);
        boolean passed = false;
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.authResult, Boolean.TRUE);
            result.put(x.captureResult, Boolean.TRUE);
            passed = true;
        } else if (x.APPROVED.equals(resultCode)) {
            result.put(x.authCode, out.get(PcChargeApi.AUTH_CODE));
            result.put(x.authResult, Boolean.TRUE);
            passed = true;
        } else if (x.PROCESSED.equals(resultCode)) {
            result.put(x.authResult, Boolean.TRUE);
        } else {
            result.put(x.authResult, Boolean.FALSE);
        }

        result.put(x.authRefNum, out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : x.emptyString);
        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authCode, out.get(PcChargeApi.AUTH_CODE));
        result.put(x.authFlag, out.get(PcChargeApi.REFERENCE));
        result.put(x.authMessage, out.get(PcChargeApi.RESULT));
        result.put(x.cvCode, out.get(PcChargeApi.CVV2_CODE));
        result.put(x.avsCode, out.get(PcChargeApi.AVS_CODE));

        if (!passed) {
            String respMsg = out.get(PcChargeApi.RESULT) + x.str_0d0c4ddd + out.get(PcChargeApi.AUTH_CODE);
            String refNum = out.get(PcChargeApi.TROUTD);
            result.put(x.customerRespMsgs, UtilMisc.toList(respMsg, refNum));
        }

        if (result.get(x.captureResult) != null) {
            result.put(x.captureCode, out.get(PcChargeApi.AUTH_CODE));
            result.put(x.captureFlag, out.get(PcChargeApi.REFERENCE));
            result.put(x.captureRefNum, out.get(PcChargeApi.TROUTD));
            result.put(x.captureMessage, out.get(PcChargeApi.RESULT));
        }

        return result;

    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, PcChargeServicesContext context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        // lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotCapture, locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPcChargeErrorGettingPaymentGatewayConfig, locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString(x.referenceNum));
        api.set(PcChargeApi.COMMAND, x._5);

        // send the transaction
        PcChargeApi out = null;
        try {
            out = api.send();
        } catch (IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(PcChargeApi.RESULT);
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.captureResult, Boolean.TRUE);
        } else {
            result.put(x.captureResult, Boolean.FALSE);
        }
        result.put(x.captureAmount, context.get(x.captureAmount));
        result.put(x.captureRefNum, out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : x.emptyString);
        result.put(x.captureCode, out.get(PcChargeApi.AUTH_CODE));
        result.put(x.captureFlag, out.get(PcChargeApi.REFERENCE));
        result.put(x.captureMessage, out.get(PcChargeApi.RESULT));

        return result;
    }

    public static Map<String, Object> ccRelease(DispatchContext dctx, PcChargeServicesContext context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Delegator delegator = dctx.getDelegator();
        // lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        Locale locale = (Locale) context.get(x.locale);

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPcChargeErrorGettingPaymentGatewayConfig, locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString(x.referenceNum));
        api.set(PcChargeApi.COMMAND, x._3);

        // check to make sure we are configured for SALE mode
        if (!x._true.equalsIgnoreCase(props.getProperty(x.autoBill))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPcChargeCannotSupportReleasingPreAuth, locale));
        }

        // send the transaction
        PcChargeApi out = null;
        try {
            out = api.send();
        } catch (IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(PcChargeApi.RESULT);
        if (x.VOIDED.equals(resultCode)) {
            result.put(x.releaseResult, Boolean.TRUE);
        } else {
            result.put(x.releaseResult, Boolean.FALSE);
        }
        result.put(x.releaseAmount, context.get(x.releaseAmount));
        result.put(x.releaseRefNum, out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : x.emptyString);
        result.put(x.releaseCode, out.get(PcChargeApi.AUTH_CODE));
        result.put(x.releaseFlag, out.get(PcChargeApi.REFERENCE));
        result.put(x.releaseMessage, out.get(PcChargeApi.RESULT));

        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, PcChargeServicesContext context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Delegator delegator = dctx.getDelegator();
        // lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        Locale locale = (Locale) context.get(x.locale);

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPcChargeErrorGettingPaymentGatewayConfig, locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString(x.referenceNum));
        api.set(PcChargeApi.COMMAND, x._2);

        // send the transaction
        PcChargeApi out = null;
        try {
            out = api.send();
        } catch (IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(PcChargeApi.RESULT);
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.refundResult, Boolean.TRUE);
        } else {
            result.put(x.refundResult, Boolean.FALSE);
        }
        result.put(x.refundAmount, context.get(x.releaseAmount));
        result.put(x.refundRefNum, out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : x.emptyString);
        result.put(x.refundCode, out.get(PcChargeApi.AUTH_CODE));
        result.put(x.refundFlag, out.get(PcChargeApi.REFERENCE));
        result.put(x.refundMessage, out.get(PcChargeApi.RESULT));

        return result;
    }

    private static void setCreditCardInfo(PcChargeApi api, PcChargeServicesContext context) throws GeneralException {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        if (creditCard != null) {
            List<String> expDateList = StringUtil.split(creditCard.getString(x.expireDate), x.str_42099b4a);
            String month = expDateList.get(0);
            String year = expDateList.get(1);
            String y2d = year.substring(2);
            String expDate = month + y2d;

            String title = creditCard.getString(x.titleOnCard);
            String fname = creditCard.getString(x.firstNameOnCard);
            String mname = creditCard.getString(x.middleNameOnCard);
            String lname = creditCard.getString(x.lastNameOnCard);
            String sufix = creditCard.getString(x.suffixOnCard);
            StringBuilder name = new StringBuilder();
            if (UtilValidate.isNotEmpty(title)) {
                name.append(title).append(x.str_b858cb28);
            }
            if (UtilValidate.isNotEmpty(fname)) {
                name.append(fname).append(x.str_b858cb28);
            }
            if (UtilValidate.isNotEmpty(mname)) {
                name.append(mname).append(x.str_b858cb28);
            }
            if (UtilValidate.isNotEmpty(lname)) {
                name.append(lname).append(x.str_b858cb28);
            }
            if (UtilValidate.isNotEmpty(sufix)) {
                name.append(sufix);
            }
            String nameOnCard = name.toString().trim();
            String acctNumber = x.F + creditCard.getString(x.cardNumber);
            String cvNum = (String) context.get(x.cardSecurityCode);

            api.set(PcChargeApi.ACCT_NUM, acctNumber);
            api.set(PcChargeApi.EXP_DATE, expDate);
            api.set(PcChargeApi.CARDHOLDER, nameOnCard);
            if (UtilValidate.isNotEmpty(cvNum)) {
                api.set(PcChargeApi.CVV2, cvNum);
            }

            // billing address information
            GenericValue billingAddress = (GenericValue) context.get(x.billingAddress);
            if (billingAddress != null) {
                api.set(PcChargeApi.STREET, billingAddress.getString(x.address1));
                api.set(PcChargeApi.ZIP_CODE, billingAddress.getString(x.postalCode));
            } else {
                String zipCode = orderPaymentPreference.getString(x.billingPostalCode);
                if (UtilValidate.isNotEmpty(zipCode)) {
                    api.set(PcChargeApi.ZIP_CODE, zipCode);
                }
            }
        } else {
            throw new GeneralException(x.No_CreditCard_object_found);
        }
    }

    private static PcChargeApi getApi(Properties props) {
        if (props == null) {
            Debug.logError(x.Cannot_load_API_w_null_properties, MODULE);
            return null;
        }
        String host = props.getProperty(x.host);
        int port = 0;
        try {
            port = Integer.parseInt(props.getProperty(x.port));
        } catch (RuntimeException e) {
            Debug.logError(e, MODULE);
        }
        PcChargeApi api = null;
        if (port > 0 && host != null) {
            api = new PcChargeApi(host, port);
        } else {
            api = new PcChargeApi();
        }

        api.set(PcChargeApi.PROCESSOR_ID, props.getProperty(x.processorID));
        api.set(PcChargeApi.MERCH_NUM, props.getProperty(x.merchantID));
        api.set(PcChargeApi.USER_ID, props.getProperty(x.userID));
        return api;
    }

    private static Properties buildPccProperties(PcChargeServicesContext context, Delegator delegator) {
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }

        String processorId = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_processorID, delegator);
        String merchantId = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_merchantID, delegator);
        String userId = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_userID, delegator);
        String host = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_host, delegator);
        String port = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_port, delegator);
        String autoBill = EntityUtilProperties.getPropertyValue(configString, x.payment_pccharge_autoBill, x._true, delegator);

        // some property checking
        if (UtilValidate.isEmpty(processorId)) {
            Debug.logWarning(x.The_processorID_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }
        if (UtilValidate.isEmpty(merchantId)) {
            Debug.logWarning(x.The_merchantID_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }
        if (UtilValidate.isEmpty(userId)) {
            Debug.logWarning(x.The_userID_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }

        // create some properties for CS Client
        Properties props = new Properties();
        props.put(x.processorID, processorId);
        props.put(x.merchantID, merchantId);
        props.put(x.userID, userId);
        props.put(x.host, host);
        props.put(x.port, port);
        props.put(x.autoBill, autoBill);
        Debug.logInfo(x.Returning_properties + props, MODULE);

        return props;
    }

    private static String getAmountString(PcChargeServicesContext context, String amountField) {
        BigDecimal processAmount = (BigDecimal) context.get(amountField);
        return processAmount.setScale(DECIMALS, ROUNDING_MODE).toPlainString();
    }

}
