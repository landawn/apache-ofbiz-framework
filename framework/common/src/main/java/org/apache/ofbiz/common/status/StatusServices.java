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
package org.apache.ofbiz.common.status;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.StatusItemDao;
import org.apache.ofbiz.persistence.dao.StatusValidChangeDao;
import org.apache.ofbiz.persistence.entity.StatusItemEntity;
import org.apache.ofbiz.persistence.entity.StatusValidChangeEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.StatusServicesContext;
/**
 * StatusServices
 */
public class StatusServices {

    private static final String MODULE = StatusServices.class.getName();
    private static final String RESOURCE = x.CommonUiLabels;

    public static Map<String, Object> getStatusItems(DispatchContext ctx, StatusServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        List<String> statusTypes = checkCollection(context.get(x.statusTypeIds), String.class);
        Locale locale = (Locale) context.get(x.locale);
        if (UtilValidate.isEmpty(statusTypes)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonStatusMandatory, locale));
        }

        List<GenericValue> statusItems = new LinkedList<>();
        for (String statusTypeId: statusTypes) {
            try {
                StatusItemDao statusItemDao = DaoRegistry.getDao(delegator, x.StatusItem, StatusItemDao.class);
                List<GenericValue> myStatusItems = new LinkedList<>();
                for (StatusItemEntity statusItemEntity : statusItemDao.list(Filters.eq(x.statusTypeId, statusTypeId))) {
                    myStatusItems.add(delegator.makeValue(x.StatusItem, Beans.beanToMap(statusItemEntity)));
                }
                myStatusItems = EntityUtil.orderBy(myStatusItems, UtilMisc.toList(x.sequenceId));
                statusItems.addAll(myStatusItems);
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put(x.statusItems, statusItems);
        return ret;
    }

    public static Map<String, Object> getStatusValidChangeToDetails(DispatchContext ctx, StatusServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        List<GenericValue> statusValidChangeToDetails = null;
        String statusId = (String) context.get(x.statusId);
        try {
            StatusValidChangeDao statusValidChangeDao = DaoRegistry.getDao(delegator, x.StatusValidChange, StatusValidChangeDao.class);
            StatusItemDao statusItemDao = DaoRegistry.getDao(delegator, x.StatusItem, StatusItemDao.class);
            statusValidChangeToDetails = new LinkedList<>();

            for (StatusValidChangeEntity statusValidChangeEntity : statusValidChangeDao.list(Filters.eq(x.statusId, statusId))) {
                StatusItemEntity statusItemEntity = statusItemDao.get(statusValidChangeEntity.getStatusIdTo()).orElse(null);
                if (statusItemEntity == null) {
                    continue;
                }

                Map<String, Object> statusValidChangeFields = Beans.beanToMap(statusValidChangeEntity);
                Map<String, Object> statusItemFields = Beans.beanToMap(statusItemEntity);
                statusItemFields.remove(x.statusId);
                statusValidChangeFields.putAll(statusItemFields);

                statusValidChangeToDetails.add(delegator.makeValue(x.StatusValidChangeToDetail, statusValidChangeFields));
            }
            statusValidChangeToDetails = EntityUtil.orderBy(statusValidChangeToDetails, UtilMisc.toList(x.sequenceId));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
        Map<String, Object> ret = ServiceUtil.returnSuccess();
        if (statusValidChangeToDetails != null) {
            ret.put(x.statusValidChangeToDetails, statusValidChangeToDetails);
        }
        return ret;
    }
}
