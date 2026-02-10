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
package org.apache.ofbiz.order.quote;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductStoreEmailSettingDao;
import org.apache.ofbiz.persistence.dao.QuoteDao;
import org.apache.ofbiz.persistence.entity.ProductStoreEmailSettingEntity;
import org.apache.ofbiz.persistence.entity.QuoteEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.QuoteServicesContext;
public class QuoteServices {

    private static final String MODULE = QuoteServices.class.getName();
    private static final String RESOURCE = x.OrderUiLabels;
    private static final String RES_ERROR = x.OrderErrorUiLabels;
    private static final String RES_PRODUCT = x.ProductUiLabels;

    public static Map<String, Object> sendQuoteReportMail(DispatchContext dctx, QuoteServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String emailType = (String) context.get(x.emailType);
        String quoteId = (String) context.get(x.quoteId);
        String sendTo = (String) context.get(x.sendTo);
        String sendCc = (String) context.get(x.sendCc);
        String note = (String) context.get(x.note);

        // prepare the order information
        Map<String, Object> sendMap = new HashMap<>();

        // get the quote and store
        GenericValue quote = null;
        try {
            QuoteDao quoteDao = DaoRegistry.getDao(delegator, x.Quote, QuoteDao.class);
            QuoteEntity quoteEntity = quoteDao.get(quoteId).orElse(null);
            if (quoteEntity != null) {
                quote = delegator.makeValue(x.Quote, Beans.beanToMap(quoteEntity));
            }
        } catch (Exception e) {
            Debug.logError(e, x.Problem_getting_Quote, MODULE);
        }

        if (quote == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderOrderQuoteCannotBeFound,
                    UtilMisc.toMap(x.quoteId, quoteId), locale));
        }

        GenericValue productStoreEmail = null;
        try {
            ProductStoreEmailSettingDao productStoreEmailSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting,
                    ProductStoreEmailSettingDao.class);
            ProductStoreEmailSettingEntity productStoreEmailSettingEntity = productStoreEmailSettingDao
                    .list(Filters.and(Filters.eq(x.productStoreId, quote.get(x.productStoreId)), Filters.eq(x.emailType, emailType)))
                    .stream().findFirst().orElse(null);
            if (productStoreEmailSettingEntity != null) {
                productStoreEmail = delegator.makeValue(x.ProductStoreEmailSetting, Beans.beanToMap(productStoreEmailSettingEntity));
            }
        } catch (Exception e) {
            Debug.logError(e, x.Problem_getting_the_ProductStoreEmailSetting_for_productStoreId + quote.get(x.productStoreId)
                    + x.and_emailType + emailType, MODULE);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_PRODUCT,
                    x.ProductProductStoreEmailSettingsNotValid,
                    UtilMisc.toMap(x.productStoreId, quote.get(x.productStoreId),
                            x.emailType, emailType), locale));
        }
        String bodyScreenLocation = productStoreEmail.getString(x.bodyScreenLocation);
        if (UtilValidate.isEmpty(bodyScreenLocation)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_PRODUCT,
                    x.ProductProductStoreEmailSettingsNotValidBodyScreenLocation,
                    UtilMisc.toMap(x.productStoreId, quote.get(x.productStoreId),
                            x.emailType, emailType), locale));
        }
        sendMap.put(x.bodyScreenUri, bodyScreenLocation);
        String xslfoAttachScreenLocation = productStoreEmail.getString(x.xslfoAttachScreenLocation);
        sendMap.put(x.xslfoAttachScreenLocation, xslfoAttachScreenLocation);

        if ((sendTo == null) || !UtilValidate.isEmail(sendTo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT,
                    x.ProductProductStoreEmailSettingsNoSendToFound, locale));
        }

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap(x.quoteId, quoteId, x.userLogin, userLogin, x.locale, locale);
        bodyParameters.put(x.note, note);
        bodyParameters.put(x.partyId, quote.getString(x.partyId)); // This is set to trigger the "storeEmailAsCommunication" seca
        sendMap.put(x.bodyParameters, bodyParameters);
        sendMap.put(x.userLogin, userLogin);

        String subjectString = productStoreEmail.getString(x.subject);
        sendMap.put(x.subject, subjectString);

        sendMap.put(x.contentType, productStoreEmail.get(x.contentType));
        sendMap.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
        sendMap.put(x.sendCc, productStoreEmail.get(x.ccAddress));
        sendMap.put(x.sendBcc, productStoreEmail.get(x.bccAddress));
        sendMap.put(x.sendTo, sendTo);
        if ((sendCc != null) && UtilValidate.isEmail(sendCc)) {
            sendMap.put(x.sendCc, sendCc);
        } else {
            sendMap.put(x.sendCc, productStoreEmail.get(x.ccAddress));
        }

        // send the notification
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync(x.sendMailFromScreen, sendMap);
            if (ServiceUtil.isError(sendResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(sendResp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderServiceExceptionSeeLogs, locale));
        }

        // check for errors
        if (sendResp != null && ServiceUtil.isSuccess(sendResp)) {
            sendResp.put(x.emailType, emailType);
        }
        return sendResp;
    }

    public static Map<String, Object> storeQuote(DispatchContext dctx, QuoteServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String quoteTypeId = (String) context.get(x.quoteTypeId);
        String partyId = (String) context.get(x.partyId);
        Timestamp issueDate = (Timestamp) context.get(x.issueDate);
        String statusId = (String) context.get(x.statusId);
        String currencyUomId = (String) context.get(x.currencyUomId);
        String productStoreId = (String) context.get(x.productStoreId);
        String salesChannelEnumId = (String) context.get(x.salesChannelEnumId);
        Timestamp validFromDate = (Timestamp) context.get(x.validFromDate);
        Timestamp validThruDate = (Timestamp) context.get(x.validThruDate);
        String quoteName = (String) context.get(x.quoteName);
        String description = (String) context.get(x.description);
        List<GenericValue> quoteItems = UtilGenerics.cast(context.get(x.quoteItems));
        List<GenericValue> quoteAttributes = UtilGenerics.cast(context.get(x.quoteAttributes));
        List<GenericValue> quoteCoefficients = UtilGenerics.cast(context.get(x.quoteCoefficients));
        List<GenericValue> quoteRoles = UtilGenerics.cast(context.get(x.quoteRoles));
        List<GenericValue> quoteWorkEfforts = UtilGenerics.cast(context.get(x.quoteWorkEfforts));
        List<GenericValue> quoteAdjustments = UtilGenerics.cast(context.get(x.quoteAdjustments));
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();

        //TODO create Quote Terms still to be implemented
        //TODO create Quote Term Attributes still to be implemented
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> quoteIn = UtilMisc.toMap(x.quoteTypeId, quoteTypeId, x.partyId, partyId, x.issueDate, issueDate,
                    x.statusId, statusId, x.currencyUomId, currencyUomId);
            quoteIn.put(x.productStoreId, productStoreId);
            quoteIn.put(x.salesChannelEnumId, salesChannelEnumId);
            quoteIn.put(x.productStoreId, productStoreId);
            quoteIn.put(x.validFromDate, validFromDate);
            quoteIn.put(x.validThruDate, validThruDate);
            quoteIn.put(x.quoteName, quoteName);
            quoteIn.put(x.description, description);
            if (userLogin != null) {
                quoteIn.put(x.userLogin, userLogin);
            }


            // create Quote
            Map<String, Object> quoteOut = dispatcher.runSync(x.createQuote, quoteIn);
            if (ServiceUtil.isError(quoteOut)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(quoteOut));
            }
            if (UtilValidate.isNotEmpty(quoteOut) && UtilValidate.isNotEmpty(quoteOut.get(x.quoteId))) {
                String quoteId = (String) quoteOut.get(x.quoteId);
                result.put(x.quoteId, quoteId);

                // create Quote Items
                if (UtilValidate.isNotEmpty(quoteItems)) {
                    for (GenericValue quoteItem : quoteItems) {
                        quoteItem.set(x.quoteId, quoteId);
                        Map<String, Object> quoteItemIn = quoteItem.getAllFields();
                        quoteItemIn.put(x.userLogin, userLogin);

                        serviceResult = dispatcher.runSync(x.createQuoteItem, quoteItemIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Attributes
                if (UtilValidate.isNotEmpty(quoteAttributes)) {
                    for (GenericValue quoteAttr : quoteAttributes) {
                        quoteAttr.set(x.quoteId, quoteId);
                        Map<String, Object> quoteAttrIn = quoteAttr.getAllFields();
                        quoteAttrIn.put(x.userLogin, userLogin);

                        serviceResult = dispatcher.runSync(x.createQuoteAttribute, quoteAttrIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Coefficients
                if (UtilValidate.isNotEmpty(quoteCoefficients)) {
                    for (GenericValue quoteCoefficient : quoteCoefficients) {
                        quoteCoefficient.set(x.quoteId, quoteId);
                        Map<String, Object> quoteCoefficientIn = quoteCoefficient.getAllFields();
                        quoteCoefficientIn.put(x.userLogin, userLogin);

                        serviceResult = dispatcher.runSync(x.createQuoteCoefficient, quoteCoefficientIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Roles
                if (UtilValidate.isNotEmpty(quoteRoles)) {
                    for (GenericValue quoteRole : quoteRoles) {
                        quoteRole.set(x.quoteId, quoteId);
                        Map<String, Object> quoteRoleIn = quoteRole.getAllFields();
                        quoteRoleIn.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.createQuoteRole, quoteRoleIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote WorkEfforts
                if (UtilValidate.isNotEmpty(quoteWorkEfforts)) {
                    for (GenericValue quoteWorkEffort : quoteWorkEfforts) {
                        quoteWorkEffort.set(x.quoteId, quoteId);
                        Map<String, Object> quoteWorkEffortIn = quoteWorkEffort.getAllFields();
                        quoteWorkEffortIn.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.createQuoteWorkEffort, quoteWorkEffortIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Adjustments
                if (UtilValidate.isNotEmpty(quoteAdjustments)) {
                    for (GenericValue quoteAdjustment : quoteAdjustments) {
                        quoteAdjustment.set(x.quoteId, quoteId);
                        Map<String, Object> quoteAdjustmentIn = quoteAdjustment.getAllFields();
                        quoteAdjustmentIn.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.createQuoteAdjustment, quoteAdjustmentIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                //TODO create Quote Terms still to be implemented the base service createQuoteTerm
                //TODO create Quote Term Attributes still to be implemented the base service createQuoteTermAttribute
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                        x.OrderOrderQuoteCannotBeStored, locale));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_storing_Quote, MODULE);
        }

        return result;
    }
}
