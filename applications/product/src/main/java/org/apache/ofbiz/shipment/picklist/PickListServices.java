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
package org.apache.ofbiz.shipment.picklist;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.OrderHeaderDao;
import org.apache.ofbiz.persistence.dao.PicklistItemDao;
import org.apache.ofbiz.persistence.entity.OrderHeaderEntity;
import org.apache.ofbiz.persistence.entity.PicklistItemEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PickListServicesContext;
public class PickListServices {

    private static final String MODULE = PickListServices.class.getName();

    public static Map<String, Object> convertOrderIdListToHeaders(DispatchContext dctx, PickListServicesContext context) {
        Delegator delegator = dctx.getDelegator();

        List<GenericValue> orderHeaderList = UtilGenerics.cast(context.get(x.orderHeaderList));
        List<String> orderIdList = UtilGenerics.cast(context.get(x.orderIdList));

        // we don't want to process if there is already a header list
        if (orderHeaderList == null) {
            // convert the ID list to headers
            if (orderIdList != null) {
                try {
                    OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
                    Map<String, GenericValue> matchedOrderHeaders = new LinkedHashMap<>();
                    for (String orderId : orderIdList) {
                        if (matchedOrderHeaders.containsKey(orderId)) {
                            continue;
                        }

                        OrderHeaderEntity orderHeaderEntity = orderHeaderDao.get(orderId).orElse(null);
                        if (orderHeaderEntity == null
                                || !x.ORDER_APPROVED.equals(orderHeaderEntity.getStatusId())
                                || !x.SALES_ORDER.equals(orderHeaderEntity.getOrderTypeId())) {
                            continue;
                        }

                        matchedOrderHeaders.put(orderId, delegator.makeValue(x.OrderHeader, Beans.beanToMap(orderHeaderEntity)));
                    }

                    orderHeaderList = EntityUtil.orderBy(new LinkedList<>(matchedOrderHeaders.values()), UtilMisc.toList(x.orderDate));
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logInfo(x.Recieved_orderIdList + orderIdList, MODULE);
                Debug.logInfo(x.Found_orderHeaderList + orderHeaderList, MODULE);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.orderHeaderList, orderHeaderList);
        return result;
    }

    public static boolean isBinComplete(Delegator delegator, String picklistBinId) throws GeneralException {
        try {
            PicklistItemDao picklistItemDao = DaoRegistry.getDao(delegator, x.PicklistItem, PicklistItemDao.class);
            long picklistItemCount = 0L;
            for (PicklistItemEntity picklistItemEntity : picklistItemDao.list(Filters.eq(x.picklistBinId, picklistBinId))) {
                String itemStatusId = picklistItemEntity.getItemStatusId();
                if (!x.PICKITEM_COMPLETED.equals(itemStatusId) && !x.PICKITEM_CANCELLED.equals(itemStatusId)) {
                    picklistItemCount++;
                }
            }
            if (picklistItemCount != 0) {
                return false;
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new GeneralException(e);
        }

        return true;
    }
}
