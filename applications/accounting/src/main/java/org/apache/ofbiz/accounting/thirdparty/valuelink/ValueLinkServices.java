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
import java.util.LinkedList;
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
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductFeatureApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureDao;
import org.apache.ofbiz.persistence.dao.ProductStoreEmailSettingDao;
import org.apache.ofbiz.persistence.dao.SurveyResponseDao;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureEntity;
import org.apache.ofbiz.persistence.entity.ProductStoreEmailSettingEntity;
import org.apache.ofbiz.persistence.entity.SurveyResponseEntity;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ValueLinkServicesContext;
/**
 * ValueLinkServices - Integration with ValueLink Gift Cards
 */
public class ValueLinkServices {

    private static final String MODULE = ValueLinkServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final String RES_ERROR = x.AccountingErrorUiLabels;
    private static final String RES_ORDER = x.OrderUiLabels;

    // generate/display new public/private/kek keys
    public static Map<String, Object> createKeys(DispatchContext dctx, ValueLinkServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        Boolean kekOnly = context.get(x.kekOnly) != null ? (Boolean) context.get(x.kekOnly) : Boolean.FALSE;
        String kekTest = (String) context.get(x.kekTest);
        Debug.logInfo(x.KEK_Only + kekOnly, MODULE);

        StringBuffer buf = vl.outputKeyCreation(kekOnly, kekTest);
        String output = buf.toString();
        Debug.logInfo(x.Key_Generation_Output + output, MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.output, output);
        return result;
    }

    // test the KEK encryption
    public static Map<String, Object> testKekEncryption(DispatchContext dctx, ValueLinkServicesContext context) {
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
        String desc = x.emptyString;

        if (mode == 1) {
            // encrypt the test bytes
            testEncryption = vl.encryptViaKek(testBytes);
            desc = x.Encrypted;
        } else {
            // decrypt the test bytes
            testEncryption = vl.decryptViaKek(testBytes);
            desc = x.Decrypted;
        }

        // setup the output
        StringBuilder buf = new StringBuilder();
        buf.append(x.Begin_Test_String).append(testString.length()).append(x.str_f5150789);
        buf.append(testString).append(x.str_adc83b19);
        buf.append(x.End_Test_String);

        buf.append(x.Begin_Test_Bytes).append(testBytes.length).append(x.str_f5150789);
        buf.append(StringUtil.toHexString(testBytes)).append(x.str_adc83b19);
        buf.append(x.End_Test_Bytes);

        buf.append(x.Begin_Test_Bytes_01684795).append(desc).append(x.str_d21048c5).append(testEncryption.length).append(x.str_f5150789);
        buf.append(StringUtil.toHexString(testEncryption)).append(x.str_adc83b19);
        buf.append(x.End_Test_Bytes_0c492459).append(desc).append(x.str_07e9411e);

        String output = buf.toString();
        Debug.logInfo(x.KEK_Test_Output + output, MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.output, output);
        return result;
    }

