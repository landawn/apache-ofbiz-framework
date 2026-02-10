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
package org.apache.ofbiz.manufacturing.mrp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.MrpEventDao;
import org.apache.ofbiz.persistence.entity.MrpEventEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.InventoryEventPlannedServicesContext;
public class InventoryEventPlannedServices {

    private static final String MODULE = InventoryEventPlannedServices.class.getName();
    private static final String RESOURCE = x.ManufacturingUiLabels;

    /**
     *  Create an MrpEvent.
     *  Make an update if a record exist with same key, (adding the event quantity to the exiting record)
     * @param ctx the dispatch context
     * @param context a map containing the parameters used to create an MrpEvent
     * @return result a map with service status
     */
    public static Map<String, Object> createMrpEvent(DispatchContext ctx, InventoryEventPlannedServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> parameters = UtilMisc.<String, Object>toMap(x.mrpId, context.get(x.mrpId),
                                        x.productId, context.get(x.productId),
                                        x.eventDate, context.get(x.eventDate),
                                        x.mrpEventTypeId, context.get(x.mrpEventTypeId));
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        try {
            createOrUpdateMrpEvent(parameters, quantity, (String) context.get(x.facilityId), (String) context.get(x.eventName), false, delegator);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_findOne_MrpEvent_parameters + parameters, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCreateOrUpdateEvent,
                    UtilMisc.toMap(x.parameters, parameters), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static void createOrUpdateMrpEvent(Map<String, Object> mrpEventKeyMap, BigDecimal newQuantity, String facilityId,
            String eventName, boolean isLate, Delegator delegator) throws GenericEntityException {
        GenericValue mrpEvent = null;
        MrpEventDao mrpEventDao = DaoRegistry.getDao(delegator, x.MrpEvent, MrpEventDao.class);
        MrpEventEntity mrpEventEntity = null;
        try {
            List<Condition> conditionList = new ArrayList<>(mrpEventKeyMap.size());
            for (Map.Entry<String, Object> entry : mrpEventKeyMap.entrySet()) {
                conditionList.add(Filters.eq(entry.getKey(), entry.getValue()));
            }
            mrpEventEntity = mrpEventDao.list(Filters.and(conditionList)).stream().findFirst().orElse(null);
        } catch (Exception e) {
            throw new GenericEntityException(e);
        }
        if (mrpEventEntity != null) {
            mrpEvent = delegator.makeValue(x.MrpEvent, Beans.beanToMap(mrpEventEntity));
        }
        if (mrpEvent == null) {
            mrpEvent = delegator.makeValue(x.MrpEvent, mrpEventKeyMap);
            mrpEvent.put(x.quantity, newQuantity.doubleValue());
            mrpEvent.put(x.eventName, eventName);
            mrpEvent.put(x.facilityId, facilityId);
            mrpEvent.put(x.isLate, (isLate ? x.Y : x.N));
            mrpEvent.create();
        } else {
            BigDecimal qties = newQuantity.add(mrpEvent.getBigDecimal(x.quantity));
            mrpEvent.put(x.quantity, qties.doubleValue());
            if (UtilValidate.isNotEmpty(eventName)) {
                String existingEventName = mrpEvent.getString(x.eventName);
                mrpEvent.put(x.eventName, (UtilValidate.isEmpty(existingEventName) ? eventName : existingEventName + x.str_d3bc9a37 + eventName));
            }
            if (isLate) {
                mrpEvent.put(x.isLate, x.Y);
            }
            mrpEvent.store();
        }
    }
}

