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
package org.apache.ofbiz.product.subscription;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.uom.UomWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.SubscriptionServicesContext;
/**
 * Subscription Services
 */
public class SubscriptionServices {

    private static final String MODULE = SubscriptionServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final String RES_ERROR = x.ProductErrorUiLabels;
    private static final String RES_ORDER_ERROR = x.OrderErrorUiLabels;

    public static Map<String, Object> processExtendSubscription(DispatchContext dctx, SubscriptionServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get(x.partyId);
        String subscriptionResourceId = (String) context.get(x.subscriptionResourceId);
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        String roleTypeId = (String) context.get(x.useRoleTypeId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Integer useTime = (Integer) context.get(x.useTime);
        String useTimeUomId = (String) context.get(x.useTimeUomId);
        String alwaysCreateNewRecordStr = (String) context.get(x.alwaysCreateNewRecord);
        Locale locale = (Locale) context.get(x.locale);
        boolean alwaysCreateNewRecord = !x.N.equals(alwaysCreateNewRecordStr);

        GenericValue lastSubscription = null;
        try {
            UserLoginDao subscriptionDao = DaoRegistry.getDao(delegator, x.Subscription, UserLoginDao.class);
            Map<String, String> subscriptionFindMap = UtilMisc.toMap(x.partyId, partyId, x.subscriptionResourceId, subscriptionResourceId);
            // if this subscription is attached to something the customer owns, filter by that too
            if (UtilValidate.isNotEmpty(inventoryItemId)) {
                subscriptionFindMap.put(x.inventoryItemId, inventoryItemId);
            }
            List<GenericValue> subscriptionList = subscriptionDao.findByAnd(delegator, x.Subscription, subscriptionFindMap, null, false);
            // DEJ20070718 DON'T filter by date, we want to consider all subscriptions: List listFiltered
            // = EntityUtil.filterByDate(subscriptionList, true);
            List<GenericValue> listOrdered = EntityUtil.orderBy(subscriptionList, UtilMisc.toList(x.fromDate_f5440273));
            if (!listOrdered.isEmpty()) {
                lastSubscription = listOrdered.get(0);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        GenericValue newSubscription = null;
        if (lastSubscription == null || alwaysCreateNewRecord) {
            newSubscription = delegator.makeValue(x.Subscription);
            newSubscription.set(x.subscriptionResourceId, subscriptionResourceId);
            newSubscription.set(x.partyId, partyId);
            newSubscription.set(x.roleTypeId, roleTypeId);
            newSubscription.set(x.productId, context.get(x.productId));
            newSubscription.set(x.orderId, context.get(x.orderId));
            newSubscription.set(x.orderItemSeqId, context.get(x.orderItemSeqId));
            newSubscription.set(x.automaticExtend, context.get(x.automaticExtend));
            newSubscription.set(x.canclAutmExtTimeUomId, context.get(x.canclAutmExtTimeUomId));
            newSubscription.set(x.canclAutmExtTime, context.get(x.canclAutmExtTime));
        } else {
            newSubscription = lastSubscription;
        }
        newSubscription.set(x.inventoryItemId, inventoryItemId);

        Timestamp thruDate = lastSubscription != null ? (Timestamp) lastSubscription.get(x.thruDate) : null;

        // set the fromDate, one way or another
        if (thruDate == null) {
            // no thruDate? start with NOW
            thruDate = nowTimestamp;
            newSubscription.set(x.fromDate, nowTimestamp);
        } else {
            // there is a thru date... if it is in the past, bring it up to NOW before adding on the time period
            // don't want to penalize for skipping time, in other words if they had a subscription last year for a
            // month and buy another month, we want that second month to start now and not last year
            if (thruDate.before(nowTimestamp)) {
                thruDate = nowTimestamp;
            }
            newSubscription.set(x.fromDate, thruDate);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(thruDate);
        int[] times = UomWorker.uomTimeToCalTime(useTimeUomId);
        if (times != null) {
            calendar.add(times[0], (useTime * times[1]));
        } else {
            Debug.logWarning(x.Don_t_know_anything_about_useTimeUomId + useTimeUomId + x.defaulting_to_month, MODULE);
            calendar.add(Calendar.MONTH, useTime);
        }

        thruDate = new Timestamp(calendar.getTimeInMillis());
        newSubscription.set(x.thruDate, thruDate);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            if (lastSubscription != null && !alwaysCreateNewRecord) {
                Map<String, Object> updateSubscriptionMap = dctx.getModelService(x.updateSubscription)
                        .makeValid(newSubscription, ModelService.IN_PARAM);
                UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                updateSubscriptionMap.put(x.userLogin, userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.system), false));

                Map<String, Object> updateSubscriptionResult = dispatcher.runSync(x.updateSubscription, updateSubscriptionMap);
                result.put(x.subscriptionId, updateSubscriptionMap.get(x.subscriptionId));
                if (ServiceUtil.isError(updateSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductSubscriptionUpdateError,
                            UtilMisc.toMap(x.subscriptionId, updateSubscriptionMap.get(x.subscriptionId)), locale),
                            null, null, updateSubscriptionResult);
                }
            } else {
                Map<String, Object> ensurePartyRoleMap = new HashMap<>();
                if (UtilValidate.isNotEmpty(roleTypeId)) {
                    ensurePartyRoleMap.put(x.partyId, partyId);
                    ensurePartyRoleMap.put(x.roleTypeId, roleTypeId);
                    ensurePartyRoleMap.put(x.userLogin, userLogin);
                    Map<String, Object> createPartyRoleResult = dispatcher.runSync(x.ensurePartyRole, ensurePartyRoleMap);
                    if (ServiceUtil.isError(createPartyRoleResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductSubscriptionPartyRoleCreationError,
                                UtilMisc.toMap(x.subscriptionResourceId, subscriptionResourceId), locale),
                                null, null, createPartyRoleResult);
                    }
                }
                Map<String, Object> createSubscriptionMap = dctx.getModelService(x.createSubscription)
                        .makeValid(newSubscription, ModelService.IN_PARAM);
                UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                createSubscriptionMap.put(x.userLogin, userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, x.system), false));

