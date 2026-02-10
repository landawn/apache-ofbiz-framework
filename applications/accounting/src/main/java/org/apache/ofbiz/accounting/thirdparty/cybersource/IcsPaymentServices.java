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
package org.apache.ofbiz.accounting.thirdparty.cybersource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.SSLUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewayCyberSourceDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewayCyberSourceEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.cybersource.ws.client.Client;
import com.cybersource.ws.client.ClientException;
import com.cybersource.ws.client.FaultException;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.IcsPaymentServicesContext;
/**
 * CyberSource WS Integration Services
 */
public class IcsPaymentServices {

    private static final String MODULE = IcsPaymentServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.invoice_rounding);

    // load the JSSE properties
    static {
        SSLUtil.loadJsseProperties();
    }

    public static Map<String, Object> ccAuth(DispatchContext dctx, IcsPaymentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        // generate the request/properties
        Properties props = buildCsProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorGettingPaymentGatewayConfig, locale));
        }

        Map<String, Object> request = buildAuthRequest(context, delegator);
        request.put(x.merchantID, props.get(x.merchantID));

        // transmit the request
        Map<String, Object> reply;
        try {
            reply = UtilGenerics.cast(Client.runTransaction(request, props));
        } catch (FaultException e) {
            Debug.logError(e, x.ERROR_Fault_from_CyberSource, MODULE);
            Debug.logError(e, x.Fault + e.getFaultString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        } catch (ClientException e) {
            Debug.logError(e, x.ERROR_CyberSource_Client_exception + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        }
        // process the reply
        Map<String, Object> result = ServiceUtil.returnSuccess();
        processAuthResult(reply, result, delegator);
        return result;
    }

    public static Map<String, Object> ccReAuth(DispatchContext dctx, IcsPaymentServicesContext context) {
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, IcsPaymentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get(x.authTrans);
        Locale locale = (Locale) context.get(x.locale);
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotCapture, locale));
        }
        // generate the request/properties
        Properties props = buildCsProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorGettingPaymentGatewayConfig, locale));
        }

        Map<String, Object> request = buildCaptureRequest(context, authTransaction, delegator);
        request.put(x.merchantID, props.get(x.merchantID));

        // transmit the request
        Map<String, Object> reply;
        try {
            reply = UtilGenerics.cast(Client.runTransaction(request, props));
        } catch (FaultException e) {
            Debug.logError(e, x.ERROR_Fault_from_CyberSource, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        } catch (ClientException e) {
            Debug.logError(e, x.ERROR_CyberSource_Client_exception + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        }
        // process the reply
        Map<String, Object> result = ServiceUtil.returnSuccess();
        processCaptureResult(reply, result);
        return result;
    }

    public static Map<String, Object> ccRelease(DispatchContext dctx, IcsPaymentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        // generate the request/properties
        Properties props = buildCsProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorGettingPaymentGatewayConfig, locale));
        }

        Map<String, Object> request = buildReleaseRequest(context, authTransaction);
        request.put(x.merchantID, props.get(x.merchantID));

        // transmit the request
        Map<String, Object> reply;
        try {
            reply = UtilGenerics.cast(Client.runTransaction(request, props));
        } catch (FaultException e) {
            Debug.logError(e, x.ERROR_Fault_from_CyberSource, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        } catch (ClientException e) {
            Debug.logError(e, x.ERROR_CyberSource_Client_exception + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        }
        // process the reply
        Map<String, Object> result = ServiceUtil.returnSuccess();
        processReleaseResult(reply, result);
        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, IcsPaymentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        // generate the request/properties
        Properties props = buildCsProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorGettingPaymentGatewayConfig, locale));
        }

        Map<String, Object> request = buildRefundRequest(context, authTransaction, delegator);
        request.put(x.merchantID, props.get(x.merchantID));

        // transmit the request
        Map<String, Object> reply;
        try {
            reply = UtilGenerics.cast(Client.runTransaction(request, props));
        } catch (FaultException e) {
            Debug.logError(e, x.ERROR_Fault_from_CyberSource, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        } catch (ClientException e) {
            Debug.logError(e, x.ERROR_CyberSource_Client_exception + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        }

        // process the reply
        Map<String, Object> result = ServiceUtil.returnSuccess();
        processRefundResult(reply, result);
        return result;
    }

    public static Map<String, Object> ccCredit(DispatchContext dctx, IcsPaymentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        // generate the request/properties
        Properties props = buildCsProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorGettingPaymentGatewayConfig, locale));
        }

        Map<String, Object> request = buildCreditRequest(context);
        request.put(x.merchantID, props.get(x.merchantID));

        // transmit the request
        Map<String, Object> reply;
        try {
            reply = UtilGenerics.cast(Client.runTransaction(request, props));
        } catch (FaultException e) {
            Debug.logError(e, x.ERROR_Fault_from_CyberSource, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        } catch (ClientException e) {
            Debug.logError(e, x.ERROR_CyberSource_Client_exception + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingCyberSourceErrorCommunicateWithCyberSource, locale));
        }

        // process the reply
        Map<String, Object> result = ServiceUtil.returnSuccess();
        processCreditResult(reply, result);
        return result;
    }

    private static Properties buildCsProperties(IcsPaymentServicesContext context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }
        String merchantId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantId, configString, x.payment_cybersource_merchantID);
        String targetApi = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.apiVersion, configString, x.payment_cybersource_api_version);
        String production = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.production, configString, x.payment_cybersource_production);
        String enableLog = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.logEnabled, configString, x.payment_cybersource_log);
        String logSize = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.logSize, configString, x.payment_cybersource_log_size);
        String logFile = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.logFile, configString, x.payment_cybersource_log_file);
        String logDir = FlexibleStringExpander.expandString(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.logDir, configString, x.payment_cybersource_log_dir), context);
        String keysDir = FlexibleStringExpander.expandString(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.keysDir, configString, x.payment_cybersource_keysDir), context);
        String keysFile = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.keysFile, configString, x.payment_cybersource_keysFile);
        // some property checking
        if (UtilValidate.isEmpty(merchantId)) {
            Debug.logWarning(x.The_merchantId_property_is_not_configured, MODULE);
            return null;
        }
        if (UtilValidate.isEmpty(keysDir)) {
            Debug.logWarning(x.The_keysDir_property_is_not_configured, MODULE);
            return null;
        }
        // create some properties for CS Client
        Properties props = new Properties();
        props.put(x.merchantID, merchantId);
        props.put(x.keysDirectory, keysDir);
        props.put(x.targetAPIVersion, targetApi);
        props.put(x.sendToProduction, production);
        props.put(x.enableLog, enableLog);
        props.put(x.logDirectory, logDir);
        props.put(x.logFilename, logFile);
        props.put(x.logMaximumSize, logSize);
        if (UtilValidate.isNotEmpty(keysFile)) {
            props.put(x.alternateKeyFilename, keysFile);
        }
        Debug.logInfo(x.Created_CyberSource_Properties + props, MODULE);
        return props;
    }

    private static Map<String, Object> buildAuthRequest(IcsPaymentServicesContext context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        String currency = (String) context.get(x.currency);
        if (configString == null) {
            configString = x.payment_properties;
        }
        // make the request map
        String capture = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.autoBill, configString, x.payment_cybersource_autoBill, x._false);
        String orderId = (String) context.get(x.orderId);
        Map<String, Object> request = new HashMap<>();
        request.put(x.ccAuthService_run, x._true);              // run auth service
        request.put(x.ccCaptureService_run, capture);          // run capture service (i.e. sale)
        request.put(x.merchantReferenceCode, orderId);         // set the order ref number
        request.put(x.purchaseTotals_currency, currency);      // set the order currency
        appendFullBillingInfo(request, context);               // add in all address info
        appendItemLineInfo(request, context, x.processAmount); // add in the item info
        appendAvsRules(request, context, delegator);           // add in the AVS flags and decline codes
        return request;
    }

    private static Map<String, Object> buildCaptureRequest(IcsPaymentServicesContext context, GenericValue authTransaction, Delegator delegator) {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        String currency = (String) context.get(x.currency);
        if (configString == null) {
            configString = x.payment_properties;
        }
        String merchantDesc = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantDescr, configString, x.payment_cybersource_merchantDescr, null);
        String merchantCont = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantContact, configString, x.payment_cybersource_merchantContact, null);
        Map<String, Object> request = new HashMap<>();
        request.put(x.ccCaptureService_run, x._true);
        request.put(x.ccCaptureService_authRequestID, authTransaction.getString(x.referenceNum));
        request.put(x.item_0_unitPrice, getAmountString(context, x.captureAmount));
        request.put(x.merchantReferenceCode, orderPaymentPreference.getString(x.orderId));
        request.put(x.purchaseTotals_currency, currency);

        // TODO: add support for verbal authorizations
        if (merchantDesc != null) {
            request.put(x.invoiceHeader_merchantDescriptor, merchantDesc);        // merchant description
        }
        if (merchantCont != null) {
            request.put(x.invoiceHeader_merchantDescriptorContact, merchantCont); // merchant contact info
        }
        return request;
    }

    private static Map<String, Object> buildReleaseRequest(IcsPaymentServicesContext context, GenericValue authTransaction) {
        Map<String, Object> request = new HashMap<>();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        String currency = (String) context.get(x.currency);
        request.put(x.ccAuthReversalService_run, x._true);
        request.put(x.ccAuthReversalService_authRequestID, authTransaction.getString(x.referenceNum));
        request.put(x.item_0_unitPrice, getAmountString(context, x.releaseAmount));
        request.put(x.merchantReferenceCode, orderPaymentPreference.getString(x.orderId));
        request.put(x.purchaseTotals_currency, currency);
        return request;
    }

    private static Map<String, Object> buildRefundRequest(IcsPaymentServicesContext context, GenericValue authTransaction, Delegator delegator) {
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }
        String currency = (String) context.get(x.currency);
        String merchantDesc = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantDescr, configString, x.payment_cybersource_merchantDescr, null);
        String merchantCont = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.merchantContact, configString, x.payment_cybersource_merchantContact, null);
        Map<String, Object> request = new HashMap<>();
        request.put(x.ccCreditService_run, x._true);
        request.put(x.ccCreditService_captureRequestID, authTransaction.getString(x.referenceNum));
        request.put(x.item_0_unitPrice, getAmountString(context, x.refundAmount));
        request.put(x.merchantReferenceCode, orderPaymentPreference.getString(x.orderId));
        request.put(x.purchaseTotals_currency, currency);
        if (merchantDesc != null) {
            request.put(x.invoiceHeader_merchantDescriptor, merchantDesc);        // merchant description
        }
        if (merchantCont != null) {
            request.put(x.invoiceHeader_merchantDescriptorContact, merchantCont); // merchant contact info
        }
        return request;
    }

    private static Map<String, Object> buildCreditRequest(IcsPaymentServicesContext context) {
        String refCode = (String) context.get(x.referenceCode);
        Map<String, Object> request = new HashMap<>();
        request.put(x.ccCreditService_run, x._true);            // run credit service
        request.put(x.merchantReferenceCode, refCode);         // set the ref number could be order id
        appendFullBillingInfo(request, context);               // add in all address info
        appendItemLineInfo(request, context, x.creditAmount);  // add in the item info
        return request;
    }

    private static void appendAvsRules(Map<String, Object> request, IcsPaymentServicesContext context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);
        String configString = (String) context.get(x.paymentConfig);
        if (configString == null) {
            configString = x.payment_properties;
        }
        String avsCodes = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.avsDeclineCodes, configString, x.payment_cybersource_avsDeclineCodes, null);
        GenericValue party = (GenericValue) context.get(x.billToParty);
        if (party != null) {
            GenericValue avsOverride = null;
            try {
                avsOverride = party.getDelegator().findOne(x.PartyIcsAvsOverride,
                        UtilMisc.toMap(x.partyId, party.getString(x.partyId)), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (avsOverride != null && avsOverride.get(x.avsDeclineString) != null) {
                String overrideString = avsOverride.getString(x.avsDeclineString);
                if (UtilValidate.isNotEmpty(overrideString)) {
                    avsCodes = overrideString;
                }
            }
        }
        if (UtilValidate.isNotEmpty(avsCodes)) {
            request.put(x.businessRules_declineAVSFlags, avsCodes);
        }
        String avsIgnore = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, x.ignoreAvs, configString, x.payment_cybersource_ignoreAvs, x._false);
        request.put(x.businessRules_ignoreAVS, avsIgnore);
    }

    private static void appendFullBillingInfo(Map<String, Object> request, IcsPaymentServicesContext context) {
        // contact info
        GenericValue email = (GenericValue) context.get(x.billToEmail);
        if (email != null) {
            request.put(x.billTo_email, email.getString(x.infoString));
        } else {
            Debug.logWarning(x.Email_not_defined_Cybersource_will_fail, MODULE);
        }
        // phone number seems to not be used; possibly only for reporting.

        // CC payment info
        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        if (creditCard != null) {
            List<String> expDateList = StringUtil.split(creditCard.getString(x.expireDate), x.str_42099b4a);
            request.put(x.billTo_firstName, creditCard.getString(x.firstNameOnCard));
            request.put(x.billTo_lastName, creditCard.getString(x.lastNameOnCard));
            request.put(x.card_accountNumber, creditCard.getString(x.cardNumber));
            request.put(x.card_expirationMonth, expDateList.get(0));
            request.put(x.card_expirationYear, expDateList.get(1));
        } else {
            Debug.logWarning(x.CreditCard_not_defined_Cybersource_will_fail, MODULE);
        }
        // CCV info
        String cvNum = (String) context.get(x.cardSecurityCode);
        String cvSet = UtilValidate.isEmpty(cvNum) ? x._1 : x._0;
        request.put(x.card_cvIndicator, cvSet);
        if (x._1.equals(cvNum)) {
            request.put(x.card_cvNumber, cvNum);
        }
        // payment contact info
        GenericValue billingAddress = (GenericValue) context.get(x.billingAddress);

        if (billingAddress != null) {
            request.put(x.billTo_street1, billingAddress.getString(x.address1));
            if (billingAddress.get(x.address2) != null) {
                request.put(x.billTo_street2, billingAddress.getString(x.address2));
            }
            request.put(x.billTo_city, billingAddress.getString(x.city));
            String bCountry = billingAddress.get(x.countryGeoId) != null ? billingAddress.getString(x.countryGeoId) : x.USA;
            request.put(x.billTo_country, bCountry);
            request.put(x.billTo_postalCode, billingAddress.getString(x.postalCode));
            if (billingAddress.get(x.stateProvinceGeoId) != null) {
                request.put(x.billTo_state, billingAddress.getString(x.stateProvinceGeoId));
            }
        } else {
            Debug.logWarning(x.BillingAddress_not_defined_Cybersource_will_fail, MODULE);
        }
        // order shipping information
        GenericValue shippingAddress = (GenericValue) context.get(x.shippingAddress);
        if (shippingAddress != null) {
            if (creditCard != null) {
                // TODO: this is just a kludge since we don't have a firstName and lastName on the PostalAddress entity, that needs to be done
                request.put(x.shipTo_firstName, creditCard.getString(x.firstNameOnCard));
                request.put(x.shipTo_lastName, creditCard.getString(x.lastNameOnCard));
            }
            request.put(x.shipTo_street1, shippingAddress.getString(x.address1));
            if (shippingAddress.get(x.address2) != null) {
                request.put(x.shipTo_street2, shippingAddress.getString(x.address2));
            }
            request.put(x.shipTo_city, shippingAddress.getString(x.city));
            String sCountry = shippingAddress.get(x.countryGeoId) != null ? shippingAddress.getString(x.countryGeoId) : x.USA;
            request.put(x.shipTo_country, sCountry);
            request.put(x.shipTo_postalCode, shippingAddress.getString(x.postalCode));
            if (shippingAddress.get(x.stateProvinceGeoId) != null) {
                request.put(x.shipTo_state, shippingAddress.getString(x.stateProvinceGeoId));
            }
        }
    }

    private static void appendItemLineInfo(Map<String, Object> request, IcsPaymentServicesContext context, String amountField) {
        // send over a line item total offer w/ the total for billing; don't trust CyberSource for calc
        String currency = (String) context.get(x.currency);
        int lineNumber = 0;
        request.put(x.item_c6b5f3a0 + lineNumber + x.unitPrice_12c8439f, getAmountString(context, amountField));
        // the currency
        request.put(x.purchaseTotals_currency, currency);
        // create the offers (one for each line item)
        List<GenericValue> orderItems = UtilGenerics.cast(context.get(x.orderItems));
        if (orderItems != null) {
            for (Object orderItem : orderItems) {
                lineNumber++;
                GenericValue item = (GenericValue) orderItem;
                GenericValue product = null;
                try {
                    product = item.getRelatedOne(x.Product, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.ERROR_Unable_to_get_Product_from_OrderItem_not_passing_info_to_CyberSource);
                }
                if (product != null) {
                    request.put(x.item_c6b5f3a0 + lineNumber + x.productName_771d826c, product.getString(x.productName));
                    request.put(x.item_c6b5f3a0 + lineNumber + x.productSKU, product.getString(x.productId));
                } else {
                    // no product; just send the item description -- non product items
                    request.put(x.item_c6b5f3a0 + lineNumber + x.productName_771d826c, item.getString(x.description));
                }
                // get the quantity..
                BigDecimal quantity = item.getBigDecimal(x.quantity);
                // test quantity if INT pass as is; if not pass as 1
                if (quantity.scale() > 0) {
                    request.put(x.item_c6b5f3a0 + lineNumber + x.quantity_4bcd23fb, x._1);
                } else {
                    request.put(x.emptyString, Integer.toString(quantity.intValue()));
                }
                // set the amount to 0.0000 -- we will send a total too.
                request.put(x.item_c6b5f3a0 + lineNumber + x.unitPrice_12c8439f, x._0_0000);
            }
        }
    }

    private static String getAmountString(IcsPaymentServicesContext context, String amountField) {
        BigDecimal processAmount = (BigDecimal) context.get(amountField);
        return processAmount.setScale(DECIMALS, ROUNDING).toPlainString();
    }

    private static void processAuthResult(Map<String, Object> reply, Map<String, Object> result, Delegator delegator) {
        String decision = getDecision(reply);
        String checkModeStatus = EntityUtilProperties.getPropertyValue(x.payment, x.payment_cybersource_ignoreStatus, delegator);
        if (x.ACCEPT.equalsIgnoreCase(decision)) {
            result.put(x.authCode, reply.get(x.ccAuthReply_authorizationCode));
            result.put(x.authResult, Boolean.TRUE);
        } else {
            result.put(x.authCode, decision);
            if (x.N.equals(checkModeStatus)) {
                result.put(x.authResult, Boolean.FALSE);
            } else {
                result.put(x.authResult, Boolean.TRUE);
            }
            // TODO: based on reasonCode populate the following flags as applicable: resultDeclined, resultNsf, resultBadExpire, resultBadCardNumber
        }

        if (reply.get(x.ccAuthReply_amount) != null) {
            result.put(x.processAmount, new BigDecimal((String) reply.get(x.ccAuthReply_amount)));
        } else {
            result.put(x.processAmount, BigDecimal.ZERO);
        }
        result.put(x.authRefNum, reply.get(x.requestID));
        result.put(x.authFlag, reply.get(x.ccAuthReply_reasonCode));
        result.put(x.authMessage, reply.get(x.ccAuthReply_processorResponse));
        result.put(x.cvCode, reply.get(x.ccAuthReply_cvCode));
        result.put(x.avsCode, reply.get(x.ccAuthReply_avsCode));
        result.put(x.scoreCode, reply.get(x.ccAuthReply_authFactorCode));
        result.put(x.captureRefNum, reply.get(x.requestID));
        if (UtilValidate.isNotEmpty(reply.get(x.ccCaptureReply_reconciliationID))) {
            if (x.ACCEPT.equalsIgnoreCase(decision)) {
                result.put(x.captureResult, Boolean.TRUE);
            } else {
                result.put(x.captureResult, Boolean.FALSE);
            }
            result.put(x.captureCode, reply.get(x.ccCaptureReply_reconciliationID));
            result.put(x.captureFlag, reply.get(x.ccCaptureReply_reasonCode));
            result.put(x.captureMessage, reply.get(x.decision));
        }
        if (Debug.infoOn())
            Debug.logInfo(x.CC_Cybersource_authorization_result + result, MODULE);
    }

    private static void processCaptureResult(Map<String, Object> reply, Map<String, Object> result) {
        String decision = getDecision(reply);
        if (x.ACCEPT.equalsIgnoreCase(decision)) {
            result.put(x.captureResult, Boolean.TRUE);
        } else {
            result.put(x.captureResult, Boolean.FALSE);
        }
        if (reply.get(x.ccCaptureReply_amount) != null) {
            result.put(x.captureAmount, new BigDecimal((String) reply.get(x.ccCaptureReply_amount)));
        } else {
            result.put(x.captureAmount, BigDecimal.ZERO);
        }
        result.put(x.captureRefNum, reply.get(x.requestID));
        result.put(x.captureCode, reply.get(x.ccCaptureReply_reconciliationID));
        result.put(x.captureFlag, reply.get(x.ccCaptureReply_reasonCode));
        result.put(x.captureMessage, reply.get(x.decision));
        if (Debug.infoOn())
            Debug.logInfo(x.CC_Cybersource_capture_result + result, MODULE);
    }

    private static void processReleaseResult(Map<String, Object> reply, Map<String, Object> result) {
        String decision = getDecision(reply);
        if (x.ACCEPT.equalsIgnoreCase(decision)) {
            result.put(x.releaseResult, Boolean.TRUE);
        } else {
            result.put(x.releaseResult, Boolean.FALSE);
        }
        if (reply.get(x.ccAuthReversalReply_amount) != null) {
            result.put(x.releaseAmount, new BigDecimal((String) reply.get(x.ccAuthReversalReply_amount)));
        } else {
            result.put(x.releaseAmount, BigDecimal.ZERO);
        }
        result.put(x.releaseRefNum, reply.get(x.requestID));
        result.put(x.releaseCode, reply.get(x.ccAuthReversalReply_reasonCode));
        result.put(x.releaseFlag, reply.get(x.reasonCode));
        result.put(x.releaseMessage, reply.get(x.decision));
        if (Debug.infoOn())
            Debug.logInfo(x.CC_Cybersource_release_result + result, MODULE);
    }

    private static void processRefundResult(Map<String, Object> reply, Map<String, Object> result) {
        String decision = getDecision(reply);
        if (x.ACCEPT.equalsIgnoreCase(decision)) {
            result.put(x.refundResult, Boolean.TRUE);
        } else {
            result.put(x.refundResult, Boolean.FALSE);
        }
        if (reply.get(x.ccCreditReply_amount) != null) {
            result.put(x.refundAmount, new BigDecimal((String) reply.get(x.ccCreditReply_amount)));
        } else {
            result.put(x.refundAmount, BigDecimal.ZERO);
        }
        result.put(x.refundRefNum, reply.get(x.requestID));
        result.put(x.refundCode, reply.get(x.ccCreditReply_reconciliationID));
        result.put(x.refundFlag, reply.get(x.ccCreditReply_reasonCode));
        result.put(x.refundMessage, reply.get(x.decision));
        if (Debug.infoOn())
            Debug.logInfo(x.CC_Cybersource_refund_result + result, MODULE);
    }

    private static void processCreditResult(Map<String, Object> reply, Map<String, Object> result) {
        String decision = (String) reply.get(x.decision);
        if (x.ACCEPT.equalsIgnoreCase(decision)) {
            result.put(x.creditResult, Boolean.TRUE);
        } else {
            result.put(x.creditResult, Boolean.FALSE);
        }

        if (reply.get(x.ccCreditReply_amount) != null) {
            result.put(x.creditAmount, new BigDecimal((String) reply.get(x.ccCreditReply_amount)));
        } else {
            result.put(x.creditAmount, BigDecimal.ZERO);
        }

        result.put(x.creditRefNum, reply.get(x.requestID));
        result.put(x.creditCode, reply.get(x.ccCreditReply_reconciliationID));
        result.put(x.creditFlag, reply.get(x.ccCreditReply_reasonCode));
        result.put(x.creditMessage, reply.get(x.decision));
        if (Debug.infoOn())
            Debug.logInfo(x.CC_Cybersource_credit_result + result, MODULE);
    }

    private static String getDecision(Map<String, Object> reply) {
        String decision = (String) reply.get(x.decision);
        String reasonCode = (String) reply.get(x.reasonCode);
        if (!x.ACCEPT.equalsIgnoreCase(decision)) {
            Debug.logInfo(x.CyberSource + decision + x.str_d21048c5 + reasonCode + x.str_e7064f0b, MODULE);
            Debug.logInfo(x.Reply_Dump + reply, MODULE);
        }
        return decision;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                PaymentGatewayCyberSourceDao cyberSourceDao = DaoRegistry.getDao(delegator, x.PaymentGatewayCyberSource, PaymentGatewayCyberSourceDao.class);
                PaymentGatewayCyberSourceEntity cyberSource = cyberSourceDao.get(paymentGatewayConfigId).orElse(null);
                if (cyberSource != null) {
                    Object cyberSourceField = Beans.getPropValue(cyberSource, paymentGatewayConfigParameterName, true);
                    if (cyberSourceField != null) {
                        returnValue = cyberSourceField.toString().trim();
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

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName, String defaultValue) {
        String returnValue = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName, resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}
