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
package org.apache.ofbiz.accounting.thirdparty.securepay;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewayResponseDao;
import org.apache.ofbiz.persistence.dao.PaymentGatewaySecurePayDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewayResponseEntity;
import org.apache.ofbiz.persistence.entity.PaymentGatewaySecurePayEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import securepay.jxa.api.Payment;
import securepay.jxa.api.Txn;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.SecurePayPaymentServicesContext;
public class SecurePayPaymentServices {

    private static final String MODULE = SecurePayPaymentServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    public static Map<String, Object> doAuth(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get(x.orderId);
        BigDecimal processAmount = (BigDecimal) context.get(x.processAmount);
        // generate the request/properties
        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayNotProperlyConfigurated, locale));
        }

        String merchantId = props.getProperty(x.merchantID);
        String serverURL = props.getProperty(x.serverurl);
        String processtimeout = props.getProperty(x.processtimeout);
        String pwd = props.getProperty(x.pwd);
        String enableamountround = props.getProperty(x.enableamountround);
        String currency = (String) context.get(x.currency);
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (x.Y.equals(enableamountround)) {
            newAmount = new BigDecimal(processAmount.setScale(0, RoundingMode.HALF_UP)+x._00);
        } else {
            newAmount = processAmount;
        }

        if (x.JPY.equals(currency)) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        String expiryDate = (String) creditCard.get(x.expireDate);
        String cardSecurityCode = (String) context.get(x.cardSecurityCode);
        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);

        Txn txn = payment.addTxn(10, orderId);
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        if (UtilValidate.isNotEmpty(currency)) {
            txn.setCurrencyCode(currency);
        } else {
            txn.setCurrencyCode(x.AUD);
        }

        txn.setCardNumber((String) creditCard.get(x.cardNumber));
        txn.setExpiryDate(expiryDate.substring(0, 3) + expiryDate.substring(5));
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            txn.setCVV(cardSecurityCode);
        }
        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayPaymentWasNotSent, locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false) {
                    result.put(x.authResult, Boolean.FALSE);
                    result.put(x.authRefNum, x.N_A);
                    result.put(x.processAmount, BigDecimal.ZERO);
                } else {
                    result.put(x.authRefNum, resp.getTxnId());
                    result.put(x.authResult, Boolean.TRUE);
                    result.put(x.processAmount, processAmount);
                }
                result.put(x.authCode, resp.getResponseCode());
                result.put(x.authMessage, resp.getResponseText());
            }
        }
        return result;
    }

    public static Map<String, Object> ccReAuth(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> doCapture(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotCapture, locale));
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayNotProperlyConfigurated, locale));
        }

        String merchantId = props.getProperty(x.merchantID);
        String serverURL = props.getProperty(x.serverurl);
        String processtimeout = props.getProperty(x.processtimeout);
        String pwd = props.getProperty(x.pwd);
        String enableamountround = props.getProperty(x.enableamountround);
        String currency = authTransaction.getString(x.currencyUomId);
        BigDecimal captureAmount = (BigDecimal) context.get(x.captureAmount);
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (x.Y.equals(enableamountround)) {
            newAmount = new BigDecimal(captureAmount.setScale(0, RoundingMode.HALF_UP)+x._00);
        } else {
            newAmount = captureAmount;
        }

        if (x.JPY.equals(currency)) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(11, (String) orderPaymentPreference.get(x.orderId));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setPreauthId(authTransaction.getString(x.referenceNum));

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayPaymentWasNotSent, locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false) {
                    result.put(x.captureResult, false);
                    result.put(x.captureRefNum, authTransaction.getString(x.referenceNum));
                    result.put(x.captureAmount, BigDecimal.ZERO);
                } else {
                    result.put(x.captureResult, true);
                    result.put(x.captureAmount, captureAmount);
                    result.put(x.captureRefNum, resp.getTxnId());
                }
                result.put(x.captureFlag, x.C);
                result.put(x.captureCode, resp.getResponseCode());
                result.put(x.captureMessage, resp.getResponseText());
            }
        }
        return result;
    }

    public static Map<String, Object> doVoid(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayNotProperlyConfigurated, locale));
        }

        String merchantId = props.getProperty(x.merchantID);
        String serverURL = props.getProperty(x.serverurl);
        String processtimeout = props.getProperty(x.processtimeout);
        String pwd = props.getProperty(x.pwd);
        String enableamountround = props.getProperty(x.enableamountround);
        String currency = authTransaction.getString(x.currencyUomId);
        BigDecimal releaseAmount = (BigDecimal) context.get(x.releaseAmount);
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (x.Y.equals(enableamountround)) {
            newAmount = new BigDecimal(releaseAmount.setScale(0, RoundingMode.HALF_UP)+x._00);
        } else {
            newAmount = releaseAmount;
        }

        if (x.JPY.equals(currency)) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(6, (String) orderPaymentPreference.get(x.orderId));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setTxnId(authTransaction.getString(x.referenceNum));

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayPaymentWasNotSent, locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false) {
                    result.put(x.releaseResult, false);
                    result.put(x.releaseRefNum, authTransaction.getString(x.referenceNum));
                    result.put(x.releaseAmount, BigDecimal.ZERO);
                } else {
                    result.put(x.releaseResult, true);
                    result.put(x.releaseAmount, releaseAmount);
                    result.put(x.releaseRefNum, resp.getTxnId());
                }
                result.put(x.releaseFlag, x.U);
                result.put(x.releaseCode, resp.getResponseCode());
                result.put(x.releaseMessage, resp.getResponseText());
            }
        }
        return result;
    }

    public static Map<String, Object> doRefund(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        String referenceNum = null;
        try {
            PaymentGatewayResponseDao paymentGatewayResponseDao = DaoRegistry.getDao(delegator, x.PaymentGatewayResponse, PaymentGatewayResponseDao.class);
            PaymentGatewayResponseEntity paymentGatewayResponse = paymentGatewayResponseDao.list(Filters.and(
                    Filters.eq(x.orderPaymentPreferenceId, authTransaction.get(x.orderPaymentPreferenceId)),
                    Filters.eq(x.paymentServiceTypeEnumId, x.PRDS_PAY_CAPTURE))).stream().findFirst().orElse(null);
            referenceNum = paymentGatewayResponse != null ? paymentGatewayResponse.getReferenceNum() : authTransaction.getString(x.referenceNum);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayNotProperlyConfigurated, locale));
        }

        String merchantId = props.getProperty(x.merchantID);
        String serverURL = props.getProperty(x.serverurl);
        String processtimeout = props.getProperty(x.processtimeout);
        String pwd = props.getProperty(x.pwd);
        String enableamountround = props.getProperty(x.enableamountround);
        String currency = authTransaction.getString(x.currencyUomId);
        BigDecimal refundAmount = (BigDecimal) context.get(x.refundAmount);
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;

        if (x.Y.equals(enableamountround)) {
            newAmount = new BigDecimal(refundAmount.setScale(0, RoundingMode.HALF_UP)+x._00);
        } else {
            newAmount = refundAmount;
        }

        int amont;
        if (x.JPY.equals(currency)) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(4, (String) orderPaymentPreference.get(x.orderId));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setTxnId(referenceNum);

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayPaymentWasNotSent, locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false) {
                    result.put(x.refundResult, false);
                    result.put(x.refundRefNum, authTransaction.getString(x.referenceNum));
                    result.put(x.refundAmount, BigDecimal.ZERO);
                } else {
                    result.put(x.refundResult, true);
                    result.put(x.refundAmount, refundAmount);
                    result.put(x.refundRefNum, resp.getTxnId());
                }
                result.put(x.refundCode, resp.getResponseCode());
                result.put(x.refundMessage, resp.getResponseText());
                result.put(x.refundFlag, x.R);
            }
        }
        return result;
    }

    public static Map<String, Object> doCredit(DispatchContext dctx, SecurePayPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        // generate the request/properties
        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayNotProperlyConfigurated, locale));
        }

        String merchantId = props.getProperty(x.merchantID);
        String serverURL = props.getProperty(x.serverurl);
        String processtimeout = props.getProperty(x.processtimeout);
        String pwd = props.getProperty(x.pwd);
        String enableamountround = props.getProperty(x.enableamountround);
        String referenceCode = (String) context.get(x.referenceCode);
        String currency = (String) context.get(x.currency);
        String cardSecurityCode = (String) context.get(x.cardSecurityCode);
        BigDecimal creditAmount = (BigDecimal) context.get(x.creditAmount);
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (x.Y.equals(enableamountround)) {
            newAmount = new BigDecimal(creditAmount.setScale(0, RoundingMode.HALF_UP)+x._00);
        } else {
            newAmount = creditAmount;
        }

        if (x.JPY.equals(currency)) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);

        Txn txn = payment.addTxn(0, referenceCode);
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setCardNumber((String) creditCard.get(x.cardNumber));
        String expiryDate = (String) creditCard.get(x.expireDate);
        txn.setExpiryDate(expiryDate.substring(0, 3) + expiryDate.substring(5));
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            txn.setCVV(cardSecurityCode);
        }

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingSecurityPayPaymentWasNotSent, locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false) {
                    result.put(x.creditResult, false);
                    result.put(x.creditRefNum, x.N_A);
                    result.put(x.creditAmount, BigDecimal.ZERO);
                } else {
                    result.put(x.creditResult, true);
                    result.put(x.creditAmount, creditAmount);
                    result.put(x.creditRefNum, resp.getTxnId());
                }
                result.put(x.creditCode, resp.getResponseCode());
                result.put(x.creditMessage, resp.getResponseText());
            }
        }
        return result;
    }

    private static Properties buildScProperties(SecurePayPaymentServicesContext context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }

        String merchantId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantId, configString, x.payment_securepay_merchantID, null);
        String pwd = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.pwd, configString, x.payment_securepay_pwd, null);
        String serverURL = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.serverURL, configString, x.payment_securepay_serverurl, null);
        String processTimeout = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.processTimeout, configString, x.payment_securepay_processtimeout, null);
        String enableAmountRound = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.enableAmountRound, configString, x.payment_securepay_enableamountround, null);

        Properties props = new Properties();
        props.put(x.merchantID, merchantId);
        props.put(x.pwd, pwd);
        props.put(x.serverurl, serverURL);
        props.put(x.processtimeout, processTimeout);
        props.put(x.enableamountround, enableAmountRound);
        return props;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                PaymentGatewaySecurePayDao paymentGatewaySecurePayDao = DaoRegistry.getDao(delegator, x.PaymentGatewaySecurePay, PaymentGatewaySecurePayDao.class);
                PaymentGatewaySecurePayEntity securePay = paymentGatewaySecurePayDao.get(paymentGatewayConfigId).orElse(null);
                if (securePay != null) {
                    Object securePayField = Beans.beanToMap(securePay).get(paymentGatewayConfigParameterName);
                    if (securePayField != null) {
                        returnValue = securePayField.toString().trim();
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

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName, String resource, String parameterName, String defaultValue) {
        String returnValue = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName, resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}
