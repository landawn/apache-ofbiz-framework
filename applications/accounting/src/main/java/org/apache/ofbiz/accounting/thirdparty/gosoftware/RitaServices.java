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
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.CreditCardDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PosTerminalStateDao;
import org.apache.ofbiz.persistence.entity.CreditCardEntity;
import org.apache.ofbiz.persistence.entity.PosTerminalStateEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.RitaServicesContext;
public class RitaServices {

    private static final String MODULE = RitaServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final String RES_ORDER = x.OrderUiLabels;

    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.invoice_rounding);

    public static Map<String, Object> ccAuth(DispatchContext dctx, RitaServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        Properties props = buildPccProperties(context, delegator);
        RitaApi api = getApi(props, x.CREDIT);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingRitaErrorGettingPaymentGatewayConfig, locale));
        }

        try {
            RitaServices.setCreditCardInfo(api, dctx.getDelegator(), context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // basic tx info
        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, x.processAmount));
        api.set(RitaApi.INVOICE, context.get(x.orderId));

        // command setting
        if (x._1.equals(props.getProperty(x.autoBill))) {
            // sale
            api.set(RitaApi.COMMAND, x.SALE);
        } else {
            // pre-auth
            api.set(RitaApi.COMMAND, x.PRE_AUTH);
        }

        // send the transaction
        RitaApi out = null;
        try {
            Debug.logInfo(x.Sending_request_to_RiTA, MODULE);
            out = api.send();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(RitaApi.RESULT);
        boolean passed = false;
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.authResult, Boolean.TRUE);
            result.put(x.captureResult, Boolean.TRUE);
            passed = true;
        } else if (x.APPROVED.equals(resultCode)) {
            result.put(x.authCode, out.get(RitaApi.AUTH_CODE));
            result.put(x.authResult, Boolean.TRUE);
            passed = true;
        } else if (x.PROCESSED.equals(resultCode)) {
            result.put(x.authResult, Boolean.TRUE);
        } else {
            result.put(x.authResult, Boolean.FALSE);
        }

        result.put(x.authRefNum, out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : x.emptyString);
        result.put(x.processAmount, context.get(x.processAmount));
        result.put(x.authCode, out.get(RitaApi.AUTH_CODE));
        result.put(x.authFlag, out.get(RitaApi.REFERENCE));
        result.put(x.authMessage, out.get(RitaApi.RESULT));
        result.put(x.cvCode, out.get(RitaApi.CVV2_CODE));
        result.put(x.avsCode, out.get(RitaApi.AVS_CODE));

        if (!passed) {
            String respMsg = out.get(RitaApi.RESULT) + x.str_0d0c4ddd + out.get(RitaApi.INTRN_SEQ_NUM);
            result.put(x.customerRespMsgs, UtilMisc.toList(respMsg));
        }

        if (result.get(x.captureResult) != null) {
            result.put(x.captureCode, out.get(RitaApi.AUTH_CODE));
            result.put(x.captureFlag, out.get(RitaApi.REFERENCE));
            result.put(x.captureRefNum, out.get(RitaApi.INTRN_SEQ_NUM));
            result.put(x.captureMessage, out.get(RitaApi.RESULT));
        }

        return result;
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, RitaServicesContext context) {
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

        // setup the RiTA Interface
        Properties props = buildPccProperties(context, delegator);
        RitaApi api = getApi(props, x.CREDIT);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingRitaErrorGettingPaymentGatewayConfig, locale));
        }

        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString(x.referenceNum));
        api.set(RitaApi.COMMAND, x.COMPLETION);

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(RitaApi.RESULT);
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.captureResult, Boolean.TRUE);
        } else {
            result.put(x.captureResult, Boolean.FALSE);
        }
        result.put(x.captureAmount, context.get(x.captureAmount));
        result.put(x.captureRefNum, out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : x.emptyString);
        result.put(x.captureCode, out.get(RitaApi.AUTH_CODE));
        result.put(x.captureFlag, out.get(RitaApi.REFERENCE));
        result.put(x.captureMessage, out.get(RitaApi.RESULT));

        return result;
    }

    public static Map<String, Object> ccVoidRelease(DispatchContext dctx, RitaServicesContext context) {
        return ccVoid(dctx, context, false);
    }

    public static Map<String, Object> ccVoidRefund(DispatchContext dctx, RitaServicesContext context) {
        return ccVoid(dctx, context, true);
    }

    private static Map<String, Object> ccVoid(DispatchContext dctx, RitaServicesContext context, boolean isRefund) {
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
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        // setup the RiTA Interface
        Properties props = buildPccProperties(context, delegator);
        RitaApi api = getApi(props, x.CREDIT);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingRitaErrorGettingPaymentGatewayConfig, locale));
        }

        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, isRefund ? x.refundAmount : x.releaseAmount));
        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString(x.referenceNum));
        api.set(RitaApi.COMMAND, x.VOID);

        // check to make sure we are configured for SALE mode
        if (!x._1.equals(props.getProperty(x.autoBill))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingRitaCannotSupportReleasingPreAuth, locale));
        }

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(RitaApi.RESULT);
        if (x.VOIDED.equals(resultCode)) {
            result.put(isRefund ? x.refundResult : x.releaseResult, Boolean.TRUE);
        } else {
            result.put(isRefund ? x.refundResult : x.releaseResult, Boolean.FALSE);
        }
        result.put(isRefund ? x.refundAmount : x.releaseAmount,
                context.get(isRefund ? x.refundAmount : x.releaseAmount));
        result.put(isRefund ? x.refundRefNum : x.releaseRefNum,
                out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : x.emptyString);
        result.put(isRefund ? x.refundCode : x.releaseCode, out.get(RitaApi.AUTH_CODE));
        result.put(isRefund ? x.refundFlag : x.releaseFlag, out.get(RitaApi.REFERENCE));
        result.put(isRefund ? x.refundMessage : x.releaseMessage, out.get(RitaApi.RESULT));

        return result;
    }

    public static Map<String, Object> ccCreditRefund(DispatchContext dctx, RitaServicesContext context) {
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
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        // setup the RiTA Interface
        Properties props = buildPccProperties(context, delegator);
        RitaApi api = getApi(props, x.CREDIT);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingRitaErrorGettingPaymentGatewayConfig, locale));
        }

        // set the required cc info
        try {
            RitaServices.setCreditCardInfo(api, dctx.getDelegator(), context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, x.refundAmount));
        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString(x.referenceNum));
        api.set(RitaApi.COMMAND, x.CREDIT);

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        String resultCode = out.get(RitaApi.RESULT);
        if (x.CAPTURED.equals(resultCode)) {
            result.put(x.refundResult, Boolean.TRUE);
        } else {
            result.put(x.refundResult, Boolean.FALSE);
        }
        result.put(x.refundAmount, context.get(x.refundAmount));
        result.put(x.refundRefNum, out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : x.emptyString);
        result.put(x.refundCode, out.get(RitaApi.AUTH_CODE));
        result.put(x.refundFlag, out.get(RitaApi.REFERENCE));
        result.put(x.refundMessage, out.get(RitaApi.RESULT));

        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, RitaServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderHeader = null;
        try {
            orderHeader = orderPaymentPreference.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderPaymentPreference.getString(x.orderId)), locale));
        }

        if (orderHeader != null) {
            String terminalId = orderHeader.getString(x.terminalId);
            boolean isVoid = false;
            if (terminalId != null) {
                Timestamp orderDate = orderHeader.getTimestamp(x.orderDate);
                PosTerminalStateEntity terminalState = null;
                try {
                    PosTerminalStateDao posTerminalStateDao = DaoRegistry.getDao(delegator, x.PosTerminalState, PosTerminalStateDao.class);
                    List<PosTerminalStateEntity> terminalStates = posTerminalStateDao.list(Filters.eq(x.posTerminalId, terminalId));
                    Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
                    for (PosTerminalStateEntity candidateState : terminalStates) {
                        Timestamp openedDate = candidateState.getOpenedDate();
                        Timestamp closedDate = candidateState.getClosedDate();
                        if ((closedDate == null || closedDate.after(nowTimestamp))
                                && (openedDate == null || openedDate.before(nowTimestamp) || openedDate.equals(nowTimestamp))) {
                            terminalState = candidateState;
                            break;
                        }
                    }
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                }

                // this is the current opened terminal
                if (terminalState != null) {
                    Timestamp openDate = terminalState.getOpenedDate();
                    // if the order date is after the open date of the current state
                    // the order happend within the current open/close of the terminal
                    if (orderDate.after(openDate)) {
                        isVoid = true;
                    }
                }
            }

            Map<String, Object> refundResp = null;
            try {
                if (isVoid) {
                    refundResp = dispatcher.runSync(x.ritaCCVoidRefund, context);
                } else {
                    refundResp = dispatcher.runSync(x.ritaCCCreditRefund, context);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                        x.AccountingRitaErrorServiceException, locale));
            }
            return refundResp;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderPaymentPreference.getString(x.orderId)), locale));
    }

    private static void setCreditCardInfo(RitaApi api, Delegator delegator, RitaServicesContext context) throws GeneralException {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        if (creditCard == null) {
            try {
                CreditCardDao creditCardDao = DaoRegistry.getDao(delegator, x.CreditCard, CreditCardDao.class);
                CreditCardEntity creditCardEntity = creditCardDao.get(orderPaymentPreference.getString(x.paymentMethodId)).orElse(null);
                if (creditCardEntity != null) {
                    creditCard = delegator.makeValue(x.CreditCard, Beans.beanToMap(creditCardEntity));
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }
        if (creditCard != null) {
            List<String> expDateList = StringUtil.split(creditCard.getString(x.expireDate), x.str_42099b4a);
            String month = expDateList.get(0);
            String year = expDateList.get(1);
            String y2d = year.substring(2);

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
            String acctNumber = creditCard.getString(x.cardNumber);
            String cvNum = (String) context.get(x.cardSecurityCode);

            api.set(RitaApi.ACCT_NUM, acctNumber);
            api.set(RitaApi.EXP_MONTH, month);
            api.set(RitaApi.EXP_YEAR, y2d);
            api.set(RitaApi.CARDHOLDER, nameOnCard);
            if (UtilValidate.isNotEmpty(cvNum)) {
                api.set(RitaApi.CVV2, cvNum);
            }

            // billing address information
            GenericValue billingAddress = (GenericValue) context.get(x.billingAddress);
            if (billingAddress != null) {
                api.set(RitaApi.CUSTOMER_STREET, billingAddress.getString(x.address1));
                api.set(RitaApi.CUSTOMER_ZIP, billingAddress.getString(x.postalCode));
            } else {
                String zipCode = orderPaymentPreference.getString(x.billingPostalCode);
                if (UtilValidate.isNotEmpty(zipCode)) {
                    api.set(RitaApi.CUSTOMER_ZIP, zipCode);
                }
            }

            // set the present flag
            String presentFlag = orderPaymentPreference.getString(x.presentFlag);
            if (presentFlag == null) {
                presentFlag = x.N;
            }
            api.set(RitaApi.PRESENT_FLAG, x.Y.equals(presentFlag) ? x._3 : x._1); // 1, no present, 2 present, 3 swiped
        } else {
            throw new GeneralException(x.No_CreditCard_object_found);
        }
    }

    private static RitaApi getApi(Properties props) {
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
        boolean ssl = x.Y.equals(props.getProperty(x.ssl, x.N));

        RitaApi api = null;
        if (port > 0 && host != null) {
            api = new RitaApi(host, port, ssl);
        } else {
            api = new RitaApi();
        }

        api.set(RitaApi.CLIENT_ID, props.getProperty(x.clientID));
        api.set(RitaApi.USER_ID, props.getProperty(x.userID));
        api.set(RitaApi.USER_PW, props.getProperty(x.userPW));
        api.set(RitaApi.FORCE_FLAG, props.getProperty(x.forceTx));

        return api;
    }

    private static RitaApi getApi(Properties props, String paymentType) {
        RitaApi api = getApi(props);
        api.set(RitaApi.FUNCTION_TYPE, x.PAYMENT_6e5eb56a);
        api.set(RitaApi.PAYMENT_TYPE, paymentType);
        return api;
    }

    private static Properties buildPccProperties(RitaServicesContext context, Delegator delegator) {
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }

        String clientId = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_clientID, delegator);
        String userId = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_userID, delegator);
        String userPw = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_userPW, delegator);
        String host = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_host, delegator);
        String port = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_port, delegator);
        String ssl = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_ssl, x.N, delegator);
        String autoBill = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_autoBill, x._0, delegator);
        String forceTx = EntityUtilProperties.getPropertyValue(configString, x.payment_rita_forceTx, x._0, delegator);

        // some property checking
        if (UtilValidate.isEmpty(clientId)) {
            Debug.logWarning(x.The_clientID_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }
        if (UtilValidate.isEmpty(userId)) {
            Debug.logWarning(x.The_userID_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }
        if (UtilValidate.isEmpty(userPw)) {
            Debug.logWarning(x.The_userPW_property_in + configString + x.is_not_configured_1531478c, MODULE);
            return null;
        }

        // create some properties for CS Client
        Properties props = new Properties();
        props.put(x.clientID, clientId);
        props.put(x.userID, userId);
        props.put(x.userPW, userPw);
        props.put(x.host, host);
        props.put(x.port, port);
        props.put(x.ssl, ssl);
        props.put(x.autoBill, autoBill);
        props.put(x.forceTx, forceTx);
        Debug.logInfo(x.Returning_properties + props, MODULE);

        return props;
    }

    private static String getAmountString(RitaServicesContext context, String amountField) {
        BigDecimal processAmount = (BigDecimal) context.get(amountField);
        return processAmount.setScale(DECIMALS, ROUNDING).toPlainString();
    }

}
