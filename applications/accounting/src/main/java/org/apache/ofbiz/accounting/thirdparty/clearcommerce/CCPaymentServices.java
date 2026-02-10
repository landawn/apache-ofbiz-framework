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
package org.apache.ofbiz.accounting.thirdparty.clearcommerce;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.CCPaymentServicesContext;
/**
 * ClearCommerce Payment Services (CCE 5.4)
 */
public class CCPaymentServices {

    private static final String MODULE = CCPaymentServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING_MODE = UtilNumber.getRoundingMode(x.invoice_rounding);
    private static final int MAX_SEV_COMP = 4;

    public static Map<String, Object> ccAuth(DispatchContext dctx, CCPaymentServicesContext context) {
        String ccAction = (String) context.get(x.ccAction);
        Delegator delegator = dctx.getDelegator();
        if (ccAction == null) {
            ccAction = x.PreAuth;
        }
        Document authRequestDoc = buildPrimaryTxRequest(context, ccAction, (BigDecimal) context.get(x.processAmount),
                (String) context.get(x.orderId));

        Document authResponseDoc = null;
        try {
            authResponseDoc = sendRequest(authRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(authResponseDoc) > MAX_SEV_COMP) { // 5 and higher, process error from HSBC
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.authResult, Boolean.FALSE);
            result.put(x.processAmount, BigDecimal.ZERO);
            result.put(x.authRefNum, getReferenceNum(authResponseDoc));
            List<String> messages = getMessageList(authResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processAuthResponse(authResponseDoc);
    }

    public static Map<String, Object> ccCredit(DispatchContext dctx, CCPaymentServicesContext context) {
        String action = x.Credit;
        Delegator delegator = dctx.getDelegator();
        if (context.get(x.pbOrder) != null) {
            action = x.Auth; // required for periodic billing....
        }

        Document creditRequestDoc = buildPrimaryTxRequest(context, action, (BigDecimal) context.get(x.creditAmount),
                (String) context.get(x.referenceCode));
        Document creditResponseDoc = null;
        try {
            creditResponseDoc = sendRequest(creditRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(creditResponseDoc) > MAX_SEV_COMP) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.creditResult, Boolean.FALSE);
            result.put(x.creditAmount, BigDecimal.ZERO);
            result.put(x.creditRefNum, getReferenceNum(creditResponseDoc));
            List<String> messages = getMessageList(creditResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processCreditResponse(creditResponseDoc);
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotCapture, locale));
        }

        Document captureRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString(x.referenceNum),
                x.PostAuth, (BigDecimal) context.get(x.captureAmount), delegator);

        Document captureResponseDoc = null;
        try {
            captureResponseDoc = sendRequest(captureRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(captureResponseDoc) > MAX_SEV_COMP) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.captureResult, Boolean.FALSE);
            result.put(x.captureAmount, BigDecimal.ZERO);
            result.put(x.captureRefNum, getReferenceNum(captureResponseDoc));
            List<String> messages = getMessageList(captureResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processCaptureResponse(captureResponseDoc);
    }

    public static Map<String, Object> ccRelease(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        Document releaseRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString(x.referenceNum), x.Void,
                null, delegator);

        Document releaseResponseDoc = null;
        try {
            releaseResponseDoc = sendRequest(releaseRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(releaseResponseDoc) > MAX_SEV_COMP) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.releaseResult, Boolean.FALSE);
            result.put(x.releaseAmount, BigDecimal.ZERO);
            result.put(x.releaseRefNum, getReferenceNum(releaseResponseDoc));
            List<String> messages = getMessageList(releaseResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processReleaseResponse(releaseResponseDoc);
    }