                Map<String, Object> createSubscriptionResult = dispatcher.runSync(x.createSubscription, createSubscriptionMap);
                if (ServiceUtil.isError(createSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductSubscriptionCreateError,
                            UtilMisc.toMap(x.subscriptionResourceId, subscriptionResourceId), locale),
                            null, null, createSubscriptionResult);
                }
                result.put(x.subscriptionId, createSubscriptionResult.get(x.subscriptionId));
            }
        } catch (GenericEntityException | GenericServiceException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> processExtendSubscriptionByProduct(DispatchContext dctx, SubscriptionServicesContext context)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get(x.productId);
        Integer qty = (Integer) context.get(x.quantity);
        Locale locale = (Locale) context.get(x.locale);
        if (qty == null) {
            qty = 1;
        }

        Timestamp orderCreatedDate = (Timestamp) context.get(x.orderCreatedDate);
        if (orderCreatedDate == null) {
            orderCreatedDate = UtilDateTime.nowTimestamp();
        }
        try {
            UserLoginDao productSubscriptionResourceDao = DaoRegistry.getDao(delegator, x.ProductSubscriptionResource, UserLoginDao.class);
            List<GenericValue> productSubscriptionResourceList = productSubscriptionResourceDao.findByAnd(delegator, x.ProductSubscriptionResource,
                    UtilMisc.toMap(x.productId, productId), null, true);
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, x.fromDate, x.thruDate,
                    true);
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, x.purchaseFromDate,
                    x.purchaseThruDate, true);

            if (productSubscriptionResourceList.isEmpty()) {
                Debug.logError(x.No_ProductSubscriptionResource_found_for_productId + productId, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductSubscriptionResourceNotFound,
                        UtilMisc.toMap(x.productId, productId), locale));
            }

            for (GenericValue productSubscriptionResource: productSubscriptionResourceList) {
                Long useTime = productSubscriptionResource.getLong(x.useTime);
                Integer newUseTime = 0;
                if (useTime != null) {
                    newUseTime = useTime.intValue() * qty;
                }
                Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
                subContext.put(x.useTime, newUseTime);
                subContext.put(x.useTimeUomId, productSubscriptionResource.get(x.useTimeUomId));
                subContext.put(x.useRoleTypeId, productSubscriptionResource.get(x.useRoleTypeId));
                subContext.put(x.subscriptionResourceId, productSubscriptionResource.get(x.subscriptionResourceId));
                subContext.put(x.automaticExtend, productSubscriptionResource.get(x.automaticExtend));
                subContext.put(x.canclAutmExtTime, productSubscriptionResource.get(x.canclAutmExtTime));
                subContext.put(x.canclAutmExtTimeUomId, productSubscriptionResource.get(x.canclAutmExtTimeUomId));
                subContext.put(x.gracePeriodOnExpiry, productSubscriptionResource.get(x.gracePeriodOnExpiry));
                subContext.put(x.gracePeriodOnExpiryUomId, productSubscriptionResource.get(x.gracePeriodOnExpiryUomId));

                Map<String, Object> ctx = dctx.getModelService(x.processExtendSubscription).makeValid(subContext, ModelService.IN_PARAM);
                Map<String, Object> processExtendSubscriptionResult = dispatcher.runSync(x.processExtendSubscription, ctx);
                if (ServiceUtil.isError(processExtendSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductSubscriptionByProductError,
                            UtilMisc.toMap(x.productId, productId), locale),
                            null, null, processExtendSubscriptionResult);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> processExtendSubscriptionByOrder(DispatchContext dctx, SubscriptionServicesContext context)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        String orderId = (String) context.get(x.orderId);
        Locale locale = (Locale) context.get(x.locale);

        Debug.logInfo(x.In_processExtendSubscriptionByOrder_service_with_orderId + orderId, MODULE);

        GenericValue orderHeader = null;
        try {
            UserLoginDao orderRoleDao = DaoRegistry.getDao(delegator, x.OrderRole, UserLoginDao.class);
            List<GenericValue> orderRoleList = orderRoleDao.findByAnd(delegator, x.OrderRole,
                    UtilMisc.toMap(x.orderId, orderId, x.roleTypeId, x.END_USER_CUSTOMER), null, false);
            if (!orderRoleList.isEmpty()) {
                GenericValue orderRole = orderRoleList.get(0);
                String partyId = (String) orderRole.get(x.partyId);
                subContext.put(x.partyId, partyId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorCannotGetOrderRoleEntity,
                        UtilMisc.toMap(x.itemMsgInfo, orderId), locale));
            }
            UserLoginDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class);
            orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                        x.OrderErrorNoValidOrderHeaderFoundForOrderId,
                        UtilMisc.toMap(x.orderId, orderId), locale));
            }
            Timestamp orderCreatedDate = (Timestamp) orderHeader.get(x.orderDate);
            subContext.put(x.orderCreatedDate, orderCreatedDate);
            List<GenericValue> orderItemList = orderHeader.getRelated(x.OrderItem, null, null, false);
            for (GenericValue orderItem: orderItemList) {
                BigDecimal qty = orderItem.getBigDecimal(x.quantity);
                String productId = orderItem.getString(x.productId);
                if (UtilValidate.isEmpty(productId)) {
                    continue;
                }
                UserLoginDao productSubscriptionResourceDao = DaoRegistry.getDao(delegator, x.ProductSubscriptionResource, UserLoginDao.class);
                List<GenericValue> productSubscriptionResourceListFiltered = productSubscriptionResourceDao.findByAnd(delegator,
                        x.ProductSubscriptionResource, UtilMisc.toMap(x.productId, productId), null, true);
                productSubscriptionResourceListFiltered = EntityUtil.filterByDate(productSubscriptionResourceListFiltered, true);
                if (!productSubscriptionResourceListFiltered.isEmpty()) {
                    subContext.put(x.subscriptionTypeId, x.PRODUCT_SUBSCR);
                    subContext.put(x.productId, productId);
                    subContext.put(x.orderId, orderId);
                    subContext.put(x.orderItemSeqId, orderItem.get(x.orderItemSeqId));
                    subContext.put(x.inventoryItemId, orderItem.get(x.fromInventoryItemId));
                    subContext.put(x.quantity, qty.intValue());
                    Map<String, Object> ctx = dctx.getModelService(x.processExtendSubscriptionByProduct).makeValid(subContext,
                            ModelService.IN_PARAM);
                    Map<String, Object> thisResult = dispatcher.runSync(x.processExtendSubscriptionByProduct, ctx);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductSubscriptionByOrderError,
                                UtilMisc.toMap(x.orderId, orderId), locale), null, null, thisResult);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> runServiceOnSubscriptionExpiry(DispatchContext dctx, SubscriptionServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> expiryMap = new HashMap<>();
        String gracePeriodOnExpiry = null;
        String gracePeriodOnExpiryUomId = null;
        String subscriptionId = null;
        Timestamp expirationCompletedDate = null;

        try {
            EntityCondition cond1 = EntityCondition.makeCondition(x.automaticExtend, EntityOperator.EQUALS, x.N);
            EntityCondition cond2 = EntityCondition.makeCondition(x.automaticExtend, EntityOperator.EQUALS, null);
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(cond1, cond2), EntityOperator.OR);
            List<GenericValue> subscriptionList = null;
            UserLoginDao subscriptionDao = DaoRegistry.getDao(delegator, x.Subscription, UserLoginDao.class);
            subscriptionList = subscriptionDao.findByCondition(delegator, x.Subscription, cond, null, null, null, false);

            if (subscriptionList != null) {
                for (GenericValue subscription : subscriptionList) {
                    expirationCompletedDate = subscription.getTimestamp(x.expirationCompletedDate);
                    if (expirationCompletedDate == null) {
                        Calendar currentDate = Calendar.getInstance();
                        currentDate.setTime(UtilDateTime.nowTimestamp());
                        // check if the thruDate + grace period (if provided) is earlier than today's date
                        Calendar endDateSubscription = Calendar.getInstance();
                        int field = Calendar.MONTH;
                        String subscriptionResourceId = subscription.getString(x.subscriptionResourceId);
                        GenericValue subscriptionResource = null;
                        UserLoginDao subscriptionResourceDao = DaoRegistry.getDao(delegator, x.SubscriptionResource, UserLoginDao.class);
                        subscriptionResource = subscriptionResourceDao.findOne(delegator, x.SubscriptionResource,
                                UtilMisc.toMap(x.subscriptionResourceId, subscriptionResourceId), false);
                        subscriptionId = subscription.getString(x.subscriptionId);
                        gracePeriodOnExpiry = subscription.getString(x.gracePeriodOnExpiry);
                        gracePeriodOnExpiryUomId = subscription.getString(x.gracePeriodOnExpiryUomId);
                        String serviceNameOnExpiry = subscriptionResource.getString(x.serviceNameOnExpiry);
                        endDateSubscription.setTime(subscription.getTimestamp(x.thruDate));

                        if (gracePeriodOnExpiry != null && gracePeriodOnExpiryUomId != null) {
                            if (x.TF_day.equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.DAY_OF_YEAR;
                            } else if (x.TF_wk.equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.WEEK_OF_YEAR;
                            } else if (x.TF_mon.equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.MONTH;
                            } else if (x.TF_yr.equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.YEAR;
                            } else {
                                Debug.logWarning(x.Don_t_know_anything_about_gracePeriodOnExpiryUomId + gracePeriodOnExpiryUomId
                                        + x.defaulting_to_month, MODULE);
                            }
                            endDateSubscription.add(field, Integer.parseInt(gracePeriodOnExpiry));
                        }
                        if ((currentDate.after(endDateSubscription) || currentDate.equals(endDateSubscription)) && serviceNameOnExpiry != null) {
                            if (userLogin != null) {
                                expiryMap.put(x.userLogin, userLogin);
                            }
                            if (subscriptionId != null) {
                                expiryMap.put(x.subscriptionId, subscriptionId);
                            }
                            result = dispatcher.runSync(serviceNameOnExpiry, expiryMap);
                            if (ServiceUtil.isSuccess(result)) {
                                subscription.set(x.expirationCompletedDate, UtilDateTime.nowTimestamp());
                                delegator.store(subscription);
                                Debug.logInfo(x.Subscription_expired_successfully_for_subscription_ID + subscriptionId, MODULE);
                            } else if (ServiceUtil.isError(result)) {
                                result = null;
                                Debug.logError(x.Error_expiring_subscription_while_processing_with_subscriptionId + subscriptionId, MODULE);
                            }

                            if (result != null && subscriptionId != null) {
                                Debug.logInfo(x.Service_mentioned_in_serviceNameOnExpiry_called_with_result
                                        + ServiceUtil.makeSuccessMessage(result, x.emptyString, x.emptyString, x.emptyString, x.emptyString), MODULE);
                            } else if (result == null && subscriptionId != null) {
                                Debug.logError(x.Subscription_couldn_t_be_expired_for_subscriptionId + subscriptionId, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductSubscriptionCouldntBeExpired,
                                        UtilMisc.toMap(x.subscriptionId, subscriptionId), locale));
                            }
                        }
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(x.Error_while_calling_service_specified_in_serviceNameOnExpiry, MODULE);
            return ServiceUtil.returnError(e.toString());
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return result;
    }

    public static Map<String, Object> runSubscriptionExpired(DispatchContext dctx, SubscriptionServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        String subscriptionId = (String) context.get(x.subscriptionId);
        Map<String, Object> result = new HashMap<>();
        if (subscriptionId != null) {
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE, x.ProductRunSubscriptionExpiredServiceCalledSuccessfully, locale));
        }
        return result;
    }
}
