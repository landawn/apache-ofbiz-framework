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
package org.apache.ofbiz.accounting.thirdparty.valuelink;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import org.apache.ofbiz.persistence.entity.x;
/**
 * ValueLinkServices - Integration with ValueLink Gift Cards
 */
public class ValueLinkServices {

    private static final String MODULE = ValueLinkServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";
    private static final String RES_ERROR = "AccountingErrorUiLabels";
    private static final String RES_ORDER = "OrderUiLabels";

    // generate/display new public/private/kek keys
    public static Map<String, Object> createKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        Boolean kekOnly = context.get(x.kekOnly) != null ? (Boolean) context.get(x.kekOnly) : Boolean.FALSE;
        String kekTest = (String) context.get(x.kekTest);
        Debug.logInfo("KEK Only : " + kekOnly, MODULE);

        StringBuffer buf = vl.outputKeyCreation(kekOnly, kekTest);
        String output = buf.toString();
        Debug.logInfo(":: Key Generation Output ::\n\n" + output, MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("output", output);
        return result;
    }

    // test the KEK encryption
    public static Map<String, Object> testKekEncryption(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        //GenericValue userLogin = (GenericValue) context.get("userLogin");
        Properties props = getProperties(context);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        String testString = (String) context.get(x.kekTest);
        Integer mode = (Integer) context.get(x.mode);
        byte[] testBytes = StringUtil.fromHexString(testString);

        // place holder
        byte[] testEncryption = null;
        String desc = "";

        if (mode == 1) {
            // encrypt the test bytes
            testEncryption = vl.encryptViaKek(testBytes);
            desc = "Encrypted";
        } else {
            // decrypt the test bytes
            testEncryption = vl.decryptViaKek(testBytes);
            desc = "Decrypted";
        }

        // setup the output
        StringBuilder buf = new StringBuilder();
        buf.append("======== Begin Test String (").append(testString.length()).append(") ========\n");
        buf.append(testString).append("\n");
        buf.append("======== End Test String ========\n\n");

        buf.append("======== Begin Test Bytes (").append(testBytes.length).append(") ========\n");
        buf.append(StringUtil.toHexString(testBytes)).append("\n");
        buf.append("======== End Test Bytes ========\n\n");

        buf.append("======== Begin Test Bytes ").append(desc).append(" (").append(testEncryption.length).append(") ========\n");
        buf.append(StringUtil.toHexString(testEncryption)).append("\n");
        buf.append("======== End Test Bytes ").append(desc).append(" ========\n\n");

        String output = buf.toString();
        Debug.logInfo(":: KEK Test Output ::\n\n" + output, MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("output", output);
        return result;
    }

    // change working key service
    public static Map<String, Object> assignWorkingKey(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Properties props = getProperties(context);
        Locale locale = (Locale) context.get(x.locale);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        // place holder
        byte[] mwk = null;

        // see if we passed in the DES hex string
        String desHexString = (String) context.get(x.desHexString);
        if (UtilValidate.isEmpty(desHexString)) {
            mwk = vl.generateMwk();
        } else {
            mwk = vl.generateMwk(StringUtil.fromHexString(desHexString));
        }

        // encrypt the mwk
        String mwkHex = StringUtil.toHexString(vl.encryptViaKek(mwk));

        // build the request
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Encrypt");
        request.put("EncryptKey", mwkHex);
        request.put("EncryptID", vl.getWorkingKeyIndex() + 1);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkCannotUpdateWorkingKey", locale));
        }
        Debug.logInfo("Response : " + response, MODULE);

        // on success update the database / reload the cached api
        String responseCode = (String) response.get("responsecode");
        if (!"00".equals(responseCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkTransactionFailed",
                    UtilMisc.toMap("responseCode", responseCode), locale));
        }
        GenericValue vlKeys = GenericValue.create(vl.getGenericValue());
        vlKeys.set(x.lastWorkingKey, vlKeys.get(x.workingKey));
        vlKeys.set(x.workingKey, StringUtil.toHexString(mwk));
        vlKeys.set(x.workingKeyIndex, request.get("EncryptID"));
        vlKeys.set(x.lastModifiedDate, UtilDateTime.nowTimestamp());
        vlKeys.set(x.lastModifiedByUserLogin, userLogin != null ? userLogin.get(x.userLoginId) : null);
        try {
            vlKeys.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store updated keys; the keys were changed with ValueLink : " + vlKeys, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkCannotStoreWorkingKey", locale));
        }
        vl.reload();
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> activate(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String vlPromoCode = (String) context.get(x.vlPromoCode);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        // override interface for void/rollback
        String iFace = (String) context.get(x.Interface);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Activate");
        if (UtilValidate.isNotEmpty(vlPromoCode)) {
            request.put("PromoCode", vlPromoCode);
        }
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        if (UtilValidate.isNotEmpty(cardNumber)) {
            request.put("CardNo", cardNumber);
        }
        if (UtilValidate.isNotEmpty(pin)) {
            request.put("PIN", vl.encryptPin(pin));
        }

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToActivateGiftCard", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        if ("00".equals(responseCode)) {
            result.put("processResult", Boolean.TRUE);
            result.put("pin", vl.decryptPin((String) response.get("pin")));
        } else {
            result.put("processResult", Boolean.FALSE);
            result.put("pin", response.get("PIN"));
        }
        result.put("responseCode", responseCode);
        result.put("authCode", response.get("authcode"));
        result.put("cardNumber", response.get("cardno"));
        result.put("amount", vl.getAmount((String) response.get("currbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Activate Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> linkPhysicalCard(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String virtualCard = (String) context.get(x.virtualCard);
        String virtualPin = (String) context.get(x.virtualPin);
        String physicalCard = (String) context.get(x.physicalCard);
        String physicalPin = (String) context.get(x.physicalPin);
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Link");
        request.put("VCardNo", virtualCard);
        request.put("VPIN", vl.encryptPin(virtualPin));
        request.put("PCardNo", physicalCard);
        request.put("PPIN", vl.encryptPin(physicalPin));

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToLinkGiftCard", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                "AccountingValueLinkGiftCardActivated", locale));

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("authCode", response.get("authcode"));
        result.put("amount", vl.getAmount((String) response.get("newbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Link Result : " + result, MODULE);
        return result;
    }

    public static Map<String, Object> disablePin(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Disable");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToDisablePin", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                "AccountingValueLinkPinDisabled", locale));

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("balance", vl.getAmount((String) response.get("currbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Disable Result : " + result, MODULE);
        return result;
    }

    public static Map<String, Object> redeem(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        // override interface for void/rollback
        String iFace = (String) context.get(x.Interface);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Redeem");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToRedeemGiftCard", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("authCode", response.get("authcode"));
        result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
        result.put("amount", vl.getAmount((String) response.get("newbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("cashBack", vl.getAmount((String) response.get("cashback")));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Redeem Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> reload(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        // override interface for void/rollback
        String iFace = (String) context.get(x.Interface);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Reload");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToReloadGiftCard", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("authCode", response.get("authcode"));
        result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
        result.put("amount", vl.getAmount((String) response.get("newbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Reload Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> balanceInquire(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Balance");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToCallBalanceInquiry", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("balance", vl.getAmount((String) response.get("currbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Balance Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> transactionHistory(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "History");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToCallHistoryInquiry", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("balance", vl.getAmount((String) response.get("currbal")));
        result.put("history", response.get("history"));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("History Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> refund(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get(x.cardNumber);
        String pin = (String) context.get(x.pin);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        // override interface for void/rollback
        String iFace = (String) context.get(x.Interface);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Refund");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToRefundGiftCard", locale));
        }

        String responseCode = (String) response.get("responsecode");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("processResult", "00".equals(responseCode));
        result.put("responseCode", responseCode);
        result.put("authCode", response.get("authcode"));
        result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
        result.put("amount", vl.getAmount((String) response.get("newbal")));
        result.put("expireDate", response.get("expiredate"));
        result.put("cardClass", response.get("cardclass"));
        result.put("referenceNum", response.get("traceno"));
        Debug.logInfo("Refund Result : " + result, MODULE);
        return result;

    }

    public static Map<String, Object> voidRedeem(DispatchContext dctx, Map<String, Object> context) {
        context.put(x.Interface, "Redeem/Void");
        return redeem(dctx, context);
    }

    public static Map<String, Object> voidRefund(DispatchContext dctx, Map<String, Object> context) {
        context.put(x.Interface, "Refund/Void");
        return refund(dctx, context);
    }

    public static Map<String, Object> voidReload(DispatchContext dctx, Map<String, Object> context) {
        context.put(x.Interface, "Reload/Void");
        return reload(dctx, context);
    }

    public static Map<String, Object> voidActivate(DispatchContext dctx, Map<String, Object> context) {
        context.put(x.Interface, "Activate/Void");
        return activate(dctx, context);
    }

    public static Map<String, Object> timeOutReversal(DispatchContext dctx, Map<String, Object> context) {
        String vlInterface = (String) context.get(x.Interface);
        Locale locale = (Locale) context.get(x.locale);
        Debug.logInfo("704 Interface : " + vlInterface, MODULE);
        if (vlInterface != null) {
            if (vlInterface.startsWith("Activate")) {
                if ("Activate/Rollback".equals(vlInterface)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingValueLinkThisTransactionIsNotSupported", locale));
                }
                return activate(dctx, context);
            } else if (vlInterface.startsWith("Redeem")) {
                return redeem(dctx, context);
            } else if (vlInterface.startsWith("Reload")) {
                return reload(dctx, context);
            } else if (vlInterface.startsWith("Refund")) {
                return refund(dctx, context);
            }
        }

        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                "AccountingValueLinkTransactionNotValid", locale));
    }

    // 0704 Timeout Reversal (Supports - Activate/Void, Redeem, Redeem/Void, Reload, Reload/Void, Refund, Refund/Void)
    private static void setTimeoutReversal(DispatchContext dctx, Map<String, Object> ctx, Map<String, Object> request) {
        String vlInterface = (String) request.get("Interface");
        // clone the context
        Map<String, Object> context = new HashMap<>();
        context.putAll(ctx);

        // append the rollback interface
        if (!vlInterface.endsWith("Rollback")) {
            context.put(x.Interface, vlInterface + "/Rollback");
        } else {
            // no need to re-run ourself we are persisted
            return;
        }

        // set the old tx time and number
        context.put(x.MerchTime, request.get("MerchTime"));
        context.put(x.TermTxnNo, request.get("TermTxnNo"));

        // Activate/Rollback is not supported by valuelink
        if (!"Activate".equals(vlInterface)) {
            // create the listener
            Debug.logInfo("Set 704 context : " + context, MODULE);
            try {
                dctx.getDispatcher().addRollbackService("vlTimeOutReversal", context, false);
                //dctx.getDispatcher().addCommitService("vlTimeOutReversal", context, false);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to setup 0704 Timeout Reversal", MODULE);
            }
        }
    }

    private static Properties getProperties(Map<String, Object> context) {
        String paymentProperties = (String) context.get(x.paymentConfig);
        if (paymentProperties == null) {
            paymentProperties = "payment.properties";
        }
        return UtilProperties.getProperties(paymentProperties);
    }


    // payment processing wrappers (process/release/refund)

    public static Map<String, Object> giftCardProcessor(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue giftCard = (GenericValue) context.get(x.giftCard);
        GenericValue party = (GenericValue) context.get(x.billToParty);
        String paymentConfig = (String) context.get(x.paymentConfig);
        String currency = (String) context.get(x.currency);
        String orderId = (String) context.get(x.orderId);
        BigDecimal amount = (BigDecimal) context.get(x.processAmount);

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<>();
        redeemCtx.put("userLogin", userLogin);
        redeemCtx.put("paymentConfig", paymentConfig);
        redeemCtx.put("cardNumber", giftCard.get(x.cardNumber));
        redeemCtx.put("pin", giftCard.get(x.pinNumber));
        redeemCtx.put("currency", currency);
        redeemCtx.put("orderId", orderId);
        redeemCtx.put("partyId", party.get(x.partyId));
        redeemCtx.put("amount", amount);

        // invoke the redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("redeemGiftCard", redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the redeem service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToRedeemGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            // confirm the amount redeemed; since VL does not error in insufficient funds
            if (processResult) {
                BigDecimal previous = (BigDecimal) redeemResult.get("previousAmount");
                if (previous == null) previous = BigDecimal.ZERO;
                BigDecimal current = (BigDecimal) redeemResult.get("amount");
                if (current == null) current = BigDecimal.ZERO;
                BigDecimal redeemed = previous.subtract(current);
                Debug.logInfo("Redeemed (" + amount + "): " + redeemed + " / " + previous + " : " + current, MODULE);
                if (redeemed.compareTo(amount) < 0) {
                    // we didn't redeem enough void the transaction and return false
                    Map<String, Object> voidResult = null;
                    try {
                        voidResult = dispatcher.runSync("voidRedeemGiftCard", redeemCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (ServiceUtil.isError(voidResult)) {
                        return voidResult;
                    }
                    processResult = Boolean.FALSE;
                    amount = redeemed;
                    result.put("authMessage", "Gift card did not contain enough funds");
                }
            }
            result.put("processAmount", amount);
            result.put("authFlag", redeemResult.get("responseCode"));
            result.put("authResult", processResult);
            result.put("captureResult", processResult);
            result.put("authCode", redeemResult.get("authCode"));
            result.put("captureCode", redeemResult.get("authCode"));
            result.put("authRefNum", redeemResult.get("referenceNum"));
            result.put("captureRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    public static Map<String, Object> giftCardRelease(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String paymentConfig = (String) context.get(x.paymentConfig);
        String currency = (String) context.get(x.currency);
        BigDecimal amount = (BigDecimal) context.get(x.releaseAmount);

        // get the orderId for tracking
        String orderId = paymentPref.getString(x.orderId);

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne(x.GiftCard, false);
        } catch (GenericEntityException e) {
            Debug.logError("Unable to get GiftCard from OrderPaymentPreference", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotLocateItFromOrderPaymentPreference", locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToReleaseGiftCard", locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<>();
        redeemCtx.put("userLogin", userLogin);
        redeemCtx.put("paymentConfig", paymentConfig);
        redeemCtx.put("cardNumber", giftCard.get(x.cardNumber));
        redeemCtx.put("pin", giftCard.get(x.pinNumber));
        redeemCtx.put("currency", currency);
        redeemCtx.put("orderId", orderId);
        redeemCtx.put("amount", amount);

        // invoke the void redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("voidRedeemGiftCard", redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the redeem service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToRedeemGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            result.put("releaseAmount", redeemResult.get("amount"));
            result.put("releaseFlag", redeemResult.get("responseCode"));
            result.put("releaseResult", processResult);
            result.put("releaseCode", redeemResult.get("authCode"));
            result.put("releaseRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    public static Map<String, Object> giftCardRefund(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        String paymentConfig = (String) context.get(x.paymentConfig);
        String currency = (String) context.get(x.currency);
        BigDecimal amount = (BigDecimal) context.get(x.refundAmount);

        // get the orderId for tracking
        String orderId = paymentPref.getString(x.orderId);

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne(x.GiftCard, false);
        } catch (GenericEntityException e) {
            Debug.logError("Unable to get GiftCard from OrderPaymentPreference", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotLocateItFromOrderPaymentPreference", locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToReleaseGiftCard", locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> refundCtx = new HashMap<>();
        refundCtx.put("userLogin", userLogin);
        refundCtx.put("paymentConfig", paymentConfig);
        refundCtx.put("cardNumber", giftCard.get(x.cardNumber));
        refundCtx.put("pin", giftCard.get(x.pinNumber));
        refundCtx.put("currency", currency);
        refundCtx.put("orderId", orderId);
        refundCtx.put("amount", amount);

        // invoke the refund service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("refundGiftCard", refundCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the refund service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToRefundGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            result.put("refundAmount", redeemResult.get("amount"));
            result.put("refundFlag", redeemResult.get("responseCode"));
            result.put("refundResult", processResult);
            result.put("refundCode", redeemResult.get("authCode"));
            result.put("refundRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    // item fulfillment wrappers (purchase/reload)

    public static Map<String, Object> giftCardPurchase(DispatchContext dctx, Map<String, Object> context) {
        // this service should always be called via FULFILLMENT_EXTASYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        Locale locale = (Locale) context.get(x.locale);

        // order ID for tracking
        String orderId = orderItem.getString(x.orderId);

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderHeader from OrderItem", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotProcess", locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "GIFT_CARD", null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString(x.paymentPropertiesPath);
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountSetting",
                    UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId", "GIFT_CARD"), locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString(x.partyId);
        }

        // amount/quantity of the gift card(s)
        BigDecimal amount = orderItem.getBigDecimal(x.unitPrice);
        BigDecimal quantity = orderItem.getBigDecimal(x.quantity);

        // the product entity needed for information
        GenericValue product = null;
        try {
            product = orderItem.getRelatedOne(x.Product, false);
        } catch (GenericEntityException e) {
            Debug.logError("Unable to get Product from OrderItem", MODULE);
        }
        if (product == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotFulfill", locale));
        }

        // get the productFeature type TYPE (VL promo code)
        GenericValue typeFeature = null;
        try {
            typeFeature = EntityQuery.use(delegator)
                    .from("ProductFeatureAndAppl")
                    .where("productId", product.get(x.productId),
                            "productFeatureTypeId", "TYPE")
                    .orderBy("-fromDate").filterByDate().queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToGetFeatureType", locale));
        }
        if (typeFeature == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkFeatureTypeRequested",
                    UtilMisc.toMap("productId", product.get(x.productId)), locale));
        }

        // get the VL promo code
        String promoCode = typeFeature.getString(x.idCode);
        if (UtilValidate.isEmpty(promoCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkPromoCodeInvalid", locale));
        }

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.surveyId", delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId,
                            "orderItemSeqId", orderItem.get(x.orderItemSeqId),
                            "surveyId", surveyId)
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotFulfillFromSurvey", locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers", locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne(x.SurveyQuestion, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers", locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        // get the send to email address - key defined in properties file
        String sendToKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.survey.sendToEmail", delegator);
        String sendToEmail = (String) answerMap.get(sendToKey);
        // get the copyMe flag and set the order email address
        String orderEmails = orh.getOrderEmailString();
        String copyMeField = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.survey.copyMe", delegator);
        String copyMeResp = copyMeField != null ? (String) answerMap.get(copyMeField) : null;
        boolean copyMe = UtilValidate.isNotEmpty(copyMeField)
                && UtilValidate.isNotEmpty(copyMeResp) && "true".equalsIgnoreCase(copyMeResp);

        int qtyLoop = quantity.intValue();
        for (int i = 0; i < qtyLoop; i++) {
            // activate a gift card
            Map<String, Object> activateCtx = new HashMap<>();
            activateCtx.put("paymentConfig", paymentConfig);
            activateCtx.put("vlPromoCode", promoCode);
            activateCtx.put("currency", currency);
            activateCtx.put("partyId", partyId);
            activateCtx.put("orderId", orderId);
            activateCtx.put("amount", amount);
            activateCtx.put("userLogin", userLogin);

            boolean failure = false;
            Map<String, Object> activateResult = null;
            try {
                activateResult = dispatcher.runSync("activateGiftCard", activateCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to activate gift card(s)", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingValueLinkUnableToActivateGiftCard", locale));
            }

            Boolean processResult = (Boolean) activateResult.get("processResult");
            if (activateResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult) {
                failure = true;
            }

            if (!failure) {
                // set the void on rollback
                try {
                    dispatcher.addRollbackService("voidActivateGiftCard", activateCtx, false);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Unable to setup Activate/Void on error", MODULE);
                }
            }

            // create the fulfillment record
            Map<String, Object> vlFulFill = new HashMap<>();
            vlFulFill.put("typeEnumId", "GC_ACTIVATE");
            vlFulFill.put("merchantId", EntityUtilProperties.getPropertyValue(paymentConfig, "payment.valuelink.merchantId", delegator));
            vlFulFill.put("partyId", partyId);
            vlFulFill.put("orderId", orderId);
            vlFulFill.put("orderItemSeqId", orderItem.get(x.orderItemSeqId));
            vlFulFill.put("surveyResponseId", surveyResponse.get(x.surveyResponseId));
            vlFulFill.put("cardNumber", activateResult.get("cardNumber"));
            vlFulFill.put("pinNumber", activateResult.get("pin"));
            vlFulFill.put("amount", activateResult.get("amount"));
            vlFulFill.put("responseCode", activateResult.get("responseCode"));
            vlFulFill.put("referenceNum", activateResult.get("referenceNum"));
            vlFulFill.put("authCode", activateResult.get("authCode"));
            vlFulFill.put("userLogin", userLogin);
            try {
                dispatcher.runAsync("createGcFulFillmentRecord", vlFulFill, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingGiftCertificateNumberCannotStoreFulfillmentInfo",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            if (failure) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingValueLinkUnableToActivateGiftCard", locale));
            }

            // add some information to the answerMap for the email
            answerMap.put("cardNumber", activateResult.get("cardNumber"));
            answerMap.put("pinNumber", activateResult.get("pin"));
            answerMap.put("amount", activateResult.get("amount"));

            // get the email setting for this email type
            GenericValue productStoreEmail = null;
            String emailType = "PRDS_GC_PURCHASE";
            try {
                productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId,
                        "emailType", emailType).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get product store email setting for gift card purchase", MODULE);
            }
            if (productStoreEmail == null) {
                Debug.logError("No gift card purchase email setting found for this store; cannot send gift card information", MODULE);
            } else {
                answerMap.put("locale", locale);

                // set the bcc address(s)
                String bcc = productStoreEmail.getString(x.bccAddress);
                if (copyMe) {
                    if (UtilValidate.isNotEmpty(bcc)) {
                        bcc = bcc + "," + orderEmails;
                    } else {
                        bcc = orderEmails;
                    }
                }

                Map<String, Object> emailCtx = new HashMap<>();
                emailCtx.put("bodyScreenUri", productStoreEmail.getString(x.bodyScreenLocation));
                emailCtx.put("bodyParameters", answerMap);
                emailCtx.put("sendTo", sendToEmail);
                emailCtx.put("contentType", productStoreEmail.get(x.contentType));
                emailCtx.put("sendFrom", productStoreEmail.get(x.fromAddress));
                emailCtx.put("sendCc", productStoreEmail.get(x.ccAddress));
                emailCtx.put("sendBcc", bcc);
                emailCtx.put("subject", productStoreEmail.getString(x.subject));
                emailCtx.put("userLogin", userLogin);

                // send off the email async so we will retry on failed attempts
                try {
                    dispatcher.runAsync("sendMailFromScreen", emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem sending mail", MODULE);
                    // this is fatal; we will rollback and try again later
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "AccountingGiftCertificateNumberCannotSendEmailNotice",
                            UtilMisc.toMap("errorString", e.toString()), locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> giftCardReload(DispatchContext dctx, Map<String, Object> context) {
        // this service should always be called via FULFILLMENT_EXTSYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        Locale locale = (Locale) context.get(x.locale);

        // order ID for tracking
        String orderId = orderItem.getString(x.orderId);

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderHeader from OrderItem", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotProcess",
                    UtilMisc.toMap("orderId", orderId), locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "GIFT_CARD", null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString(x.paymentPropertiesPath);
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotGetPaymentConfiguration", locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString(x.partyId);
        }

        // amount of the gift card reload
        BigDecimal amount = orderItem.getBigDecimal(x.unitPrice);

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.surveyId", delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId,
                            "orderItemSeqId", orderItem.get(x.orderItemSeqId),
                            "surveyId", surveyId).orderBy("-responseDate")
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotFulfillFromSurvey", locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers", locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne(x.SurveyQuestion, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers", locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        String cardNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.cardNumber", delegator);
        String pinNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.pinNumber", delegator);
        String cardNumber = (String) answerMap.get(cardNumberKey);
        String pinNumber = (String) answerMap.get(pinNumberKey);

        // reload the gift card
        Map<String, Object> reloadCtx = new HashMap<>();
        reloadCtx.put("paymentConfig", paymentConfig);
        reloadCtx.put("currency", currency);
        reloadCtx.put("partyId", partyId);
        reloadCtx.put("orderId", orderId);
        reloadCtx.put("cardNumber", cardNumber);
        reloadCtx.put("pin", pinNumber);
        reloadCtx.put("amount", amount);
        reloadCtx.put("userLogin", userLogin);

        Map<String, Object> reloadResult = null;
        try {
            reloadResult = dispatcher.runSync("reloadGiftCard", reloadCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Unable to reload gift card", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingValueLinkUnableToReloadGiftCard", locale));
        }

        // create the fulfillment record
        Map<String, Object> vlFulFill = new HashMap<>();
        vlFulFill.put("typeEnumId", "GC_RELOAD");
        vlFulFill.put("merchantId", EntityUtilProperties.getPropertyValue(paymentConfig, "payment.valuelink.merchantId", delegator));
        vlFulFill.put("partyId", partyId);
        vlFulFill.put("orderId", orderId);
        vlFulFill.put("orderItemSeqId", orderItem.get(x.orderItemSeqId));
        vlFulFill.put("surveyResponseId", surveyResponse.get(x.surveyResponseId));
        vlFulFill.put("cardNumber", cardNumber);
        vlFulFill.put("pinNumber", pinNumber);
        vlFulFill.put("amount", amount);
        vlFulFill.put("responseCode", reloadResult.get("responseCode"));
        vlFulFill.put("referenceNum", reloadResult.get("referenceNum"));
        vlFulFill.put("authCode", reloadResult.get("authCode"));
        vlFulFill.put("userLogin", userLogin);
        try {
            dispatcher.runAsync("createGcFulFillmentRecord", vlFulFill, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingGiftCertificateNumberCannotStoreFulfillmentInfo", locale));
        }

        Boolean processResult = (Boolean) reloadResult.get("processResult");
        if (reloadResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult) {
            Debug.logError("Reload Failed Need to Refund : " + reloadResult, MODULE);

            // process the return
            try {
                Map<String, Object> refundCtx = UtilMisc.<String, Object>toMap("orderItem", orderItem,
                        "partyId", partyId, "userLogin", userLogin);
                dispatcher.runAsync("refundGcPurchase", refundCtx, null, true, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR! Unable to call create refund service; this failed reload will NOT be refunded", MODULE);
            }

            String responseCode = "-1";
            if (processResult != null) {
                responseCode = (String) reloadResult.get("responseCode");
            }
            if ("17".equals(responseCode)) {
                Debug.logError("Error code : " + responseCode + " : Max Balance Exceeded", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingValueLinkUnableToRefundGiftCardMaxBalanceExceeded", locale));
            } else {
                Debug.logError("Error code : " + responseCode + " : Processing Error", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingValueLinkUnableToReloadGiftCardFailed", locale));
            }
        }

        // add some information to the answerMap for the email
        answerMap.put("processResult", reloadResult.get("processResult"));
        answerMap.put("responseCode", reloadResult.get("responseCode"));
        answerMap.put("previousAmount", reloadResult.get("previousAmount"));
        answerMap.put("amount", reloadResult.get("amount"));

        // get the email setting for this email type
        GenericValue productStoreEmail = null;
        String emailType = "PRDS_GC_RELOAD";
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType",
                    emailType).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get product store email setting for gift card purchase", MODULE);
        }
        if (productStoreEmail == null) {
            Debug.logError("No gift card purchase email setting found for this store; cannot send gift card information", MODULE);
        } else {
            Map<String, Object> emailCtx = new HashMap<>();
            answerMap.put("locale", locale);

            emailCtx.put("bodyScreenUri", productStoreEmail.getString(x.bodyScreenLocation));
            emailCtx.put("bodyParameters", answerMap);
            emailCtx.put("sendTo", orh.getOrderEmailString());
            emailCtx.put("contentType", productStoreEmail.get(x.contentType));
            emailCtx.put("sendFrom", productStoreEmail.get(x.fromAddress));
            emailCtx.put("sendCc", productStoreEmail.get(x.ccAddress));
            emailCtx.put("sendBcc", productStoreEmail.get(x.bccAddress));
            emailCtx.put("subject", productStoreEmail.getString(x.subject));
            emailCtx.put("userLogin", userLogin);

            // send off the email async so we will retry on failed attempts
            try {
                dispatcher.runAsync("sendMailFromScreen", emailCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem sending mail", MODULE);
                // this is fatal; we will rollback and try again later
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingGiftCertificateNumberCannotSendEmailNotice",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