    public static Map<String, Object> ccReleaseNoop(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRelease, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.releaseResult, Boolean.TRUE);
        result.put(x.releaseCode, authTransaction.getString(x.gatewayCode));
        result.put(x.releaseAmount, authTransaction.getBigDecimal(x.amount));
        result.put(x.releaseRefNum, authTransaction.getString(x.referenceNum));
        result.put(x.releaseFlag, authTransaction.getString(x.gatewayFlag));
        result.put(x.releaseMessage, x.Approved);

        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotRefund, locale));
        }

        // Although refunds are applied to captured transactions, using the auth reference number is ok here
        // Related auth and capture transactions will always have the same reference number
        Document refundRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString(x.referenceNum),
                x.Credit, (BigDecimal) context.get(x.refundAmount), delegator);

        Document refundResponseDoc = null;
        try {
            refundResponseDoc = sendRequest(refundRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(refundResponseDoc) > MAX_SEV_COMP) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.refundResult, Boolean.FALSE);
            result.put(x.refundAmount, BigDecimal.ZERO);
            result.put(x.refundRefNum, getReferenceNum(refundResponseDoc));
            List<String> messages = getMessageList(refundResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processRefundResponse(refundResponseDoc);
    }

    public static Map<String, Object> ccReAuth(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get(x.orderPaymentPreference);
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingPaymentTransactionAuthorizationNotFoundCannotReauth, locale));
        }

        Document reauthRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString(x.referenceNum),
                x.RePreAuth, (BigDecimal) context.get(x.reauthAmount), delegator);

        Document reauthResponseDoc = null;
        try {
            reauthResponseDoc = sendRequest(reauthRequestDoc, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(reauthResponseDoc) > MAX_SEV_COMP) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.reauthResult, Boolean.FALSE);
            result.put(x.reauthAmount, BigDecimal.ZERO);
            result.put(x.reauthRefNum, getReferenceNum(reauthResponseDoc));
            List<String> messages = getMessageList(reauthResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put(x.internalRespMsgs, messages);
            }
            return result;
        }

        return processReAuthResponse(reauthResponseDoc);

    }

    public static Map<String, Object> ccReport(DispatchContext dctx, CCPaymentServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        // configuration file
        String paymentConfig = (String) context.get(x.paymentConfig);
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }

        // orderId
        String orderId = (String) context.get(x.orderId);
        if (UtilValidate.isEmpty(orderId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.AccountingClearCommerceCannotExecuteReport, locale));
        }

        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument(x.EngineDocList);
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, x.DocVersion, x._1_0, requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, x.EngineDoc, requestDocument);
        UtilXml.addChildElementValue(engineDocElement, x.ContentType, x.ReportDoc, requestDocument);

        String sourceId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_sourceId, delegator);
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, x.SourceId, sourceId, requestDocument);
        }

        String groupId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_groupId, delegator);
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, x.GroupId, groupId, requestDocument);
        } else {
            UtilXml.addChildElementValue(engineDocElement, x.GroupId, orderId, requestDocument);
        }

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, x.User, requestDocument);
        UtilXml.addChildElementValue(userElement, x.Name,
                EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_username, x.emptyString, delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, x.Password,
                EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_password, x.emptyString, delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, x.Alias,
                EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_alias, x.emptyString, delegator), requestDocument);

        String effectiveAlias = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_effectiveAlias, delegator);
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, x.EffectiveAlias, effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, x.Instructions, requestDocument);
        Element routingListDocElement = UtilXml.addChildElement(instructionsElement, x.RoutingList, requestDocument);
        Element routingDocElement = UtilXml.addChildElement(routingListDocElement, x.Routing, requestDocument);
        UtilXml.addChildElementValue(routingDocElement, x.name, x.CcxReports, requestDocument);

        // EngineDocList.EngineDoc.ReportDoc
        Element reportDocElement = UtilXml.addChildElement(engineDocElement, x.ReportDoc, requestDocument);
        Element compList = UtilXml.addChildElement(reportDocElement, x.CompList, requestDocument);
        Element comp = UtilXml.addChildElement(compList, x.Comp, requestDocument);
        UtilXml.addChildElementValue(comp, x.Name, x.CcxReports, requestDocument);
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList
        Element actionList = UtilXml.addChildElement(comp, x.ReportActionList, requestDocument);
        Element action = UtilXml.addChildElement(actionList, x.ReportAction, requestDocument);
        UtilXml.addChildElementValue(action, x.ReportName, x.CCE_OrderDetail, requestDocument);
        Element start = UtilXml.addChildElementValue(action, x.Start, x._1, requestDocument);
        start.setAttribute(x.DataType, x.S32);
        Element count = UtilXml.addChildElementValue(action, x.Count, x._10, requestDocument);
        count.setAttribute(x.DataType, x.S32);
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList.ReportAction.ValueList
        Element valueList = UtilXml.addChildElement(action, x.ValueList, requestDocument);
        Element value = UtilXml.addChildElement(valueList, x.Value, requestDocument);
        String clientIdConfig = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_clientId, delegator);
        if (UtilValidate.isNotEmpty(clientIdConfig)) {
            Element clientId = UtilXml.addChildElementValue(value, x.ClientId, clientIdConfig, requestDocument);
            clientId.setAttribute(x.DataType, x.S32);
        }
        UtilXml.addChildElementValue(value, x.OrderId, orderId, requestDocument);

        Debug.set(Debug.VERBOSE, true);
        // Document reportResponseDoc = null;
        try {
            sendRequest(requestDocument, (String) context.get(x.paymentConfig), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }
        Debug.set(Debug.VERBOSE, true);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        return result;
    }

    private static Map<String, Object> processAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.authResult, Boolean.TRUE);
            result.put(x.authCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String authAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.processAmount, new BigDecimal(authAmountStr).movePointLeft(2));
        } else {
            result.put(x.authResult, Boolean.FALSE);
            result.put(x.processAmount, BigDecimal.ZERO);
        }

        result.put(x.authRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.authFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.authMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        // AVS
        String avsCode = UtilXml.childElementValue(procResponseElement, x.AvsDisplay);
        if (UtilValidate.isNotEmpty(avsCode)) {
            result.put(x.avsCode, avsCode);
        }

        // Fraud score
        Element fraudInfoElement = UtilXml.firstChildElement(orderFormElement, x.FraudInfo);
        if (fraudInfoElement != null) {
            result.put(x.scoreCode, UtilXml.childElementValue(fraudInfoElement, x.TotalScore));
        }

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static Map<String, Object> processCreditResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.creditResult, Boolean.TRUE);
            result.put(x.creditCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String creditAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.creditAmount, new BigDecimal(creditAmountStr).movePointLeft(2));
        } else {
            result.put(x.creditResult, Boolean.FALSE);
            result.put(x.creditAmount, BigDecimal.ZERO);
        }

        result.put(x.creditRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.creditFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.creditMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static Map<String, Object> processCaptureResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.captureResult, Boolean.TRUE);
            result.put(x.captureCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String captureAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.captureAmount, new BigDecimal(captureAmountStr).movePointLeft(2));
        } else {
            result.put(x.captureResult, Boolean.FALSE);
            result.put(x.captureAmount, BigDecimal.ZERO);
        }

        result.put(x.captureRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.captureFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.captureMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static Map<String, Object> processReleaseResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.releaseResult, Boolean.TRUE);
            result.put(x.releaseCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String releaseAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.releaseAmount, new BigDecimal(releaseAmountStr).movePointLeft(2));
        } else {
            result.put(x.releaseResult, Boolean.FALSE);
            result.put(x.releaseAmount, BigDecimal.ZERO);
        }

        result.put(x.releaseRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.releaseFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.releaseMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static Map<String, Object> processRefundResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.refundResult, Boolean.TRUE);
            result.put(x.refundCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String refundAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.refundAmount, new BigDecimal(refundAmountStr).movePointLeft(2));
        } else {
            result.put(x.refundResult, Boolean.FALSE);
            result.put(x.refundAmount, BigDecimal.ZERO);
        }

        result.put(x.refundRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.refundFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.refundMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static Map<String, Object> processReAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, x.Transaction);
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, x.CardProcResp);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, x.CcErrCode);
        if (x._1.equals(errorCode)) {
            result.put(x.reauthResult, Boolean.TRUE);
            result.put(x.reauthCode, UtilXml.childElementValue(transactionElement, x.AuthCode));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, x.CurrentTotals);
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, x.Totals);
            String reauthAmountStr = UtilXml.childElementValue(totalsElement, x.Total);
            result.put(x.reauthAmount, new BigDecimal(reauthAmountStr).movePointLeft(2));
        } else {
            result.put(x.reauthResult, Boolean.FALSE);
            result.put(x.reauthAmount, BigDecimal.ZERO);
        }

        result.put(x.reauthRefNum, UtilXml.childElementValue(orderFormElement, x.Id));
        result.put(x.reauthFlag, UtilXml.childElementValue(procResponseElement, x.Status_bae7d5be));
        result.put(x.reauthMessage, UtilXml.childElementValue(procResponseElement, x.CcReturnMsg));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put(x.internalRespMsgs, messages);
        }
        return result;
    }

    private static List<String> getMessageList(Document responseDocument) {

        List<String> messageList = new ArrayList<>();

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element messageListElement = UtilXml.firstChildElement(engineDocElement, x.MessageList);
        List<? extends Element> messageElementList = UtilXml.childElementList(messageListElement, x.Message);
        if (UtilValidate.isNotEmpty(messageElementList)) {
            for (Element messageElement : messageElementList) {
                int severity = 0;
                try {
                    severity = Integer.parseInt(UtilXml.childElementValue(messageElement, x.Sev));
                } catch (NumberFormatException nfe) {
                    Debug.logError(x.Error_parsing_message_severity + nfe.getMessage(), MODULE);
                    severity = 9;
                }
                String message = x.str_1e5c2f36 + UtilXml.childElementValue(messageElement, x.Audience) + x.str_01af9139 + UtilXml
                        .childElementValue(messageElement, x.Text) + x.str_d21048c5 + severity + x.str_e7064f0b;
                messageList.add(message);
            }
        }

        return messageList;
    }

    private static int getMessageListMaxSev(Document responseDocument) {

        int maxSev = 0;

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        Element messageListElement = UtilXml.firstChildElement(engineDocElement, x.MessageList);
        String maxSevStr = UtilXml.childElementValue(messageListElement, x.MaxSev);
        if (UtilValidate.isNotEmpty(maxSevStr)) {
            try {
                maxSev = Integer.parseInt(maxSevStr);
            } catch (NumberFormatException nfe) {
                Debug.logError(x.Error_parsing_MaxSev + nfe.getMessage(), MODULE);
                maxSev = 9;
            }
        }
        return maxSev;
    }

    private static String getReferenceNum(Document responseDocument) {
        String referenceNum = null;
        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.EngineDoc);
        if (engineDocElement != null) {
            Element orderFormElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
            if (orderFormElement != null) {
                referenceNum = UtilXml.childElementValue(orderFormElement, x.Id);
            }
        }
        return referenceNum;
    }

    private static Document buildPrimaryTxRequest(CCPaymentServicesContext context, String type, BigDecimal amount, String refNum) {

        String paymentConfig = (String) context.get(x.paymentConfig);
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }
        // payment mech
        GenericValue creditCard = (GenericValue) context.get(x.creditCard);
        Delegator delegator = creditCard.getDelegator();
        Document requestDocument = createRequestDocument(paymentConfig, delegator);

        Element engineDocElement = UtilXml.firstChildElement(requestDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormDocElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);

        // add the reference number as a comment
        UtilXml.addChildElementValue(orderFormDocElement, x.Comments, refNum, requestDocument);

        Element consumerElement = UtilXml.addChildElement(orderFormDocElement, x.Consumer, requestDocument);

        // email address
        GenericValue billToEmail = (GenericValue) context.get(x.billToEmail);
        if (billToEmail != null) {
            UtilXml.addChildElementValue(consumerElement, x.Email, billToEmail.getString(x.infoString), requestDocument);
        }

        boolean enableCVM = EntityUtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, x.payment_clearcommerce_enableCVM, x.Y, delegator);
        String cardSecurityCode = enableCVM ? (String) context.get(x.cardSecurityCode) : null;

        // Default to locale code 840 (United States)
        String localCode = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_localeCode, x._840, delegator);

        appendPaymentMechNode(consumerElement, creditCard, cardSecurityCode, localCode);

        // billing address
        GenericValue billingAddress = (GenericValue) context.get(x.billingAddress);
        if (billingAddress != null) {
            Element billToElement = UtilXml.addChildElement(consumerElement, x.BillTo, requestDocument);
            Element billToLocationElement = UtilXml.addChildElement(billToElement, x.Location, requestDocument);
            appendAddressNode(billToLocationElement, billingAddress);
        }

        // shipping address
        GenericValue shippingAddress = (GenericValue) context.get(x.shippingAddress);
        if (shippingAddress != null) {
            Element shipToElement = UtilXml.addChildElement(consumerElement, x.ShipTo, requestDocument);
            Element shipToLocationElement = UtilXml.addChildElement(shipToElement, x.Location, requestDocument);
            appendAddressNode(shipToLocationElement, shippingAddress);
        }

        // Default to currency code 840 (USD)
        String currencyCode = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_currencyCode, x._840, delegator);

        // transaction
        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        // TODO: determine if adding OrderItemList is worthwhile - JFE 2004.02.14

        Map<String, Object> pbOrder = UtilGenerics.cast(context.get(x.pbOrder));
        if (pbOrder != null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.pbOrder_Map_not_empty + pbOrder.toString(), MODULE);
            }
            Element pbOrderElement = UtilXml.addChildElement(orderFormDocElement, x.PbOrder, requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, x.OrderFrequencyCycle, (String) pbOrder.get(
                    x.OrderFrequencyCycle), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, x.OrderFrequencyInterval, (String) pbOrder
                    .get(x.OrderFrequencyInterval), requestDocument);
            interval.setAttribute(x.DataType, x.S32);
            Element total = UtilXml.addChildElementValue(pbOrderElement, x.TotalNumberPayments, (String) pbOrder.get(
                    x.TotalNumberPayments), requestDocument);
            total.setAttribute(x.DataType, x.S32);
        } else if (context.get(x.OrderFrequencyCycle) != null && context.get(x.OrderFrequencyInterval) != null
                && context.get(x.TotalNumberPayments) != null) {
            Element pbOrderElement = UtilXml.addChildElement(orderFormDocElement, x.PbOrder, requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, x.OrderFrequencyCycle, (String) context.get(
                    x.OrderFrequencyCycle), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, x.OrderFrequencyInterval, (String) context
                    .get(x.OrderFrequencyInterval), requestDocument);
            interval.setAttribute(x.DataType, x.S32);
            Element total = UtilXml.addChildElementValue(pbOrderElement, x.TotalNumberPayments, (String) context.get(
                    x.TotalNumberPayments), requestDocument);
            total.setAttribute(x.DataType, x.S32);
        }

        return requestDocument;
    }

    private static Document buildSecondaryTxRequest(CCPaymentServicesContext context, String id, String type, BigDecimal amount, Delegator delegator) {

        String paymentConfig = (String) context.get(x.paymentConfig);
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }

        Document requestDocument = createRequestDocument(paymentConfig, delegator);

        Element engineDocElement = UtilXml.firstChildElement(requestDocument.getDocumentElement(), x.EngineDoc);
        Element orderFormDocElement = UtilXml.firstChildElement(engineDocElement, x.OrderFormDoc);
        UtilXml.addChildElementValue(orderFormDocElement, x.Id, id, requestDocument);

        // Default to currency code 840 (USD)
        String currencyCode = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_currencyCode, x._840, delegator);

        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        return requestDocument;
    }

    private static void appendPaymentMechNode(Element element, GenericValue creditCard, String cardSecurityCode, String localeCode) {

        final int securityCodeLength = 4;
        Document document = element.getOwnerDocument();

        Element paymentMechElement = UtilXml.addChildElement(element, x.PaymentMech, document);
        Element creditCardElement = UtilXml.addChildElement(paymentMechElement, x.CreditCard, document);

        UtilXml.addChildElementValue(creditCardElement, x.Number, creditCard.getString(x.cardNumber), document);

        String expDate = creditCard.getString(x.expireDate);
        Element expiresElement = UtilXml.addChildElementValue(creditCardElement, x.Expires, expDate.substring(0, 3)
                + expDate.substring(5), document);
        expiresElement.setAttribute(x.DataType, x.ExpirationDate);
        expiresElement.setAttribute(x.Locale, localeCode);

        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            // Cvv2Val must be exactly securityCodeLength characters
            if (cardSecurityCode.length() < securityCodeLength) {
                // space padding on right side of cardSecurityCode
                cardSecurityCode = String.format(x.str_409fa362 + securityCodeLength + x.s, cardSecurityCode);

            } else if (cardSecurityCode.length() > securityCodeLength) {
                cardSecurityCode = cardSecurityCode.substring(0, securityCodeLength);
            }
            UtilXml.addChildElementValue(creditCardElement, x.Cvv2Val, cardSecurityCode, document);
            UtilXml.addChildElementValue(creditCardElement, x.Cvv2Indicator, x._1, document);
        }
    }

    private static void appendAddressNode(Element element, GenericValue address) {

        Document document = element.getOwnerDocument();

        Element addressElement = UtilXml.addChildElement(element, x.Address, document);

        UtilXml.addChildElementValue(addressElement, x.Name, address.getString(x.toName), document);
        UtilXml.addChildElementValue(addressElement, x.Street1, address.getString(x.address1), document);
        UtilXml.addChildElementValue(addressElement, x.Street2, address.getString(x.address2), document);
        UtilXml.addChildElementValue(addressElement, x.City, address.getString(x.city), document);
        UtilXml.addChildElementValue(addressElement, x.StateProv, address.getString(x.stateProvinceGeoId), document);
        UtilXml.addChildElementValue(addressElement, x.PostalCode, address.getString(x.postalCode), document);

        String countryGeoId = address.getString(x.countryGeoId);
        if (UtilValidate.isNotEmpty(countryGeoId)) {
            try {
                GenericValue countryGeo = address.getRelatedOne(x.CountryGeo, true);
                UtilXml.addChildElementValue(addressElement, x.Country, countryGeo.getString(x.geoSecCode), document);
            } catch (GenericEntityException gee) {
                Debug.logInfo(gee, x.Error_finding_related_Geo_for_countryGeoId + countryGeoId, MODULE);
            }
        }
    }

    private static void appendTransactionNode(Element element, String type, BigDecimal amount, String currencyCode) {

        Document document = element.getOwnerDocument();

        Element transactionElement = UtilXml.addChildElement(element, x.Transaction, document);
        UtilXml.addChildElementValue(transactionElement, x.Type, type, document);

        // Some transactions will not have an amount (release, reAuth)
        if (amount != null) {
            Element currentTotalsElement = UtilXml.addChildElement(transactionElement, x.CurrentTotals, document);
            Element totalsElement = UtilXml.addChildElement(currentTotalsElement, x.Totals, document);

            // DecimalFormat("#") is used here in case the total is something like 9.9999999...
            // in that case, we want to send 999, not 999.9999999...
            String totalString = amount.setScale(DECIMALS, ROUNDING_MODE).movePointRight(2).toPlainString();

            Element totalElement = UtilXml.addChildElementValue(totalsElement, x.Total, totalString, document);
            totalElement.setAttribute(x.DataType, x.Money);
            totalElement.setAttribute(x.Currency, currencyCode);
        }
    }

    private static Document createRequestDocument(String paymentConfig, Delegator delegator) {

        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument(x.EngineDocList);
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, x.DocVersion, x._1_0, requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, x.EngineDoc, requestDocument);
        UtilXml.addChildElementValue(engineDocElement, x.ContentType, x.OrderFormDoc, requestDocument);

        String sourceId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_sourceId, delegator);
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, x.SourceId, sourceId, requestDocument);
        }

        String groupId = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_groupId, delegator);
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, x.GroupId, groupId, requestDocument);
        }

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, x.User, requestDocument);
        UtilXml.addChildElementValue(userElement, x.Name, EntityUtilProperties.getPropertyValue(paymentConfig,
                x.payment_clearcommerce_username, x.emptyString, delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, x.Password, EntityUtilProperties.getPropertyValue(paymentConfig,
                x.payment_clearcommerce_password, x.emptyString, delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, x.Alias, EntityUtilProperties.getPropertyValue(paymentConfig,
                x.payment_clearcommerce_alias, x.emptyString, delegator), requestDocument);

        String effectiveAlias = EntityUtilProperties.getPropertyValue(paymentConfig,
                x.payment_clearcommerce_effectiveAlias, delegator);
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, x.EffectiveAlias, effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, x.Instructions, requestDocument);

        String pipeline = x.PaymentNoFraud;
        if (EntityUtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, x.payment_clearcommerce_enableFraudShield, x.Y, delegator)) {
            pipeline = x.Payment;
        }
        UtilXml.addChildElementValue(instructionsElement, x.Pipeline, pipeline, requestDocument);

        // EngineDocList.EngineDoc.OrderFormDoc
        Element orderFormDocElement = UtilXml.addChildElement(engineDocElement, x.OrderFormDoc, requestDocument);

        // default to "P" for Production Mode
        String mode = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_processMode, x.P, delegator);
        UtilXml.addChildElementValue(orderFormDocElement, x.Mode, mode, requestDocument);

        return requestDocument;
    }

    private static Document sendRequest(Document requestDocument, String paymentConfig, Delegator delegator) throws ClearCommerceException {
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = x.payment_properties;
        }
        String serverURL = EntityUtilProperties.getPropertyValue(paymentConfig, x.payment_clearcommerce_serverURL, delegator);
        if (UtilValidate.isEmpty(serverURL)) {
            throw new ClearCommerceException(x.Missing_server_URL_check_your_ClearCommerce_configuration);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.ClearCommerce_server_URL + serverURL, MODULE);
        }

        OutputStream os = new ByteArrayOutputStream();

        try {
            UtilXml.writeXmlDocument(requestDocument, os, x.UTF_8, true, false, 0);
        } catch (TransformerException e) {
            throw new ClearCommerceException(x.Error_serializing_requestDocument + e.getMessage());
        }

        String xmlString = os.toString();

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.ClearCommerce_XML_request_string + xmlString, MODULE);
        }

        HttpClient http = new HttpClient(serverURL);
        http.setParameter(x.CLRCMRC_XML, xmlString);

        String response = null;
        try {
            response = http.post();
        } catch (HttpClientException hce) {
            Debug.logInfo(hce, MODULE);
            throw new ClearCommerceException(x.ClearCommerce_connection_problem, hce);
        }

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(response, false);
        } catch (Exception e) {
            throw new ClearCommerceException(x.Error_reading_response_Document_from_a_String + e.getMessage());
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Result_severity_from_clearCommerce + getMessageListMaxSev(responseDocument), MODULE);
        }
        if (Debug.verboseOn() && getMessageListMaxSev(responseDocument) > MAX_SEV_COMP) {
            Debug.logVerbose(x.Returned_messages + getMessageList(responseDocument), MODULE);
        }
        return responseDocument;
    }

}

@SuppressWarnings(x.serial)
class ClearCommerceException extends GeneralException {

    ClearCommerceException() {
        super();
    }

    ClearCommerceException(String msg) {
        super(msg);
    }

    ClearCommerceException(Throwable t) {
        super(t);
    }

    ClearCommerceException(String msg, Throwable t) {
        super(msg, t);
    }
}