    // change working key service
    public static Map<String, Object> assignWorkingKey(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, x.Encrypt);
        request.put(x.EncryptKey, mwkHex);
        request.put(x.EncryptID, vl.getWorkingKeyIndex() + 1);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkCannotUpdateWorkingKey, locale));
        }
        Debug.logInfo(x.Response + response, MODULE);

        // on success update the database / reload the cached api
        String responseCode = (String) response.get(x.responsecode);
        if (!x._00_fb965496.equals(responseCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkTransactionFailed,
                    UtilMisc.toMap(x.responseCode, responseCode), locale));
        }
        GenericValue vlKeys = GenericValue.create(vl.getGenericValue());
        vlKeys.set(x.lastWorkingKey, vlKeys.get(x.workingKey));
        vlKeys.set(x.workingKey, StringUtil.toHexString(mwk));
        vlKeys.set(x.workingKeyIndex, request.get(x.EncryptID));
        vlKeys.set(x.lastModifiedDate, UtilDateTime.nowTimestamp());
        vlKeys.set(x.lastModifiedByUserLogin, userLogin != null ? userLogin.get(x.userLoginId) : null);
        try {
            vlKeys.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_store_updated_keys_the_keys_were_changed_with_ValueLink + vlKeys, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkCannotStoreWorkingKey, locale));
        }
        vl.reload();
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> activate(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, iFace != null ? iFace : x.Activate);
        if (UtilValidate.isNotEmpty(vlPromoCode)) {
            request.put(x.PromoCode, vlPromoCode);
        }
        request.put(x.Amount, vl.getAmount(amount));
        request.put(x.LocalCurr, vl.getCurrency(currency));

        if (UtilValidate.isNotEmpty(cardNumber)) {
            request.put(x.CardNo, cardNumber);
        }
        if (UtilValidate.isNotEmpty(pin)) {
            request.put(x.PIN, vl.encryptPin(pin));
        }

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToActivateGiftCard, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (x._00_fb965496.equals(responseCode)) {
            result.put(x.processResult, Boolean.TRUE);
            result.put(x.pin, vl.decryptPin((String) response.get(x.pin)));
        } else {
            result.put(x.processResult, Boolean.FALSE);
            result.put(x.pin, response.get(x.PIN));
        }
        result.put(x.responseCode, responseCode);
        result.put(x.authCode, response.get(x.authcode));
        result.put(x.cardNumber, response.get(x.cardno));
        result.put(x.amount, vl.getAmount((String) response.get(x.currbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Activate_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> linkPhysicalCard(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, x.Link);
        request.put(x.VCardNo, virtualCard);
        request.put(x.VPIN, vl.encryptPin(virtualPin));
        request.put(x.PCardNo, physicalCard);
        request.put(x.PPIN, vl.encryptPin(physicalPin));

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToLinkGiftCard, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                x.AccountingValueLinkGiftCardActivated, locale));

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.authCode, response.get(x.authcode));
        result.put(x.amount, vl.getAmount((String) response.get(x.newbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Link_Result + result, MODULE);
        return result;
    }

    public static Map<String, Object> disablePin(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, x.Disable);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));
        request.put(x.Amount, vl.getAmount(amount));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToDisablePin, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                x.AccountingValueLinkPinDisabled, locale));

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.balance, vl.getAmount((String) response.get(x.currbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Disable_Result + result, MODULE);
        return result;
    }

    public static Map<String, Object> redeem(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, iFace != null ? iFace : x.Redeem);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));
        request.put(x.Amount, vl.getAmount(amount));
        request.put(x.LocalCurr, vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToRedeemGiftCard, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.authCode, response.get(x.authcode));
        result.put(x.previousAmount, vl.getAmount((String) response.get(x.prevbal)));
        result.put(x.amount, vl.getAmount((String) response.get(x.newbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.cashBack, vl.getAmount((String) response.get(x.cashback)));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Redeem_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> reload(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, iFace != null ? iFace : x.Reload);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));
        request.put(x.Amount, vl.getAmount(amount));
        request.put(x.LocalCurr, vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToReloadGiftCard, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.authCode, response.get(x.authcode));
        result.put(x.previousAmount, vl.getAmount((String) response.get(x.prevbal)));
        result.put(x.amount, vl.getAmount((String) response.get(x.newbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Reload_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> balanceInquire(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, x.Balance_90eef613);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));
        request.put(x.LocalCurr, vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToCallBalanceInquiry, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.balance, vl.getAmount((String) response.get(x.currbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Balance_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> transactionHistory(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, x.History);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToCallHistoryInquiry, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.balance, vl.getAmount((String) response.get(x.currbal)));
        result.put(x.history, response.get(x.history));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.History_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> refund(DispatchContext dctx, ValueLinkServicesContext context) {
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
        request.put(x.Interface, iFace != null ? iFace : x.Refund);
        request.put(x.CardNo, cardNumber);
        request.put(x.PIN, vl.encryptPin(pin));
        request.put(x.Amount, vl.getAmount(amount));
        request.put(x.LocalCurr, vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put(x.User1, orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put(x.User2, partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_communicating_with_VL);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToRefundGiftCard, locale));
        }

        String responseCode = (String) response.get(x.responsecode);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.processResult, x._00_fb965496.equals(responseCode));
        result.put(x.responseCode, responseCode);
        result.put(x.authCode, response.get(x.authcode));
        result.put(x.previousAmount, vl.getAmount((String) response.get(x.prevbal)));
        result.put(x.amount, vl.getAmount((String) response.get(x.newbal)));
        result.put(x.expireDate, response.get(x.expiredate));
        result.put(x.cardClass, response.get(x.cardclass));
        result.put(x.referenceNum, response.get(x.traceno));
        Debug.logInfo(x.Refund_Result + result, MODULE);
        return result;

    }

    public static Map<String, Object> voidRedeem(DispatchContext dctx, ValueLinkServicesContext context) {
        context.put(x.Interface, x.Redeem_Void);
        return redeem(dctx, context);
    }

    public static Map<String, Object> voidRefund(DispatchContext dctx, ValueLinkServicesContext context) {
        context.put(x.Interface, x.Refund_Void);
        return refund(dctx, context);
    }

    public static Map<String, Object> voidReload(DispatchContext dctx, ValueLinkServicesContext context) {
        context.put(x.Interface, x.Reload_Void);
        return reload(dctx, context);
    }

    public static Map<String, Object> voidActivate(DispatchContext dctx, ValueLinkServicesContext context) {
        context.put(x.Interface, x.Activate_Void);
        return activate(dctx, context);
    }

    public static Map<String, Object> timeOutReversal(DispatchContext dctx, ValueLinkServicesContext context) {
        String vlInterface = (String) context.get(x.Interface);
        Locale locale = (Locale) context.get(x.locale);
        Debug.logInfo(x._704_Interface + vlInterface, MODULE);
        if (vlInterface != null) {
            if (vlInterface.startsWith(x.Activate)) {
                if (x.Activate_Rollback.equals(vlInterface)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.AccountingValueLinkThisTransactionIsNotSupported, locale));
                }
                return activate(dctx, context);
            } else if (vlInterface.startsWith(x.Redeem)) {
                return redeem(dctx, context);
            } else if (vlInterface.startsWith(x.Reload)) {
                return reload(dctx, context);
            } else if (vlInterface.startsWith(x.Refund)) {
                return refund(dctx, context);
            }
        }

        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                x.AccountingValueLinkTransactionNotValid, locale));
    }

    // 0704 Timeout Reversal (Supports - Activate/Void, Redeem, Redeem/Void, Reload, Reload/Void, Refund, Refund/Void)
    private static void setTimeoutReversal(DispatchContext dctx, Map<String, Object> ctx, Map<String, Object> request) {
        String vlInterface = (String) request.get(x.Interface);
        // clone the context
        ServiceContext context = new ServiceContext();
        context.putAll(ctx);

        // append the rollback interface
        if (!vlInterface.endsWith(x.Rollback)) {
            context.put(x.Interface, vlInterface + x.Rollback_3577e800);
        } else {
            // no need to re-run ourself we are persisted
            return;
        }

        // set the old tx time and number
        context.put(x.MerchTime, request.get(x.MerchTime));
        context.put(x.TermTxnNo, request.get(x.TermTxnNo));

        // Activate/Rollback is not supported by valuelink
        if (!x.Activate.equals(vlInterface)) {
            // create the listener
            Debug.logInfo(x.Set_704_context + context, MODULE);
            try {
                dctx.getDispatcher().addRollbackService(x.vlTimeOutReversal, context, false);
                //dctx.getDispatcher().addCommitService("vlTimeOutReversal", context, false);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Unable_to_setup_0704_Timeout_Reversal, MODULE);
            }
        }
    }

    private static Properties getProperties(ValueLinkServicesContext context) {
        String paymentProperties = (String) context.get(x.paymentConfig);
        if (paymentProperties == null) {
            paymentProperties = x.payment_properties;
        }
        return UtilProperties.getProperties(paymentProperties);
    }


    // payment processing wrappers (process/release/refund)

    public static Map<String, Object> giftCardProcessor(DispatchContext dctx, ValueLinkServicesContext context) {
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
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<>();
        redeemCtx.put(x.userLogin, userLogin);
        redeemCtx.put(x.paymentConfig, paymentConfig);
        redeemCtx.put(x.cardNumber, giftCard.get(x.cardNumber));
        redeemCtx.put(x.pin, giftCard.get(x.pinNumber));
        redeemCtx.put(x.currency, currency);
        redeemCtx.put(x.orderId, orderId);
        redeemCtx.put(x.partyId, party.get(x.partyId));
        redeemCtx.put(x.amount, amount);

        // invoke the redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync(x.redeemGiftCard, redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_redeem_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToRedeemGiftCardFailure, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get(x.processResult);
            // confirm the amount redeemed; since VL does not error in insufficient funds
            if (processResult) {
                BigDecimal previous = (BigDecimal) redeemResult.get(x.previousAmount);
                if (previous == null) previous = BigDecimal.ZERO;
                BigDecimal current = (BigDecimal) redeemResult.get(x.amount);
                if (current == null) current = BigDecimal.ZERO;
                BigDecimal redeemed = previous.subtract(current);
                Debug.logInfo(x.Redeemed + amount + x.str_e6d56e05 + redeemed + x.str_0d0c4ddd + previous + x.str_d98411eb + current, MODULE);
                if (redeemed.compareTo(amount) < 0) {
                    // we didn't redeem enough void the transaction and return false
                    Map<String, Object> voidResult = null;
                    try {
                        voidResult = dispatcher.runSync(x.voidRedeemGiftCard, redeemCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (ServiceUtil.isError(voidResult)) {
                        return voidResult;
                    }
                    processResult = Boolean.FALSE;
                    amount = redeemed;
                    result.put(x.authMessage, x.Gift_card_did_not_contain_enough_funds);
                }
            }
            result.put(x.processAmount, amount);
            result.put(x.authFlag, redeemResult.get(x.responseCode));
            result.put(x.authResult, processResult);
            result.put(x.captureResult, processResult);
            result.put(x.authCode, redeemResult.get(x.authCode));
            result.put(x.captureCode, redeemResult.get(x.authCode));
            result.put(x.authRefNum, redeemResult.get(x.referenceNum));
            result.put(x.captureRefNum, redeemResult.get(x.referenceNum));
        }

        return result;
    }

    public static Map<String, Object> giftCardRelease(DispatchContext dctx, ValueLinkServicesContext context) {
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
            Debug.logError(x.Unable_to_get_GiftCard_from_OrderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotLocateItFromOrderPaymentPreference, locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToReleaseGiftCard, locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<>();
        redeemCtx.put(x.userLogin, userLogin);
        redeemCtx.put(x.paymentConfig, paymentConfig);
        redeemCtx.put(x.cardNumber, giftCard.get(x.cardNumber));
        redeemCtx.put(x.pin, giftCard.get(x.pinNumber));
        redeemCtx.put(x.currency, currency);
        redeemCtx.put(x.orderId, orderId);
        redeemCtx.put(x.amount, amount);

        // invoke the void redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync(x.voidRedeemGiftCard, redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_redeem_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToRedeemGiftCardFailure, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get(x.processResult);
            result.put(x.releaseAmount, redeemResult.get(x.amount));
            result.put(x.releaseFlag, redeemResult.get(x.responseCode));
            result.put(x.releaseResult, processResult);
            result.put(x.releaseCode, redeemResult.get(x.authCode));
            result.put(x.releaseRefNum, redeemResult.get(x.referenceNum));
        }

        return result;
    }

    public static Map<String, Object> giftCardRefund(DispatchContext dctx, ValueLinkServicesContext context) {
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
            Debug.logError(x.Unable_to_get_GiftCard_from_OrderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotLocateItFromOrderPaymentPreference, locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToReleaseGiftCard, locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        Map<String, Object> refundCtx = new HashMap<>();
        refundCtx.put(x.userLogin, userLogin);
        refundCtx.put(x.paymentConfig, paymentConfig);
        refundCtx.put(x.cardNumber, giftCard.get(x.cardNumber));
        refundCtx.put(x.pin, giftCard.get(x.pinNumber));
        refundCtx.put(x.currency, currency);
        refundCtx.put(x.orderId, orderId);
        refundCtx.put(x.amount, amount);

        // invoke the refund service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync(x.refundGiftCard, refundCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_refund_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToRefundGiftCardFailure, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get(x.processResult);
            result.put(x.refundAmount, redeemResult.get(x.amount));
            result.put(x.refundFlag, redeemResult.get(x.responseCode));
            result.put(x.refundResult, processResult);
            result.put(x.refundCode, redeemResult.get(x.authCode));
            result.put(x.refundRefNum, redeemResult.get(x.referenceNum));
        }

        return result;
    }

    // item fulfillment wrappers (purchase/reload)

    public static Map<String, Object> giftCardPurchase(DispatchContext dctx, ValueLinkServicesContext context) {
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
            Debug.logError(e, x.Unable_to_get_OrderHeader_from_OrderItem, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotProcess, locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, x.GIFT_CARD, null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString(x.paymentPropertiesPath);
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountSetting,
                    UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, x.GIFT_CARD), locale));
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
            Debug.logError(x.Unable_to_get_Product_from_OrderItem, MODULE);
        }
        if (product == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfill, locale));
        }

        // get the productFeature type TYPE (VL promo code)
        GenericValue typeFeature = null;
        try {
            ProductFeatureApplDao productFeatureApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class);
            ProductFeatureDao productFeatureDao = DaoRegistry.getDao(delegator, x.ProductFeature, ProductFeatureDao.class);
            List<ProductFeatureApplEntity> productFeatureApplEntities = productFeatureApplDao.list(
                    Filters.eq(x.productId, product.get(x.productId)));
            List<GenericValue> featureAndAppls = new LinkedList<>();
            for (ProductFeatureApplEntity productFeatureApplEntity : productFeatureApplEntities) {
                ProductFeatureEntity productFeatureEntity = productFeatureDao.get(productFeatureApplEntity.getProductFeatureId()).orElse(null);
                if (productFeatureEntity == null || !x.TYPE.equals(productFeatureEntity.getProductFeatureTypeId())) {
                    continue;
                }
                GenericValue featureAndAppl = delegator.makeValue(x.ProductFeatureAndAppl);
                featureAndAppl.setAllFields(delegator.makeValue(x.ProductFeatureAppl, Beans.beanToMap(productFeatureApplEntity)), false, null, false);
                featureAndAppl.setAllFields(delegator.makeValue(x.ProductFeature, Beans.beanToMap(productFeatureEntity)), false, null, false);
                featureAndAppls.add(featureAndAppl);
            }
            featureAndAppls = EntityUtil.filterByDate(featureAndAppls);
            featureAndAppls = EntityUtil.orderBy(featureAndAppls, UtilMisc.toList(x.fromDate_f5440273));
            typeFeature = EntityUtil.getFirst(featureAndAppls);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToGetFeatureType, locale));
        }
        if (typeFeature == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkFeatureTypeRequested,
                    UtilMisc.toMap(x.productId, product.get(x.productId)), locale));
        }

        // get the VL promo code
        String promoCode = typeFeature.getString(x.idCode);
        if (UtilValidate.isEmpty(promoCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkPromoCodeInvalid, locale));
        }

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_purchase_surveyId, delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            SurveyResponseDao surveyResponseDao = DaoRegistry.getDao(delegator, x.SurveyResponse, SurveyResponseDao.class);
            List<SurveyResponseEntity> surveyResponseEntities = surveyResponseDao.list(Filters.and(
                    Filters.eq(x.orderId, orderId),
                    Filters.eq(x.orderItemSeqId, orderItem.get(x.orderItemSeqId)),
                    Filters.eq(x.surveyId, surveyId)));
            surveyResponse = surveyResponseEntities.isEmpty() ? null
                    : delegator.makeValue(x.SurveyResponse, Beans.beanToMap(surveyResponseEntities.get(0)));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurvey, locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
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
                            x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        // get the send to email address - key defined in properties file
        String sendToKey = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_purchase_survey_sendToEmail, delegator);
        String sendToEmail = (String) answerMap.get(sendToKey);
        // get the copyMe flag and set the order email address
        String orderEmails = orh.getOrderEmailString();
        String copyMeField = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_purchase_survey_copyMe, delegator);
        String copyMeResp = copyMeField != null ? (String) answerMap.get(copyMeField) : null;
        boolean copyMe = UtilValidate.isNotEmpty(copyMeField)
                && UtilValidate.isNotEmpty(copyMeResp) && x._true.equalsIgnoreCase(copyMeResp);

        int qtyLoop = quantity.intValue();
        for (int i = 0; i < qtyLoop; i++) {
            // activate a gift card
            Map<String, Object> activateCtx = new HashMap<>();
            activateCtx.put(x.paymentConfig, paymentConfig);
            activateCtx.put(x.vlPromoCode, promoCode);
            activateCtx.put(x.currency, currency);
            activateCtx.put(x.partyId, partyId);
            activateCtx.put(x.orderId, orderId);
            activateCtx.put(x.amount, amount);
            activateCtx.put(x.userLogin, userLogin);

            boolean failure = false;
            Map<String, Object> activateResult = null;
            try {
                activateResult = dispatcher.runSync(x.activateGiftCard, activateCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Unable_to_activate_gift_card_s, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingValueLinkUnableToActivateGiftCard, locale));
            }

            Boolean processResult = (Boolean) activateResult.get(x.processResult);
            if (activateResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult) {
                failure = true;
            }

            if (!failure) {
                // set the void on rollback
                try {
                    dispatcher.addRollbackService(x.voidActivateGiftCard, activateCtx, false);
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Unable_to_setup_Activate_Void_on_error, MODULE);
                }
            }

            // create the fulfillment record
            Map<String, Object> vlFulFill = new HashMap<>();
            vlFulFill.put(x.typeEnumId, x.GC_ACTIVATE);
            vlFulFill.put(x.merchantId, EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_valuelink_merchantId, delegator));
            vlFulFill.put(x.partyId, partyId);
            vlFulFill.put(x.orderId, orderId);
            vlFulFill.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
            vlFulFill.put(x.surveyResponseId, surveyResponse.get(x.surveyResponseId));
            vlFulFill.put(x.cardNumber, activateResult.get(x.cardNumber));
            vlFulFill.put(x.pinNumber, activateResult.get(x.pin));
            vlFulFill.put(x.amount, activateResult.get(x.amount));
            vlFulFill.put(x.responseCode, activateResult.get(x.responseCode));
            vlFulFill.put(x.referenceNum, activateResult.get(x.referenceNum));
            vlFulFill.put(x.authCode, activateResult.get(x.authCode));
            vlFulFill.put(x.userLogin, userLogin);
            try {
                dispatcher.runAsync(x.createGcFulFillmentRecord, vlFulFill, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingGiftCertificateNumberCannotStoreFulfillmentInfo,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            if (failure) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingValueLinkUnableToActivateGiftCard, locale));
            }

            // add some information to the answerMap for the email
            answerMap.put(x.cardNumber, activateResult.get(x.cardNumber));
            answerMap.put(x.pinNumber, activateResult.get(x.pin));
            answerMap.put(x.amount, activateResult.get(x.amount));

            // get the email setting for this email type
            GenericValue productStoreEmail = null;
            String emailType = x.PRDS_GC_PURCHASE;
            try {
                ProductStoreEmailSettingDao productStoreEmailSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting,
                        ProductStoreEmailSettingDao.class);
                ProductStoreEmailSettingEntity productStoreEmailSettingEntity = productStoreEmailSettingDao.list(Filters.and(
                        Filters.eq(x.productStoreId, productStoreId),
                        Filters.eq(x.emailType, emailType))).stream().findFirst().orElse(null);
                productStoreEmail = productStoreEmailSettingEntity == null ? null
                        : delegator.makeValue(x.ProductStoreEmailSetting, Beans.beanToMap(productStoreEmailSettingEntity));
            } catch (Exception e) {
                Debug.logError(e, x.Unable_to_get_product_store_email_setting_for_gift_card_purchase, MODULE);
            }
            if (productStoreEmail == null) {
                Debug.logError(x.No_gift_card_purchase_email_setting_found_for_this_store_cannot_send_gift_card_information, MODULE);
            } else {
                answerMap.put(x.locale, locale);

                // set the bcc address(s)
                String bcc = productStoreEmail.getString(x.bccAddress);
                if (copyMe) {
                    if (UtilValidate.isNotEmpty(bcc)) {
                        bcc = bcc + x.str_5c10b5b2 + orderEmails;
                    } else {
                        bcc = orderEmails;
                    }
                }

                Map<String, Object> emailCtx = new HashMap<>();
                emailCtx.put(x.bodyScreenUri, productStoreEmail.getString(x.bodyScreenLocation));
                emailCtx.put(x.bodyParameters, answerMap);
                emailCtx.put(x.sendTo, sendToEmail);
                emailCtx.put(x.contentType, productStoreEmail.get(x.contentType));
                emailCtx.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
                emailCtx.put(x.sendCc, productStoreEmail.get(x.ccAddress));
                emailCtx.put(x.sendBcc, bcc);
                emailCtx.put(x.subject, productStoreEmail.getString(x.subject));
                emailCtx.put(x.userLogin, userLogin);

                // send off the email async so we will retry on failed attempts
                try {
                    dispatcher.runAsync(x.sendMailFromScreen, emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_sending_mail, MODULE);
                    // this is fatal; we will rollback and try again later
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingGiftCertificateNumberCannotSendEmailNotice,
                            UtilMisc.toMap(x.errorString, e.toString()), locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> giftCardReload(DispatchContext dctx, ValueLinkServicesContext context) {
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
            Debug.logError(e, x.Unable_to_get_OrderHeader_from_OrderItem, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER,
                    x.OrderOrderNotFound, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotProcess,
                    UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, x.GIFT_CARD, null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString(x.paymentPropertiesPath);
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotGetPaymentConfiguration, locale));
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
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_surveyId, delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            SurveyResponseDao surveyResponseDao = DaoRegistry.getDao(delegator, x.SurveyResponse, SurveyResponseDao.class);
            List<SurveyResponseEntity> surveyResponseEntities = surveyResponseDao.list(Filters.and(
                    Filters.eq(x.orderId, orderId),
                    Filters.eq(x.orderItemSeqId, orderItem.get(x.orderItemSeqId)),
                    Filters.eq(x.surveyId, surveyId)));
            List<GenericValue> surveyResponses = new LinkedList<>();
            for (SurveyResponseEntity surveyResponseEntity : surveyResponseEntities) {
                surveyResponses.add(delegator.makeValue(x.SurveyResponse, Beans.beanToMap(surveyResponseEntity)));
            }
            surveyResponses = EntityUtil.orderBy(surveyResponses, UtilMisc.toList(x.responseDate_37a6232b));
            surveyResponse = EntityUtil.getFirst(surveyResponses);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurvey, locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated(x.SurveyResponseAnswer, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
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
                            x.AccountingGiftCertificateNumberCannotFulfillFromSurveyAnswers, locale));
                }
                if (question != null) {
                    String desc = question.getString(x.description);
                    String ans = answer.getString(x.textResponse);  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        String cardNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_survey_cardNumber, delegator);
        String pinNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_giftcert_reload_survey_pinNumber, delegator);
        String cardNumber = (String) answerMap.get(cardNumberKey);
        String pinNumber = (String) answerMap.get(pinNumberKey);

        // reload the gift card
        Map<String, Object> reloadCtx = new HashMap<>();
        reloadCtx.put(x.paymentConfig, paymentConfig);
        reloadCtx.put(x.currency, currency);
        reloadCtx.put(x.partyId, partyId);
        reloadCtx.put(x.orderId, orderId);
        reloadCtx.put(x.cardNumber, cardNumber);
        reloadCtx.put(x.pin, pinNumber);
        reloadCtx.put(x.amount, amount);
        reloadCtx.put(x.userLogin, userLogin);

        Map<String, Object> reloadResult = null;
        try {
            reloadResult = dispatcher.runSync(x.reloadGiftCard, reloadCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Unable_to_reload_gift_card, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingValueLinkUnableToReloadGiftCard, locale));
        }

        // create the fulfillment record
        Map<String, Object> vlFulFill = new HashMap<>();
        vlFulFill.put(x.typeEnumId, x.GC_RELOAD);
        vlFulFill.put(x.merchantId, EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_valuelink_merchantId, delegator));
        vlFulFill.put(x.partyId, partyId);
        vlFulFill.put(x.orderId, orderId);
        vlFulFill.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
        vlFulFill.put(x.surveyResponseId, surveyResponse.get(x.surveyResponseId));
        vlFulFill.put(x.cardNumber, cardNumber);
        vlFulFill.put(x.pinNumber, pinNumber);
        vlFulFill.put(x.amount, amount);
        vlFulFill.put(x.responseCode, reloadResult.get(x.responseCode));
        vlFulFill.put(x.referenceNum, reloadResult.get(x.referenceNum));
        vlFulFill.put(x.authCode, reloadResult.get(x.authCode));
        vlFulFill.put(x.userLogin, userLogin);
        try {
            dispatcher.runAsync(x.createGcFulFillmentRecord, vlFulFill, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingGiftCertificateNumberCannotStoreFulfillmentInfo, locale));
        }

        Boolean processResult = (Boolean) reloadResult.get(x.processResult);
        if (reloadResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult) {
            Debug.logError(x.Reload_Failed_Need_to_Refund + reloadResult, MODULE);

            // process the return
            try {
                Map<String, Object> refundCtx = UtilMisc.<String, Object>toMap(x.orderItem, orderItem,
                        x.partyId, partyId, x.userLogin, userLogin);
                dispatcher.runAsync(x.refundGcPurchase, refundCtx, null, true, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.ERROR_Unable_to_call_create_refund_service_this_failed_reload_will_NOT_be_refunded, MODULE);
            }

            String responseCode = x._1_7984b0a0;
            if (processResult != null) {
                responseCode = (String) reloadResult.get(x.responseCode);
            }
            if (x._17.equals(responseCode)) {
                Debug.logError(x.Error_code + responseCode + x.Max_Balance_Exceeded, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingValueLinkUnableToRefundGiftCardMaxBalanceExceeded, locale));
            } else {
                Debug.logError(x.Error_code + responseCode + x.Processing_Error, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingValueLinkUnableToReloadGiftCardFailed, locale));
            }
        }

        // add some information to the answerMap for the email
        answerMap.put(x.processResult, reloadResult.get(x.processResult));
        answerMap.put(x.responseCode, reloadResult.get(x.responseCode));
        answerMap.put(x.previousAmount, reloadResult.get(x.previousAmount));
        answerMap.put(x.amount, reloadResult.get(x.amount));

        // get the email setting for this email type
        GenericValue productStoreEmail = null;
        String emailType = x.PRDS_GC_RELOAD;
        try {
            ProductStoreEmailSettingDao productStoreEmailSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting,
                    ProductStoreEmailSettingDao.class);
            ProductStoreEmailSettingEntity productStoreEmailSettingEntity = productStoreEmailSettingDao.list(Filters.and(
                    Filters.eq(x.productStoreId, productStoreId),
                    Filters.eq(x.emailType, emailType))).stream().findFirst().orElse(null);
            productStoreEmail = productStoreEmailSettingEntity == null ? null
                    : delegator.makeValue(x.ProductStoreEmailSetting, Beans.beanToMap(productStoreEmailSettingEntity));
        } catch (Exception e) {
            Debug.logError(e, x.Unable_to_get_product_store_email_setting_for_gift_card_purchase, MODULE);
        }
        if (productStoreEmail == null) {
            Debug.logError(x.No_gift_card_purchase_email_setting_found_for_this_store_cannot_send_gift_card_information, MODULE);
        } else {
            Map<String, Object> emailCtx = new HashMap<>();
            answerMap.put(x.locale, locale);

            emailCtx.put(x.bodyScreenUri, productStoreEmail.getString(x.bodyScreenLocation));
            emailCtx.put(x.bodyParameters, answerMap);
            emailCtx.put(x.sendTo, orh.getOrderEmailString());
            emailCtx.put(x.contentType, productStoreEmail.get(x.contentType));
            emailCtx.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
            emailCtx.put(x.sendCc, productStoreEmail.get(x.ccAddress));
            emailCtx.put(x.sendBcc, productStoreEmail.get(x.bccAddress));
            emailCtx.put(x.subject, productStoreEmail.getString(x.subject));
            emailCtx.put(x.userLogin, userLogin);

            // send off the email async so we will retry on failed attempts
            try {
                dispatcher.runAsync(x.sendMailFromScreen, emailCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_sending_mail, MODULE);
                // this is fatal; we will rollback and try again later
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.AccountingGiftCertificateNumberCannotSendEmailNotice,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
