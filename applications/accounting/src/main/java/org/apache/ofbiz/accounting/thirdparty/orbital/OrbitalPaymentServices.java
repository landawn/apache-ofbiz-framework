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
package org.apache.ofbiz.accounting.thirdparty.orbital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewayOrbitalDao;
import org.apache.ofbiz.persistence.dao.TrackingCodeOrderDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewayOrbitalEntity;
import org.apache.ofbiz.persistence.entity.TrackingCodeOrderEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;
import com.paymentech.orbital.sdk.configurator.Configurator;
import com.paymentech.orbital.sdk.interfaces.RequestIF;
import com.paymentech.orbital.sdk.interfaces.ResponseIF;
import com.paymentech.orbital.sdk.interfaces.TransactionProcessorIF;
import com.paymentech.orbital.sdk.request.FieldNotFoundException;
import com.paymentech.orbital.sdk.request.Request;
import com.paymentech.orbital.sdk.transactionProcessor.TransactionException;
import com.paymentech.orbital.sdk.transactionProcessor.TransactionProcessor;
import com.paymentech.orbital.sdk.util.exceptions.InitializationException;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.OrbitalPaymentServicesContext;
public class OrbitalPaymentServices {

    private static final String MODULE = OrbitalPaymentServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final String ERROR = x.Error;

    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.invoice_rounding);


    public static final String BIN_VALUE = x._000002;
    public static TransactionProcessorIF tp = null;
    public static ResponseIF response = null;
    public static RequestIF request = null;

    public static Map<String, Object> ccAuth(DispatchContext ctx, OrbitalPaymentServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        props.put(x.transType, x.AUTH_ONLY);
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, x.Error_in_request_initialization, MODULE);
        }
        buildAuthOrAuthCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, x.Validation_Failed_invalid_values);
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get(x.processCardResponse));
        processAuthTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccAuthCapture(DispatchContext ctx, OrbitalPaymentServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        props.put(x.transType, x.AUTH_CAPTURE);
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, x.Error_in_request_initialization, MODULE);
        }
        buildAuthOrAuthCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, x.Validation_Failed_invalid_values);
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get(x.processCardResponse));
        processAuthCaptureTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, OrbitalPaymentServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne(x.CreditCard, false);
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
        context.put(x.orderId, orderPaymentPreference.getString(x.orderId));

        props.put(x.transType, x.PRIOR_AUTH_CAPTURE);
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.MARK_FOR_CAPTURE_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, x.Error_in_request_initialization, MODULE);
        }
        buildCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, x.Validation_Failed_invalid_values);
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get(x.processCardResponse));
        processCaptureTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, OrbitalPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne(x.CreditCard, false);
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
        context.put(x.orderId, orderPaymentPreference.getString(x.orderId));

        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, x.Error_in_request_initialization, MODULE);
        }
        buildRefundTransaction(context, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, x.Validation_Failed_invalid_values);
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get(x.processCardResponse));
        processRefundTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, OrbitalPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);

        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        try {
            orderPaymentPreference.getRelatedOne(x.CreditCard, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentUnableToGetCCInfo, locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }
        context.put(x.authTransaction, authTransaction);
        context.put(x.orderId, orderPaymentPreference.getString(x.orderId));

        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.REVERSE_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, x.Error_in_request_initialization, MODULE);
        }
        buildReleaseTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, x.Validation_Failed_invalid_values);
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get(x.processCardResponse));
        processReleaseTransResult(processCardResponseContext, results);
        return results;
    }


    private static Map<String, Object> buildOrbitalProperties(OrbitalPaymentServicesContext context, Delegator delegator) {
        //TODO: Will move this to property file and then will read it from there.
        String configFile = x.applications_accounting_config_linehandler_properties;
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        Map<String, Object> buildConfiguratorContext = new HashMap<>();
        try {
            buildConfiguratorContext.put(x.OrbitalConnectionUsername, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.username));
            buildConfiguratorContext.put(x.OrbitalConnectionPassword, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.connectionPassword));
            buildConfiguratorContext.put(x.merchantId, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantId));
            buildConfiguratorContext.put(x.engine_class, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.engineClass));
            buildConfiguratorContext.put(x.engine_hostname, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.hostName));
            buildConfiguratorContext.put(x.engine_port, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.port));
            buildConfiguratorContext.put(x.engine_hostname_failover, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.hostNameFailover));
            buildConfiguratorContext.put(x.engine_port_failover, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.portFailover));
            buildConfiguratorContext.put(x.engine_connection_timeout_seconds, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.connectionTimeoutSeconds));
            buildConfiguratorContext.put(x.engine_read_timeout_seconds, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.readTimeoutSeconds));
            buildConfiguratorContext.put(x.engine_authorizationURI, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.authorizationURI));
            buildConfiguratorContext.put(x.engine_sdk_version, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.sdkVersion));
            buildConfiguratorContext.put(x.engine_ssl_socketfactory, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.sslSocketFactory));
            buildConfiguratorContext.put(x.Response_response_type, getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.responseType));
            String configFileLocation = System.getProperty(x.ofbiz_home) + configFile;
            Configurator config = Configurator.getInstance(configFileLocation);
            buildConfiguratorContext.putAll(config.getConfigurations());
            config.setConfigurations(buildConfiguratorContext);
        } catch (InitializationException e) {
            Debug.logError(e, x.Orbital_Configurator_Initialization_Error + e.getMessage(), MODULE);
        }
        return buildConfiguratorContext;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId,
            String paymentGatewayConfigParameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                PaymentGatewayOrbitalDao paymentGatewayOrbitalDao = DaoRegistry.getDao(delegator, x.PaymentGatewayOrbital, PaymentGatewayOrbitalDao.class);
                PaymentGatewayOrbitalEntity paymentGatewayOrbitalEntity = paymentGatewayOrbitalDao.get(paymentGatewayConfigId).orElse(null);
                GenericValue paymentGatewayOrbital = paymentGatewayOrbitalEntity == null ? null
                        : delegator.makeValue(x.PaymentGatewayOrbital, Beans.beanToMap(paymentGatewayOrbitalEntity));
                if (paymentGatewayOrbital != null) {
                    Object paymentGatewayOrbitalField = paymentGatewayOrbital.get(paymentGatewayConfigParameterName);
                    if (paymentGatewayOrbitalField != null) {
                        return returnValue = paymentGatewayOrbitalField.toString().trim();
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }
        return returnValue;
    }

    private static void buildAuthOrAuthCaptureTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue cc = (GenericValue) params.get(x.creditCard);
        BigDecimal amount = (BigDecimal) params.get(x.processAmount);
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String number = UtilFormatOut.checkNull(cc.getString(x.cardNumber));
        String expDate = UtilFormatOut.checkNull(cc.getString(x.expireDate));
        expDate = formatExpDateForOrbital(expDate);
        String cardSecurityCode = (String) params.get(x.cardSecurityCode);
        String orderId = UtilFormatOut.checkNull((String) params.get(x.orderId));
        String transType = props.get(x.transType).toString();
        String messageType = null;
        if (x.AUTH_ONLY.equals(transType)) {
            messageType = x.A;
        } else if (x.AUTH_CAPTURE.equals(transType)) {
            messageType = x.AC;
        }
        try {
            request.setFieldValue(x.IndustryType, x.EC);
            request.setFieldValue(x.MessageType, UtilFormatOut.checkNull(messageType));
            request.setFieldValue(x.MerchantID, UtilFormatOut.checkNull(props.get(x.merchantId).toString()));
            request.setFieldValue(x.BIN, BIN_VALUE);
            request.setFieldValue(x.OrderID, UtilFormatOut.checkNull(orderId));
            request.setFieldValue(x.AccountNum, UtilFormatOut.checkNull(number));

            request.setFieldValue(x.Amount, UtilFormatOut.checkNull(amountValue));
            request.setFieldValue(x.Exp, UtilFormatOut.checkNull(expDate));
            // AVS Information
            GenericValue creditCard = null;
            if (params.get(x.orderPaymentPreference) != null) {
                GenericValue opp = (GenericValue) params.get(x.orderPaymentPreference);
                if (x.CREDIT_CARD.equals(opp.getString(x.paymentMethodTypeId))) {
                    // sometimes the ccAuthCapture interface is used, in which case the creditCard is passed directly
                     creditCard = (GenericValue) params.get(x.creditCard);
                    if (creditCard == null || !(opp.get(x.paymentMethodId).equals(creditCard.get(x.paymentMethodId)))) {
                        creditCard = opp.getRelatedOne(x.CreditCard, false);
                    }
                }

                request.setFieldValue(x.AVSname, x.Demo_Customer);
                if (UtilValidate.isNotEmpty(creditCard.getString(x.contactMechId))) {
                    GenericValue address = creditCard.getRelatedOne(x.PostalAddress, false);
                    if (address != null) {
                        request.setFieldValue(x.AVSaddress1, UtilFormatOut.checkNull(address.getString(x.address1)));
                        request.setFieldValue(x.AVScity, UtilFormatOut.checkNull(address.getString(x.city)));
                        request.setFieldValue(x.AVSstate, UtilFormatOut.checkNull(address.getString(x.stateProvinceGeoId)));
                        request.setFieldValue(x.AVSzip, UtilFormatOut.checkNull(address.getString(x.postalCode)));
                    }
                }
            } else {
                // this would be the case for an authorization
                GenericValue cp = (GenericValue) params.get(x.billToParty);
                GenericValue ba = (GenericValue) params.get(x.billingAddress);
                request.setFieldValue(x.AVSname, UtilFormatOut.checkNull(cp.getString(x.firstName)) + UtilFormatOut.checkNull(cp.getString(x.lastName)));
                request.setFieldValue(x.AVSaddress1, UtilFormatOut.checkNull(ba.getString(x.address1)));
                request.setFieldValue(x.AVScity, UtilFormatOut.checkNull(ba.getString(x.city)));
                request.setFieldValue(x.AVSstate, UtilFormatOut.checkNull(ba.getString(x.stateProvinceGeoId)));
                request.setFieldValue(x.AVSzip, UtilFormatOut.checkNull(ba.getString(x.postalCode)));
                request.setFieldValue(x.AVSCountryCode, UtilFormatOut.checkNull(ba.getString(x.countryGeoId)));
            }
            // Additional Information
            request.setFieldValue(x.Comments, x.This_is_building_of_request_object);
            String shippingRef = getShippingRefForOrder(orderId, delegator);
            request.setFieldValue(x.ShippingRef, shippingRef);
            request.setFieldValue(x.CardSecVal, UtilFormatOut.checkNull(cardSecurityCode));

            //Display the request
            if (x.AUTH_ONLY.equals(transType)) {
                Debug.logInfo(x.Auth_Request + request.getXML());
            } else if (x.AUTH_CAPTURE.equals(transType)) {
                Debug.logInfo(x.Auth_Capture_Request + request.getXML());
            }
            results.put(x.processAmount, amount);
        } catch (InitializationException ie) {
            Debug.logInfo(x.Unable_to_initialize_request_object, MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError(x.Unable_to_find_XML_field_in_template, MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildCaptureTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue authTransaction = (GenericValue) params.get(x.authTransaction);
        GenericValue creditCard = (GenericValue) params.get(x.creditCard);
        BigDecimal amount = (BigDecimal) params.get(x.captureAmount);
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String orderId = UtilFormatOut.checkNull((String) params.get(x.orderId));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue(x.MerchantID, UtilFormatOut.checkNull(props.get(x.merchantId).toString()));
            request.setFieldValue(x.BIN, BIN_VALUE);
            request.setFieldValue(x.TxRefNum, UtilFormatOut.checkNull(authTransaction.get(x.referenceNum).toString()));
            request.setFieldValue(x.OrderID, UtilFormatOut.checkNull(orderId));
            request.setFieldValue(x.Amount, UtilFormatOut.checkNull(amountValue));

            request.setFieldValue(x.PCDestName, UtilFormatOut.checkNull(creditCard.getString(x.firstNameOnCard) + creditCard.getString(x.lastNameOnCard)));
            if (UtilValidate.isNotEmpty(creditCard.getString(x.contactMechId))) {
                GenericValue address = creditCard.getRelatedOne(x.PostalAddress, false);
                if (address != null) {
                    request.setFieldValue(x.PCOrderNum, UtilFormatOut.checkNull(orderId));
                    request.setFieldValue(x.PCDestAddress1, UtilFormatOut.checkNull(address.getString(x.address1)));
                    request.setFieldValue(x.PCDestAddress2, UtilFormatOut.checkNull(address.getString(x.address2)));
                    request.setFieldValue(x.PCDestCity, UtilFormatOut.checkNull(address.getString(x.city)));
                    request.setFieldValue(x.PCDestState, UtilFormatOut.checkNull(address.getString(x.stateProvinceGeoId)));
                    request.setFieldValue(x.PCDestZip, UtilFormatOut.checkNull(address.getString(x.postalCode)));
                }
            }
            //Display the request
            Debug.logInfo(x.Capture_Request + request.getXML());
            results.put(x.captureAmount, amount);
        } catch (InitializationException ie) {
            Debug.logInfo(x.Unable_to_initialize_request_object, MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError(x.Unable_to_find_XML_field_in_template + fnfe.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildRefundTransaction(Map<String, Object> params, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue cc = (GenericValue) params.get(x.creditCard);
        BigDecimal amount = (BigDecimal) params.get(x.refundAmount);
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String number = UtilFormatOut.checkNull(cc.getString(x.cardNumber));
        String expDate = UtilFormatOut.checkNull(cc.getString(x.expireDate));
        expDate = formatExpDateForOrbital(expDate);
        String orderId = UtilFormatOut.checkNull((String) params.get(x.orderId));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue(x.IndustryType, x.EC);
            request.setFieldValue(x.MessageType, x.R);
            request.setFieldValue(x.MerchantID, UtilFormatOut.checkNull(props.get(x.merchantId).toString()));
            request.setFieldValue(x.BIN, BIN_VALUE);
            request.setFieldValue(x.OrderID, UtilFormatOut.checkNull(orderId));
            request.setFieldValue(x.AccountNum, UtilFormatOut.checkNull(number));
            request.setFieldValue(x.Amount, UtilFormatOut.checkNull(amountValue));
            request.setFieldValue(x.Exp, UtilFormatOut.checkNull(expDate));
            request.setFieldValue(x.Comments, x.This_is_a_credit_card_refund);

            Debug.logInfo(x.Refund_Request + request.getXML());
            results.put(x.refundAmount, amount);
        } catch (InitializationException ie) {
            Debug.logInfo(x.Unable_to_initialize_request_object, MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError(x.Unable_to_find_XML_field_in_template, MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildReleaseTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        BigDecimal amount = (BigDecimal) params.get(x.releaseAmount);
        GenericValue authTransaction = (GenericValue) params.get(x.authTransaction);
        String orderId = UtilFormatOut.checkNull((String) params.get(x.orderId));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue(x.MerchantID, UtilFormatOut.checkNull(props.get(x.merchantId).toString()));
            request.setFieldValue(x.BIN, BIN_VALUE);
            request.setFieldValue(x.TxRefNum, UtilFormatOut.checkNull(authTransaction.get(x.referenceNum).toString()));
            request.setFieldValue(x.OrderID, UtilFormatOut.checkNull(orderId));

            //Display the request
            Debug.logInfo(x.Release_Request + request.getXML());
            results.put(x.releaseAmount, amount);
        } catch (InitializationException ie) {
            Debug.logInfo(x.Unable_to_initialize_request_object, MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError(x.Unable_to_find_XML_field_in_template + fnfe.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }


    private static void initializeTransactionProcessor() {
        //Create a Transaction Processor
        //The Transaction Processor acquires and releases resources and executes transactions.
        //It configures a pool of protocol engines, then uses the pool to execute transactions.
        try {
            tp = new TransactionProcessor();
        } catch (InitializationException iex) {
            Debug.logError(x.TransactionProcessor_failed_to_initialize + iex.getMessage(), MODULE);
            iex.printStackTrace();
        }
    }

    private static Map<String, Object> processCard(RequestIF request) {
        Map<String, Object> processCardResult = new HashMap<>();
        try {
            response = tp.process(request);
            if (response.isApproved()) {
                processCardResult.put(x.authResult, Boolean.TRUE);
            } else {
                processCardResult.put(x.authResult, Boolean.FALSE);
            }
            processCardResult.put(x.processCardResponse, response);
        } catch (TransactionException tex) {
            Debug.logError(x.TransactionProcessor_failed_to_initialize + tex.getMessage(), MODULE);
            tex.printStackTrace();
        }
        processCardResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return processCardResult;
    }

    private static void processAuthTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get(x.processCardResponse);
        Boolean authResult = (Boolean) processCardResponseContext.get(x.authResult);
        results.put(x.authResult, authResult);
        results.put(x.authFlag, response.getResponseCode());
        results.put(x.authMessage, response.getMessage());
        if (authResult) { //passed
            results.put(x.authCode, response.getAuthCode());
            results.put(x.authRefNum, response.getTxRefNum());
            results.put(x.cvCode, UtilFormatOut.checkNull(response.getCVV2RespCode()));
            results.put(x.avsCode, response.getAVSResponseCode());
            results.put(x.processAmount, new BigDecimal(results.get(x.processAmount).toString()));
        } else {
            results.put(x.authCode, response.getAuthCode());
            results.put(x.processAmount, BigDecimal.ZERO);
            results.put(x.authRefNum, OrbitalPaymentServices.ERROR);
        }
        Debug.logInfo(x.processAuthTransResult + results.toString(), MODULE);
    }

    private static void processAuthCaptureTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get(x.processCardResponse);
        Boolean authResult = (Boolean) processCardResponseContext.get(x.authResult);
        results.put(x.authResult, authResult);
        results.put(x.authFlag, response.getResponseCode());
        results.put(x.authMessage, response.getMessage());
        results.put(x.captureResult, authResult);
        results.put(x.captureFlag, response.getResponseCode());
        results.put(x.captureMessage, response.getMessage());
        results.put(x.captureRefNum, response.getTxRefNum());
        if (authResult) { //passed
            results.put(x.authCode, response.getAuthCode());
            results.put(x.authRefNum, response.getTxRefNum());
            results.put(x.cvCode, UtilFormatOut.checkNull(response.getCVV2RespCode()));
            results.put(x.avsCode, response.getAVSResponseCode());
            results.put(x.processAmount, new BigDecimal(results.get(x.processAmount).toString()));
        } else {
            results.put(x.authCode, response.getAuthCode());
            results.put(x.processAmount, BigDecimal.ZERO);
            results.put(x.authRefNum, OrbitalPaymentServices.ERROR);
        }
        Debug.logInfo(x.processAuthCaptureTransResult + results.toString(), MODULE);
    }

    private static void processCaptureTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get(x.processCardResponse);
        Boolean captureResult = (Boolean) processCardResponseContext.get(x.authResult);
        results.put(x.captureResult, captureResult);
        results.put(x.captureFlag, response.getResponseCode());
        results.put(x.captureMessage, response.getMessage());
        results.put(x.captureRefNum, response.getTxRefNum());
        if (captureResult) { //passed
            results.put(x.captureCode, response.getAuthCode());
            results.put(x.captureAmount, new BigDecimal(results.get(x.captureAmount).toString()));
        } else {
            results.put(x.captureAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.processCaptureTransResult + results.toString(), MODULE);
    }

    private static void processRefundTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get(x.processCardResponse);
        Boolean refundResult = (Boolean) processCardResponseContext.get(x.authResult);
        results.put(x.refundResult, refundResult);
        results.put(x.refundFlag, response.getResponseCode());
        results.put(x.refundMessage, response.getMessage());
        results.put(x.refundRefNum, response.getTxRefNum());
        if (refundResult) { //passed
            results.put(x.refundCode, response.getAuthCode());
            results.put(x.refundAmount, new BigDecimal(results.get(x.refundAmount).toString()));
        } else {
            results.put(x.refundAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.processRefundTransResult + results.toString(), MODULE);
    }

    private static void processReleaseTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get(x.processCardResponse);
        Boolean releaseResult = (Boolean) processCardResponseContext.get(x.authResult);
        results.put(x.releaseResult, releaseResult);
        results.put(x.releaseFlag, response.getResponseCode());
        results.put(x.releaseMessage, response.getMessage());
        results.put(x.releaseRefNum, response.getTxRefNum());
        if (releaseResult) { //passed
            results.put(x.releaseCode, response.getAuthCode());
            results.put(x.releaseAmount, new BigDecimal(results.get(x.releaseAmount).toString()));
        } else {
            results.put(x.releaseAmount, BigDecimal.ZERO);
        }
        Debug.logInfo(x.processReleaseTransResult + results.toString(), MODULE);
    }

    private static void printTransResult(ResponseIF response) {
        Map<String, Object> generatedResponse = new HashMap<>();
        generatedResponse.put(x.isGood, response.isGood());
        generatedResponse.put(x.isError, response.isError());
        generatedResponse.put(x.isQuickResponse, response.isQuickResponse());
        generatedResponse.put(x.isApproved, response.isApproved());
        generatedResponse.put(x.isDeclined, response.isDeclined());
        generatedResponse.put(x.AuthCode, response.getAuthCode());
        generatedResponse.put(x.TxRefNum, response.getTxRefNum());
        generatedResponse.put(x.ResponseCode, response.getResponseCode());
        generatedResponse.put(x.Status_bae7d5be, response.getStatus());
        generatedResponse.put(x.Message, response.getMessage());
        generatedResponse.put(x.AVSCode, response.getAVSResponseCode());
        generatedResponse.put(x.CVV2ResponseCode, response.getCVV2RespCode());

        Debug.logInfo(x.printTransResult + generatedResponse.toString(), MODULE);
    }

    private static String formatExpDateForOrbital(String expDate) {
        String formatedDate = expDate.substring(0, 2) + expDate.substring(5);
        return formatedDate;
    }

    private static String getShippingRefForOrder(String orderId, Delegator delegator) {
        String shippingRef = x.emptyString;
        try {
            TrackingCodeOrderDao trackingCodeOrderDao = DaoRegistry.getDao(delegator, x.TrackingCodeOrder, TrackingCodeOrderDao.class);
            TrackingCodeOrderEntity trackingCodeOrderEntity = trackingCodeOrderDao.list(Filters.eq(x.orderId, orderId)).stream().findFirst().orElse(null);
            GenericValue trackingCodeOrder = trackingCodeOrderEntity == null ? null
                    : delegator.makeValue(x.TrackingCodeOrder, Beans.beanToMap(trackingCodeOrderEntity));
            GenericValue trackingCode = null;
            if (trackingCodeOrder != null) {
                trackingCode = trackingCodeOrder.getRelatedOne(x.TrackingCode, false);
            }
            if (trackingCode != null && UtilValidate.isNotEmpty(trackingCode.getString(x.description))) {
                // get tracking code description and provide it into shipping reference.
                shippingRef = trackingCode.getString(x.trackingCodeId) + x.str_17ad2cb1 + trackingCode.getString(x.description);
            } else {
                shippingRef = x.No_Tracking_Info_processed_in_order;
            }
        } catch (Exception e) {
            Debug.logError(x.Shipping_Ref_not_found_returning_empty_string, MODULE);
            Debug.logError(e, MODULE);
        }
        return shippingRef;
    }

    private static Map<String, Object> validateRequest(Map<String, Object> params, Map props, RequestIF request) {
        Map<String, Object> result = new HashMap<>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
