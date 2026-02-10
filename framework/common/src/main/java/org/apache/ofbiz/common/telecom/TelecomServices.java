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
package org.apache.ofbiz.common.telecom;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductStoreTelecomSettingDao;
import org.apache.ofbiz.persistence.entity.ProductStoreTelecomSettingEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.TelecomServicesContext;
public class TelecomServices {

    private static final String MODULE = TelecomServices.class.getName();

    public static Map<String, Object> sendTelecomMessage(DispatchContext ctx, TelecomServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Map<String, Object> results = ServiceUtil.returnSuccess();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String telecomMsgTypeEnumId = (String) context.get(x.telecomMsgTypeEnumId);
        String telecomMethodTypeId = (String) context.get(x.telecomMethodTypeId);
        String telecomGatewayConfigId = (String) context.get(x.telecomGatewayConfigId);
        List<String> numbers = checkCollection(context.get(x.numbers), String.class);
        String message = (String) context.get(x.message);
        String telecomEnabled = EntityUtilProperties.getPropertyValue(x.general, x.telecom_notifications_enabled, delegator);
        if (!x.Y.equals(telecomEnabled)) {
            Debug.logImportant(x.Telecom_message_not_sent_to + numbers.toString()
                    + x.because_telecom_notifications_enabled_property_is_set_to_N_or_empty, MODULE);
            return ServiceUtil.returnSuccess(x.Telecom_message_not_sent_to + numbers.toString()
                    + x.because_sms_notifications_enabled_property_is_set_to_N_or_empty);
        }

        String redirectNumber = EntityUtilProperties.getPropertyValue(x.general, x.telecom_notifications_redirectTo, delegator);
        if (UtilValidate.isNotEmpty(redirectNumber)) {
            numbers.clear();
            numbers.add(redirectNumber);
        }

        try {
            Map<String, Object> createCommEventCtx = new HashMap<>();
            createCommEventCtx = ctx.makeValidContext(x.createCommunicationEvent, ModelService.IN_PARAM, context);
            createCommEventCtx.put(x.content, message);
            createCommEventCtx.put(x.communicationEventTypeId, x.PHONE_COMMUNICATION);
            createCommEventCtx.put(x.fromString, EntityUtilProperties.getPropertyValue(x.general, x.defaultFromTelecomAddress, delegator));
            createCommEventCtx.put(x.subject, telecomMsgTypeEnumId);
            createCommEventCtx.put(x.toString, numbers.toString());
            Map<String, Object> createCommEventResult = dispatcher.runSync(x.createCommunicationEvent, createCommEventCtx);
            if (!ServiceUtil.isSuccess(createCommEventResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(createCommEventResult), MODULE);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createCommEventResult));
            }
            String communicationEventId = (String) createCommEventResult.get(x.communicationEventId);

            Map<String, Object> conditions = new HashMap<>();
            conditions.put(x.productStoreId, productStoreId);
            conditions.put(x.telecomMsgTypeEnumId, telecomMsgTypeEnumId);
            conditions.put(x.telecomMethodTypeId, telecomMethodTypeId);
            List<Condition> conditionList = new ArrayList<>(conditions.size());
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                conditionList.add(Filters.eq(entry.getKey(), entry.getValue()));
            }
            ProductStoreTelecomSettingDao productStoreTelecomSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreTelecomSetting,
                    ProductStoreTelecomSettingDao.class);
            ProductStoreTelecomSettingEntity productStoreTelecomSettingEntity =
                    productStoreTelecomSettingDao.list(Filters.and(conditionList)).stream().findFirst().orElse(null);
            GenericValue productStoreTelecomSetting = null;
            if (productStoreTelecomSettingEntity != null) {
                productStoreTelecomSetting = delegator.makeValue(x.ProductStoreTelecomSetting, Beans.beanToMap(productStoreTelecomSettingEntity));
            }
            if (productStoreTelecomSetting != null) {
                GenericValue customMethod = productStoreTelecomSetting.getRelatedOne(x.CustomMethod, false);
                if (UtilValidate.isNotEmpty(customMethod.getString(x.customMethodName))) {
                    Map<String, Object> serviceCtx = new HashMap<>();
                    serviceCtx.put(x.numbers, numbers);
                    serviceCtx.put(x.message, message);
                    if (telecomGatewayConfigId != null) {
                        serviceCtx.put(x.configId, telecomGatewayConfigId);
                    }
                    serviceCtx.put(x.userLogin, userLogin);
                    Map<String, Object> customMethodResult = dispatcher.runSync(customMethod.getString(x.customMethodName), serviceCtx);
                    if (ServiceUtil.isError(customMethodResult) || ServiceUtil.isFailure(customMethodResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(customMethodResult);
                        Debug.logError(errorMessage, MODULE);
                        return ServiceUtil.returnError(errorMessage);
                    }

                    createCommEventCtx.clear();
                    createCommEventCtx.put(x.communicationEventId, communicationEventId);
                    if (UtilValidate.isNotEmpty(createCommEventResult.get(x.response))) {
                        createCommEventCtx.put(x.note, customMethodResult.get(x.response));
                    }
                    createCommEventCtx.put(x.statusId, x.COM_COMPLETE);
                    createCommEventCtx.put(x.userLogin, userLogin);
                    dispatcher.runSync(x.updateCommunicationEvent, createCommEventCtx);
                }
            } else {
                return ServiceUtil.returnError(x.Not_sending_SMS_as_no_ProductStoreEmailSetting_found_for_the_passed_inputs);
            }
        } catch (GenericEntityException | GenericServiceException | SQLException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }
}

