/*
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
 */

package org.apache.ofbiz.order.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.thirdparty.paypal.ExpressCheckoutEvents;
import org.apache.ofbiz.persistence.dao.BillingAccountDao;
import org.apache.ofbiz.persistence.dao.BillingAccountRoleDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FinAccountRoleDao;
import org.apache.ofbiz.persistence.dao.ItemIssuanceDao;
import org.apache.ofbiz.persistence.dao.OrderAdjustmentDao;
import org.apache.ofbiz.persistence.dao.OrderHeaderDao;
import org.apache.ofbiz.persistence.dao.OrderItemAssocDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGrpInvResDao;
import org.apache.ofbiz.persistence.dao.OrderPaymentPreferenceDao;
import org.apache.ofbiz.persistence.dao.PaymentMethodTypeDao;
import org.apache.ofbiz.persistence.dao.ProductStoreEmailSettingDao;
import org.apache.ofbiz.persistence.dao.ReturnAdjustmentDao;
import org.apache.ofbiz.persistence.dao.ReturnHeaderDao;
import org.apache.ofbiz.persistence.dao.ReturnItemDao;
import org.apache.ofbiz.persistence.dao.ReturnItemResponseDao;
import org.apache.ofbiz.persistence.dao.ReturnItemTypeMapDao;
import org.apache.ofbiz.persistence.dao.SubscriptionDao;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.OrderReturnServicesContext;
/**
 * OrderReturnServices
 */
public class OrderReturnServices {

    private static final String MODULE = OrderReturnServices.class.getName();
    private static final String RESOURCE = x.OrderUiLabels;
    private static final String RES_ERROR = x.OrderErrorUiLabels;
    private static final String RES_PRODUCT = x.ProductUiLabels;

    //  set some BigDecimal properties
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.invoice_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.invoice_rounding);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    // locate the return item's initial inventory item cost
    public static Map<String, Object> getReturnItemInitialCost(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        String returnItemSeqId = (String) context.get(x.returnItemSeqId);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.initialItemCost, getReturnItemInitialCost(delegator, returnId, returnItemSeqId));
        return result;
    }

    // obtain order/return total information
    public static Map<String, Object> getOrderAvailableReturnedTotal(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get(x.orderId);
        OrderReadHelper orh = null;
        try {
            orh = new OrderReadHelper(delegator, orderId);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // an adjustment value to test
        BigDecimal adj = (BigDecimal) context.get(x.adjustment);
        if (adj == null) {
            adj = ZERO;
        }

        Boolean countNewReturnItems = (Boolean) context.get(x.countNewReturnItems);
        if (countNewReturnItems == null) {
            countNewReturnItems = Boolean.FALSE;
        }
        BigDecimal returnTotal = orh.getOrderReturnedTotal(countNewReturnItems);
        BigDecimal orderTotal = orh.getOrderGrandTotal();
        BigDecimal available = orderTotal.subtract(returnTotal).subtract(adj);


        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.availableReturnTotal, available);
        result.put(x.orderTotal, orderTotal);
        result.put(x.returnTotal, returnTotal);
        return result;
    }

    // worker method which can be used in screen iterations
    public static BigDecimal getReturnItemInitialCost(Delegator delegator, String returnId, String returnItemSeqId) {
        if (delegator == null || returnId == null || returnItemSeqId == null) {
            throw new IllegalArgumentException(x.Method_parameters_cannot_contain_nulls);
        }
        Debug.logInfo(x.Finding_the_initial_item_cost_for_return_item + returnId + x.str_0d0c4ddd + returnItemSeqId, MODULE);

        // the cost holder
        BigDecimal itemCost = BigDecimal.ZERO;

        // get the return item information
        GenericValue returnItem = null;
        try {
            ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
            returnItem = returnItemDao.findOne(delegator, x.ReturnItem,
                    UtilMisc.toMap(x.returnId, returnId, x.returnItemSeqId, returnItemSeqId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GeneralRuntimeException(e.getMessage());
        }
        Debug.logInfo(x.Return_item_value_object + returnItem, MODULE);

        // check for an orderItem association
        if (returnItem != null) {
            String orderId = returnItem.getString(x.orderId);
            String orderItemSeqId = returnItem.getString(x.orderItemSeqId);
            if (orderItemSeqId != null && orderId != null) {
                Debug.logInfo(x.Found_order_item_reference, MODULE);
                // locate the item issuance(s) for this order item
                GenericValue issue = null;
                try {
                    ItemIssuanceDao itemIssuanceDao = DaoRegistry.getDao(delegator, x.ItemIssuance, ItemIssuanceDao.class);
                    issue = itemIssuanceDao.findFirstByCondition(delegator, x.ItemIssuance,
                            EntityCondition.makeCondition(UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId)),
                            null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    throw new GeneralRuntimeException(e.getMessage());
                }
                if (issue != null) {
                    Debug.logInfo(x.Found_item_issuance_reference, MODULE);
                    // just use the first one for now; maybe later we can find a better way to determine which was the
                    // actual item being returned; maybe by serial number
                    GenericValue inventoryItem = null;
                    try {
                        inventoryItem = issue.getRelatedOne(x.InventoryItem, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        throw new GeneralRuntimeException(e.getMessage());
                    }
                    if (inventoryItem != null) {
                        Debug.logInfo(x.Located_inventory_item + inventoryItem.getString(x.inventoryItemId), MODULE);
                        if (inventoryItem.get(x.unitCost) != null) {
                            itemCost = inventoryItem.getBigDecimal(x.unitCost);
                        } else {
                            Debug.logInfo(x.Found_item_cost_but_cost_was_null_Returning_default_amount_0_00, MODULE);
                        }
                    }
                }
            }
        }

        Debug.logInfo(x.Initial_item_cost + itemCost, MODULE);
        return itemCost;
    }

    // helper method for sending return notifications
    private static Map<String, Object> sendReturnNotificationScreen(DispatchContext dctx, OrderReturnServicesContext context, String emailType) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String returnId = (String) context.get(x.returnId);
        Locale locale = (Locale) context.get(x.locale);

        // get the return header
        GenericValue returnHeader = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorUnableToGetReturnHeaderForID, UtilMisc.toMap(x.returnId, returnId), locale));
        }

        // get the return items
        List<GenericValue> returnItems = null;
        List<GenericValue> returnAdjustments;
        try {
            returnItems = returnHeader.getRelated(x.ReturnItem, null, null, false);
            ReturnAdjustmentDao returnAdjustmentDao = DaoRegistry.getDao(delegator, x.ReturnAdjustment, ReturnAdjustmentDao.class);
            returnAdjustments = returnAdjustmentDao.findByAnd(delegator, x.ReturnAdjustment,
                    UtilMisc.toMap(x.returnId, returnId, x.returnItemSeqId, x.NA), UtilMisc.toList(x.returnAdjustmentTypeId), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorUnableToGetReturnItemRecordsFromReturnHeader, locale));
        }

        // get the order header -- the first item will determine which product store to use from the order
        String productStoreId = null;
        String emailAddress = null;
        if (UtilValidate.isNotEmpty(returnItems)) {
            GenericValue firstItem = EntityUtil.getFirst(returnItems);
            GenericValue orderHeader = null;
            try {
                orderHeader = firstItem.getRelatedOne(x.OrderHeader, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderErrorUnableToGetOrderHeaderFromReturnItem, locale));
            }

            if (orderHeader != null && UtilValidate.isNotEmpty(orderHeader.getString(x.productStoreId))) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                productStoreId = orh.getProductStoreId();
                emailAddress = orh.getOrderEmailString();
            }
        }

        // get the email setting and send the mail
        if (UtilValidate.isNotEmpty(productStoreId)) {
            Map<String, Object> sendMap = new HashMap<>();

            GenericValue productStoreEmail = null;
            try {
                ProductStoreEmailSettingDao productStoreEmailSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting,
                        ProductStoreEmailSettingDao.class);
                productStoreEmail = productStoreEmailSettingDao.findOne(delegator, x.ProductStoreEmailSetting,
                        UtilMisc.toMap(x.productStoreId, productStoreId, x.emailType, emailType), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            if (productStoreEmail != null && emailAddress != null) {
                sendMap.put(x.bodyScreenUri, productStoreEmail.getString(x.bodyScreenLocation));
                String xslfoAttachScreenLocation = productStoreEmail.getString(x.xslfoAttachScreenLocation);
                sendMap.put(x.xslfoAttachScreenLocation, xslfoAttachScreenLocation);

                Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap(x.returnHeader, returnHeader, x.returnItems, returnItems,
                        x.returnAdjustments, returnAdjustments, x.locale, locale, x.userLogin, userLogin);
                sendMap.put(x.bodyParameters, bodyParameters);

                sendMap.put(x.subject, productStoreEmail.getString(x.subject));
                sendMap.put(x.contentType, productStoreEmail.get(x.contentType));
                sendMap.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
                sendMap.put(x.sendCc, productStoreEmail.get(x.ccAddress));
                sendMap.put(x.sendBcc, productStoreEmail.get(x.bccAddress));
                sendMap.put(x.sendTo, emailAddress);
                sendMap.put(x.partyId, returnHeader.getString(x.fromPartyId));
                sendMap.put(x.returnId, returnId);

                sendMap.put(x.userLogin, userLogin);

                Map<String, Object> sendResp = null;
                try {
                    sendResp = dispatcher.runSync(x.sendMailFromScreen, sendMap);
                    if (ServiceUtil.isError(sendResp)) {
                        sendResp.put(x.emailType, emailType);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemSendingEmail, locale), null, null, sendResp);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_sending_mail, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemSendingEmail, locale));
                }
                return ServiceUtil.returnSuccess();
            }
        }

        return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_PRODUCT,
                x.ProductProductStoreEmailSettingsNotValid,
                UtilMisc.toMap(x.productStoreId, productStoreId,
                        x.emailType, emailType), locale));
    }

    // return request notification
    public static Map<String, Object> sendReturnAcceptNotification(DispatchContext dctx, OrderReturnServicesContext context) {
        return sendReturnNotificationScreen(dctx, context, x.PRDS_RTN_ACCEPT);
    }

    // return complete notification
    public static Map<String, Object> sendReturnCompleteNotification(DispatchContext dctx, OrderReturnServicesContext context) {
        return sendReturnNotificationScreen(dctx, context, x.PRDS_RTN_COMPLETE);
    }

    // return cancel notification
    public static Map<String, Object> sendReturnCancelNotification(DispatchContext dctx, OrderReturnServicesContext context) {
        return sendReturnNotificationScreen(dctx, context, x.PRDS_RTN_CANCEL);
    }

    // cancel replacement order if return not received within 30 days and send notification
    public static Map<String, Object> autoCancelReplacementOrders(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        List<GenericValue> returnHeaders = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeaders = returnHeaderDao.findByAnd(delegator, x.ReturnHeader,
                    UtilMisc.toMap(x.statusId, x.RETURN_ACCEPTED, x.returnHeaderTypeId, x.CUSTOMER_RETURN), UtilMisc.toList(x.entryDate),
                    false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_getting_Return_headers, MODULE);
        }
        for (GenericValue returnHeader : returnHeaders) {
            String returnId = returnHeader.getString(x.returnId);
            Timestamp entryDate = returnHeader.getTimestamp(x.entryDate);
            String daysTillCancelStr = EntityUtilProperties.getPropertyValue(x.order, x.daysTillCancelReplacementOrder, x._30, delegator);
            int daysTillCancel = 0;
            try {
                daysTillCancel = Integer.parseInt(daysTillCancelStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, x.Unable_to_get_daysTillCancel, MODULE);
            }
            if (daysTillCancel > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(entryDate.getTime());
                cal.add(Calendar.DAY_OF_YEAR, daysTillCancel);
                Date cancelDate = cal.getTime();
                Date nowDate = new Date();
                if (cancelDate.equals(nowDate) || nowDate.after(cancelDate)) {
                    try {
                        ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
                        List<GenericValue> returnItems = returnItemDao.findByAnd(delegator, x.ReturnItem,
                                UtilMisc.toMap(x.returnId, returnId, x.returnTypeId, x.RTN_WAIT_REPLACE_RES),
                                UtilMisc.toList(x.createdStamp), false);
                        for (GenericValue returnItem : returnItems) {
                            GenericValue returnItemResponse = returnItem.getRelatedOne(x.ReturnItemResponse, false);
                            if (returnItemResponse != null) {
                                String replacementOrderId = returnItemResponse.getString(x.replacementOrderId);
                                Map<String, Object> svcCtx = UtilMisc.<String, Object>toMap(x.orderId, replacementOrderId, x.userLogin, userLogin);
                                OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
                                GenericValue orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader,
                                        UtilMisc.toMap(x.orderId, replacementOrderId), false);
                                if (x.ORDER_HOLD.equals(orderHeader.getString(x.statusId))) {
                                    try {
                                        Map<String, Object> result = dispatcher.runSync(x.cancelOrderItem, svcCtx);
                                        if (ServiceUtil.isError(result)) {
                                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                        }
                                    } catch (GenericServiceException e) {
                                        Debug.logError(e, x.Problem_calling_service_cancelOrderItem + svcCtx, MODULE);
                                    }
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    // get the returnable quantiy for an order item
    public static Map<String, Object> getReturnableQuantity(DispatchContext dctx, OrderReturnServicesContext context) {
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        GenericValue product = null;
        Locale locale = (Locale) context.get(x.locale);
        if (orderItem.get(x.productId) != null) {
            try {
                product = orderItem.getRelatedOne(x.Product, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.ERROR_Unable_to_get_Product_from_OrderItem, MODULE);
            }
        }

        // check returnable status
        boolean returnable = true;

        // first check returnable flag
        if (product != null && product.get(x.returnable) != null
                && x.N.equalsIgnoreCase(product.getString(x.returnable))) {
            // the product is not returnable at all
            returnable = false;
        }

        // next check support discontinuation
        if (product != null && product.get(x.supportDiscontinuationDate) != null
                && !UtilDateTime.nowTimestamp().before(product.getTimestamp(x.supportDiscontinuationDate))) {
            // support discontinued either now or in the past
            returnable = false;
        }

        String itemStatus = orderItem.getString(x.statusId);
        BigDecimal orderQty = orderItem.getBigDecimal(x.quantity);
        if (orderItem.getBigDecimal(x.cancelQuantity) != null) {
            orderQty = orderQty.subtract(orderItem.getBigDecimal(x.cancelQuantity));
        }

        // get the returnable quantity
        BigDecimal returnableQuantity = BigDecimal.ZERO;
        if (returnable && (x.ITEM_APPROVED.equals(itemStatus) || x.ITEM_COMPLETED.equals(itemStatus))) {
            List<GenericValue> returnedItems = null;
            try {
                returnedItems = orderItem.getRelated(x.ReturnItem, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderErrorUnableToGetReturnItemInformation, locale));
            }
            if (UtilValidate.isEmpty(returnedItems)) {
                returnableQuantity = orderQty;
            } else {
                BigDecimal returnedQty = BigDecimal.ZERO;
                for (GenericValue returnItem : returnedItems) {
                    GenericValue returnHeader = null;
                    try {
                        returnHeader = returnItem.getRelatedOne(x.ReturnHeader, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToGetReturnHeaderFromItem, locale));
                    }
                    String returnStatus = returnHeader.getString(x.statusId);
                    if (!x.RETURN_CANCELLED.equals(returnStatus)) {
                        if (UtilValidate.isNotEmpty(returnItem.getBigDecimal(x.returnQuantity))) {
                            returnedQty = returnedQty.add(returnItem.getBigDecimal(x.returnQuantity));
                        }
                    }
                }
                if (returnedQty.compareTo(orderQty) < 0) {
                    returnableQuantity = orderQty.subtract(returnedQty);
                }
            }
        }

        // get the returnable price now equals to orderItem.unitPrice, since adjustments are booked separately

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.returnableQuantity, returnableQuantity);
        result.put(x.returnablePrice, orderItem.getBigDecimal(x.unitPrice));
        return result;
    }

    // get a map of returnable items (items not already returned) and quantities
    public static Map<String, Object> getReturnableItems(DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get(x.orderId);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue orderHeader = null;
        try {
            OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
            orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorUnableToGetReturnItemInformation, locale));
        }

        Map<GenericValue, Map<String, Object>> returnable = new LinkedHashMap<>();
        if (orderHeader != null) {
            // OrderItems which have been issued may be returned.
            EntityConditionList<EntityExpr> whereConditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(x.orderId, EntityOperator.EQUALS, orderHeader.getString(x.orderId)),
                    EntityCondition.makeCondition(x.orderItemStatusId, EntityOperator.IN, UtilMisc.toList(x.ITEM_APPROVED, x.ITEM_COMPLETED))),
                    EntityOperator.AND);
            List<GenericValue> orderItemQuantitiesIssued = null;
            try {
                OrderItemShipGrpInvResDao orderItemShipGrpInvResDao = DaoRegistry.getDao(delegator, x.OrderItemQuantityReportGroupByItem,
                        OrderItemShipGrpInvResDao.class);
                orderItemQuantitiesIssued = orderItemShipGrpInvResDao.findByCondition(delegator, x.OrderItemQuantityReportGroupByItem,
                        whereConditions, UtilMisc.toList(x.orderId, x.orderItemSeqId, x.quantityIssued), UtilMisc.toList(x.orderItemSeqId), null,
                        false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderErrorUnableToGetReturnHeaderFromItem, locale));
            }

            if (orderItemQuantitiesIssued != null) {
                for (GenericValue orderItemQuantityIssued : orderItemQuantitiesIssued) {
                    GenericValue item = null;
                    try {
                        item = orderItemQuantityIssued.getRelatedOne(x.OrderItem, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToGetOrderItemInformation, locale));
                    }
                    // items not issued/shipped are considered as returnable only if they are
                    // not physical items
                    if (x.SALES_ORDER.equals(orderHeader.getString(x.orderTypeId))) {
                        BigDecimal quantityIssued = orderItemQuantityIssued.getBigDecimal(x.quantityIssued);
                        if (UtilValidate.isEmpty(quantityIssued) || quantityIssued.compareTo(BigDecimal.ZERO) == 0) {
                            try {
                                GenericValue itemProduct = item.getRelatedOne(x.Product, false);
                                if (ProductWorker.isPhysical(itemProduct)) {
                                    continue;
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, x.Problems_looking_up_returnable_product_type_information, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderErrorUnableToGetTheItemReturnableProduct, locale));
                            }
                        }
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync(x.getReturnableQuantity, UtilMisc.toMap(x.orderItem, item));
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToGetTheItemReturnableQuantity, locale));
                    }
                    // Don't add the OrderItem to the map of returnable OrderItems if there isn't any returnable quantity.
                    if (((BigDecimal) serviceResult.get(x.returnableQuantity)).compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    Map<String, Object> returnInfo = new HashMap<>();
                    // first the return info (quantity/price)
                    returnInfo.put(x.returnableQuantity, serviceResult.get(x.returnableQuantity));
                    returnInfo.put(x.returnablePrice, serviceResult.get(x.returnablePrice));

                    // now the product type information
                    String itemTypeKey = x.FINISHED_GOOD; // default item type (same as invoice)
                    GenericValue product = null;
                    if (item.get(x.productId) != null) {
                        try {
                            product = item.getRelatedOne(x.Product, false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.OrderErrorUnableToGetOrderItemInformation, locale));
                        }
                    }
                    if (product != null) {
                        itemTypeKey = product.getString(x.productTypeId);
                    } else if (item.getString(x.orderItemTypeId) != null) {
                        itemTypeKey = item.getString(x.orderItemTypeId);
                    }
                    returnInfo.put(x.itemTypeKey, itemTypeKey);

                    returnable.put(item, returnInfo);

                    // Order item adjustments
                    List<GenericValue> itemAdjustments = null;
                    try {
                        itemAdjustments = item.getRelated(x.OrderAdjustment, null, null, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToGetOrderAdjustmentsFromItem, locale));
                    }
                    if (UtilValidate.isNotEmpty(itemAdjustments)) {
                        for (GenericValue itemAdjustment : itemAdjustments) {
                            returnInfo = new HashMap<>();
                            returnInfo.put(x.returnableQuantity, BigDecimal.ONE);
                            // TODO: the returnablePrice should be set to the amount minus the already returned amount
                            returnInfo.put(x.returnablePrice, itemAdjustment.get(x.amount));
                            returnInfo.put(x.itemTypeKey, itemTypeKey);
                            returnable.put(itemAdjustment, returnInfo);
                        }
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderErrorNoOrderItemsFound, locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorUnableToFindOrderHeader, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.returnableItems, returnable);
        return result;
    }

    // check return items status and update return header status
    public static Map<String, Object> checkReturnComplete(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String returnId = (String) context.get(x.returnId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated(x.ReturnItem, null, null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_return_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorGettingReturnHeaderItemInformation, locale));
        }

        // if already completed just return
        String currentStatus = null;
        if (returnHeader != null && returnHeader.get(x.statusId) != null) {
            currentStatus = returnHeader.getString(x.statusId);
            if (x.RETURN_COMPLETED.equals(currentStatus) || x.RETURN_CANCELLED.equals(currentStatus)) {
                return ServiceUtil.returnSuccess();
            }
        }

        List<GenericValue> completedItems = new LinkedList<>();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            for (GenericValue item : returnItems) {
                String itemStatus = item != null ? item.getString(x.statusId) : null;
                if (itemStatus != null) {
                    // both completed and cancelled items qualify for completed status change
                    if (x.RETURN_COMPLETED.equals(itemStatus) || x.RETURN_CANCELLED.equals(itemStatus)) {
                        completedItems.add(item);
                    } else {
                        // Non-physical items don't need an inventory receive and so are
                        // considered completed after the return is accepted
                        if (x.RETURN_ACCEPTED.equals(returnHeader.getString(x.statusId))) {
                            try {
                                GenericValue itemProduct = item.getRelatedOne(x.Product, false);
                                if (!ProductWorker.isPhysical(itemProduct)) {
                                    completedItems.add(item);
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, x.Problems_looking_up_returned_product_type_information, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderErrorGettingReturnHeaderItemInformation, locale));
                            }
                        }
                    }
                }
            }

            // if all items are completed/cancelled these should match
            if (completedItems.size() == returnItems.size()) {
                // The return is just moved to its next status by calling the
                // updateReturnHeader service; this will trigger all the appropriate ecas
                // including this service again, so that the return is moved
                // to the final status
                if (currentStatus != null && x.RETURN_ACCEPTED.equals(currentStatus)) {
                    try {
                        serviceResult = dispatcher.runSync(x.updateReturnHeader, UtilMisc.<String, Object>toMap(x.returnId, returnId,
                                x.statusId, x.RETURN_RECEIVED,
                                x.userLogin, userLogin));
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.OrderErrorUnableToCreateReturnStatusHistory, locale));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToCreateReturnStatusHistory, locale));
                    }
                } else if (currentStatus != null && x.RETURN_RECEIVED.equals(currentStatus)) {
                    try {
                        serviceResult = dispatcher.runSync(x.updateReturnHeader, UtilMisc.<String, Object>toMap(x.returnId, returnId,
                                x.statusId, x.RETURN_COMPLETED,
                                x.userLogin, userLogin));
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.OrderErrorUnableToCreateReturnStatusHistory, locale));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderErrorUnableToCreateReturnStatusHistory, locale));
                    }
                }
            }

        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (returnHeader != null) {
            result.put(x.statusId, returnHeader.get(x.statusId));
        }
        return result;
    }

    // credit (billingAccount) return
    public static Map<String, Object> processCreditReturn(DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated(x.ReturnItem, UtilMisc.toMap(x.returnTypeId, x.RTN_CREDIT), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_return_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorGettingReturnHeaderItemInformation, locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap(x.returnId, returnId, x.returnTypeId, x.RTN_CREDIT));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            String finAccountId = returnHeader.getString(x.finAccountId);
            String billingAccountId = returnHeader.getString(x.billingAccountId);
            String fromPartyId = returnHeader.getString(x.fromPartyId);
            String toPartyId = returnHeader.getString(x.toPartyId);

            // make sure total refunds on a return don't exceed amount of returned orders
            Map<String, Object> serviceResult = null;
            try {
                serviceResult = dispatcher.runSync(x.checkPaymentAmountForRefund, UtilMisc.toMap(x.returnId, returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_running_the_checkPaymentAmountForRefund_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderProblemsWithCheckPaymentAmountForRefund, locale));
            }
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }

            // Fetch the ProductStore
            GenericValue productStore = null;
            GenericValue orderHeader = null;
            GenericValue returnItem = null;
            if (UtilValidate.isNotEmpty(returnItems)) {
                returnItem = EntityUtil.getFirst(returnItems);
            }
            if (returnItem != null) {
                try {
                    orderHeader = returnItem.getRelatedOne(x.OrderHeader, false);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            if (orderHeader != null) {
                OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
                productStore = orderReadHelper.getProductStore();
            }

            // if both billingAccountId and finAccountId are supplied, look for productStore.storeCreditAccountEnumId preference
            if (finAccountId != null && billingAccountId != null && productStore != null
                    && productStore.getString(x.storeCreditAccountEnumId) != null) {
                Debug.logWarning(x.You_have_entered_both_financial_account_and_billing_account_for_store_credit_Based_on_the_configuration_on
                        + x.product_store_only_one_of_them_will_be_selected, MODULE);
                if (x.BILLING_ACCOUNT.equals(productStore.getString(x.storeCreditAccountEnumId))) {
                    finAccountId = null;
                    Debug.logWarning(x.Default_setting_on_product_store_is_billing_account_Store_credit_will_goes_to_billing_account
                            + billingAccountId + x.str_4ff447b8, MODULE);
                } else {
                    billingAccountId = null;
                    Debug.logWarning(x.Default_setting_on_product_store_is_financial_account_Store_credit_will_goes_to_financial_account
                            + finAccountId + x.str_4ff447b8, MODULE);
                }
            }

            if (finAccountId == null && billingAccountId == null) {
                // First find a Billing Account with negative balance, and if found store credit to that
                List<GenericValue> billingAccounts;
                try {
                    BillingAccountRoleDao billingAccountRoleDao = DaoRegistry.getDao(delegator, x.BillingAccountRoleAndAddress,
                            BillingAccountRoleDao.class);
                    billingAccounts = billingAccountRoleDao.findByAnd(delegator, x.BillingAccountRoleAndAddress,
                            UtilMisc.toMap(x.partyId, fromPartyId, x.roleTypeId, x.BILL_TO_CUSTOMER), UtilMisc.toList(x.fromDate_f5440273), false);
                    billingAccounts = EntityUtil.filterByDate(billingAccounts);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (UtilValidate.isNotEmpty(billingAccounts)) {
                    ListIterator<GenericValue> billingAccountItr = billingAccounts.listIterator();
                    while (billingAccountItr.hasNext() && billingAccountId == null) {
                        String thisBillingAccountId = billingAccountItr.next().getString(x.billingAccountId);
                        BigDecimal billingAccountBalance = ZERO;
                        try {
                            BillingAccountDao billingAccountDao = DaoRegistry.getDao(delegator, x.BillingAccount, BillingAccountDao.class);
                            GenericValue billingAccount = billingAccountDao.findOne(delegator, x.BillingAccount,
                                    UtilMisc.toMap(x.billingAccountId, thisBillingAccountId), false);
                            billingAccountBalance = OrderReadHelper.getBillingAccountBalance(billingAccount);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (billingAccountBalance.signum() == -1) {
                            billingAccountId = thisBillingAccountId;
                        }
                    }
                }

                // if no billing account with negative balance is found, look for productStore.storeCreditAccountEnumId settings
                if (billingAccountId == null) {
                    if (productStore != null && productStore.getString(x.storeCreditAccountEnumId) != null
                            && x.BILLING_ACCOUNT.equals(productStore.getString(x.storeCreditAccountEnumId))) {
                        if (UtilValidate.isNotEmpty(billingAccounts)) {
                            billingAccountId = EntityUtil.getFirst(billingAccounts).getString(x.billingAccountId);
                        } else {
                            // create new BillingAccount w/ 0 balance
                            Map<String, Object> results = createBillingAccountFromReturn(returnHeader, returnItems, dctx, context);
                            if (ServiceUtil.isError(results)) {
                                Debug.logError(x.Error_creating_BillingAccount + results.get(ModelService.ERROR_MESSAGE), MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderErrorWithCreateBillingAccount, locale) + results.get(ModelService.ERROR_MESSAGE));
                            }
                            billingAccountId = (String) results.get(x.billingAccountId);

                            // double check; make sure we have a billingAccount
                            if (billingAccountId == null) {
                                Debug.logError(x.No_available_billing_account_none_was_created, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderNoAvailableBillingAccount, locale));
                            }
                        }
                    } else {
                        GenericValue finAccount = null;
                        try {
                            FinAccountRoleDao finAccountRoleDao = DaoRegistry.getDao(delegator, x.FinAccountAndRole, FinAccountRoleDao.class);
                            List<GenericValue> finAccounts = finAccountRoleDao.findByAnd(delegator, x.FinAccountAndRole,
                                    UtilMisc.toMap(x.partyId, fromPartyId, x.finAccountTypeId, x.STORE_CREDIT_ACCT, x.roleTypeId, x.OWNER,
                                            x.statusId, x.FNACT_ACTIVE, x.currencyUomId, returnHeader.getString(x.currencyUomId)),
                                    UtilMisc.toList(x.fromDate_f5440273), false);
                            finAccounts = EntityUtil.filterByDate(finAccounts);
                            finAccount = EntityUtil.getFirst(finAccounts);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (finAccount != null) {
                            finAccountId = finAccount.getString(x.finAccountId);
                        }

                        if (finAccountId == null) {
                            Map<String, Object> createAccountCtx = new HashMap<>();
                            createAccountCtx.put(x.ownerPartyId, fromPartyId);
                            createAccountCtx.put(x.finAccountTypeId, x.STORE_CREDIT_ACCT);
                            createAccountCtx.put(x.productStoreId, productStore.getString(x.productStoreId));
                            createAccountCtx.put(x.currencyUomId, returnHeader.getString(x.currencyUomId));
                            createAccountCtx.put(x.finAccountName, x.Store_Credit_Account_for_party + fromPartyId + x.str_4ff447b8);
                            createAccountCtx.put(x.userLogin, userLogin);
                            Map<String, Object> createAccountResult = null;
                            try {
                                createAccountResult = dispatcher.runSync(x.createFinAccountForStore, createAccountCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, x.Problems_running_the_createFinAccountForStore_service, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderProblemsCreatingFinAccountForStore, locale));
                            }
                            if (ServiceUtil.isError(createAccountResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountResult));
                            }
                            finAccountId = (String) createAccountResult.get(x.finAccountId);

                            // double check; make sure we have a FinAccount
                            if (finAccountId == null) {
                                Debug.logError(x.No_available_fin_account_none_was_created, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderNoAvailableFinAccount, locale));
                            }

                            Map<String, Object> finAccountRoleResult = null;
                            try {
                                finAccountRoleResult = dispatcher.runSync(x.createFinAccountRole, UtilMisc.toMap(x.finAccountId,
                                        finAccountId, x.partyId, fromPartyId, x.roleTypeId, x.OWNER, x.userLogin, userLogin));
                            } catch (GenericServiceException e) {
                                Debug.logError(e, x.Problem_running_the_createFinAccountRole_service, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderProblemCreatingFinAccountRoleRecord, locale));
                            }
                            if (ServiceUtil.isError(finAccountRoleResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(finAccountRoleResult));
                            }
                        }
                    }
                }
            }

            // now; to be used for all timestamps
            Timestamp now = UtilDateTime.nowTimestamp();

            // first, compute the total credit from the return items
            BigDecimal creditTotal = ZERO;
            for (GenericValue item : returnItems) {
                BigDecimal quantity = item.getBigDecimal(x.returnQuantity);
                BigDecimal price = item.getBigDecimal(x.returnPrice);
                if (quantity == null) {
                    quantity = ZERO;
                }
                if (price == null) {
                    price = ZERO;
                }
                creditTotal = creditTotal.add(price.multiply(quantity).setScale(DECIMALS, ROUNDING));
            }

            // add the adjustments to the total
            creditTotal = creditTotal.add(adjustments.setScale(DECIMALS, ROUNDING));

            // create finAccountRole and finAccountTrans
            String finAccountTransId = null;
            if (finAccountId != null) {
                Map<String, Object> finAccountTransResult = null;
                try {
                    finAccountTransResult = dispatcher.runSync(x.createFinAccountTrans, UtilMisc.toMap(x.finAccountId, finAccountId,
                            x.finAccountTransTypeId, x.DEPOSIT, x.partyId, toPartyId, x.amount, creditTotal, x.reasonEnumId, x.FATR_REFUND,
                            x.userLogin, userLogin));
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_creating_FinAccountTrans_record, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemCreatingFinAccountTransRecord, locale));
                }
                if (ServiceUtil.isError(finAccountTransResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(finAccountTransResult));
                }
                finAccountTransId = (String) finAccountTransResult.get(x.finAccountTransId);
            }

            // create a Payment record for this credit; will look just like a normal payment
            // However, since this payment is not a DISBURSEMENT or RECEIPT but really a matter of internal record
            // it is of type "Other (Non-posting)"
            String paymentId = delegator.getNextSeqId(x.Payment);
            GenericValue payment = delegator.makeValue(x.Payment, UtilMisc.toMap(x.paymentId, paymentId));
            payment.set(x.paymentTypeId, x.CUSTOMER_REFUND);
            payment.set(x.partyIdFrom, toPartyId);  // if you receive a return FROM someone, then you'd have to give a return TO that person
            payment.set(x.partyIdTo, fromPartyId);
            payment.set(x.effectiveDate, now);
            payment.set(x.amount, creditTotal);
            payment.set(x.comments, x.Return_Credit);
            payment.set(x.statusId, x.PMNT_CONFIRMED);  // set the status to confirmed so nothing else can happen to the payment
            if (billingAccountId != null) {
                payment.set(x.paymentMethodTypeId, x.EXT_BILLACT);
            } else {
                payment.set(x.paymentMethodTypeId, x.FIN_ACCOUNT);
            }
            try {
                delegator.create(payment);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Problem_creating_Payment_record, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderProblemCreatingPaymentRecord, locale));
            }

            // create a return item response
            Map<String, Object> itemResponse = UtilMisc.<String, Object>toMap(x.paymentId, paymentId);
            itemResponse.put(x.responseAmount, creditTotal);
            itemResponse.put(x.responseDate, now);
            itemResponse.put(x.userLogin, userLogin);
            if (billingAccountId != null) {
                itemResponse.put(x.billingAccountId, billingAccountId);
            } else {
                itemResponse.put(x.finAccountTransId, finAccountTransId);
            }
            Map<String, Object> serviceResults = null;
            try {
                serviceResults = dispatcher.runSync(x.createReturnItemResponse, itemResponse);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemCreatingReturnItemResponseRecord, locale), null, null, serviceResults);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_creating_ReturnItemResponse_record, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderProblemCreatingReturnItemResponseRecord, locale));
            }

            // the resulting response ID will be associated with the return items
            String itemResponseId = (String) serviceResults.get(x.returnItemResponseId);

            // loop through the items again to update them and store a status change history
            for (GenericValue item : returnItems) {
                Map<String, Object> returnItemMap = UtilMisc.<String, Object>toMap(x.returnItemResponseId, itemResponseId, x.returnId,
                        item.get(x.returnId), x.returnItemSeqId, item.get(x.returnItemSeqId), x.statusId, x.RETURN_COMPLETED, x.userLogin,
                        userLogin);
                // store the item changes (attached responseId)
                try {
                    serviceResults = dispatcher.runSync(x.updateReturnItem, returnItemMap);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemStoringReturnItemUpdates, locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_storing_ReturnItem_updates, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemStoringReturnItemUpdates, locale));
                }
            }

            if (billingAccountId != null) {
                // create the PaymentApplication for the billing account
                String paId = delegator.getNextSeqId(x.PaymentApplication);
                GenericValue pa = delegator.makeValue(x.PaymentApplication, UtilMisc.toMap(x.paymentApplicationId, paId));
                pa.set(x.paymentId, paymentId);
                pa.set(x.billingAccountId, billingAccountId);
                pa.set(x.amountApplied, creditTotal);
                try {
                    delegator.create(pa);
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Problem_creating_PaymentApplication_record_for_billing_account, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemCreatingPaymentApplicationRecord, locale));
                }

                // create the payment applications for the return invoice in case of billing account
                try {
                    serviceResults = dispatcher.runSync(x.createPaymentApplicationsFromReturnItemResponse,
                            UtilMisc.<String, Object>toMap(x.returnItemResponseId, itemResponseId, x.userLogin, userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemCreatingPaymentApplicationRecord, locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_creating_PaymentApplication_records_for_return_invoice, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderProblemCreatingPaymentApplicationRecord, locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Helper method to generate a BillingAccount (store credit) from a return
     * header.  This method takes care of all business logic relating to
     * the initialization of a Billing Account from the Return data.
     * <p>
     * The BillingAccount.thruDate will be set to (now +
     * ProductStore.storeCreditValidDays + end of day).  The product stores
     * are obtained via the return orders, and the minimum storeCreditValidDays
     * will be used.  The default is to set thruDate to null, which implies no
     * expiration.
     * <p>
     * Note that we set BillingAccount.accountLimit to 0.0 for store credits.
     * This is because the available balance of BillingAccounts is
     * calculated as accountLimit + sum of Payments - sum of Invoices.
     */
    private static Map<String, Object> createBillingAccountFromReturn(GenericValue returnHeader, List<GenericValue> returnItems,
                                                                      DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        try {
            // get the related product stores via the orders related to this return
            List<GenericValue> orders = EntityUtil.getRelated(x.OrderHeader, null, returnItems, false);
            List<GenericValue> productStores = EntityUtil.getRelated(x.ProductStore, null, orders, false);

            // find the minimum storeCreditValidDays of all the ProductStores associated with all the Orders on the Return, skipping null ones
            Long storeCreditValidDays = null;
            for (GenericValue productStore : productStores) {
                Long thisStoreValidDays = productStore.getLong(x.storeCreditValidDays);
                if (thisStoreValidDays == null) {
                    continue;
                }

                if (storeCreditValidDays == null) {
                    storeCreditValidDays = thisStoreValidDays;
                } else if (thisStoreValidDays.compareTo(storeCreditValidDays) < 0) {
                    // if this store's days < store credit valid days, use this store's days
                    storeCreditValidDays = thisStoreValidDays;
                }
            }

            // if there is a storeCreditValidDays, set the thruDate to (nowTimestamp + storeCreditValidDays + end of day)
            Timestamp thruDate = null;
            if (storeCreditValidDays != null) {
                thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), storeCreditValidDays);
            }

            // create the billing account
            Map<String, Object> input = UtilMisc.<String, Object>toMap(x.accountLimit, BigDecimal.ZERO, x.description, x.Credit_Account_for_Return
                    + returnHeader.get(x.returnId), x.userLogin, userLogin);
            input.put(x.accountCurrencyUomId, returnHeader.get(x.currencyUomId));
            input.put(x.thruDate, thruDate);
            Map<String, Object> results = dispatcher.runSync(x.createBillingAccount, input);
            if (ServiceUtil.isError(results)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
            }
            String billingAccountId = (String) results.get(x.billingAccountId);

            // set the role on the account
            input = UtilMisc.toMap(x.billingAccountId, billingAccountId, x.partyId, returnHeader.get(x.fromPartyId), x.roleTypeId,
                    x.BILL_TO_CUSTOMER, x.userLogin, userLogin);
            Map<String, Object> roleResults = dispatcher.runSync(x.createBillingAccountRole, input);
            if (ServiceUtil.isError(roleResults)) {
                Debug.logError(x.Error_with_createBillingAccountRole + roleResults.get(ModelService.ERROR_MESSAGE), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderErrorWithCreateBillingAccountRole, locale) + roleResults.get(ModelService.ERROR_MESSAGE));
            }

            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_error_when_creating_BillingAccount + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderProblemsCreatingBillingAccount, locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Service_error_when_creating_BillingAccount + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderProblemsCreatingBillingAccount, locale));
        }
    }

    public static Map<String, Object> processRefundReturnForReplacement(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderId = (String) context.get(x.orderId);
        Map<String, Object> serviceResult = new HashMap<>();

        GenericValue orderHeader = null;
        List<GenericValue> orderPayPrefs;
        try {
            OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
            orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
            orderPayPrefs = orderHeader.getRelated(x.OrderPaymentPreference, null, UtilMisc.toList(x.maxAmount_7b034ae5), false);
        } catch (GenericEntityException e) {
            Debug.logError(x.Problem_looking_up_order_information_for_orderId + orderId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderCannotGetOrderHeader, locale));
        }

        // Check for replacement order
        if (UtilValidate.isEmpty(orderPayPrefs)) {
            List<GenericValue> returnItemResponses;
            try {
                returnItemResponses = orderHeader.getRelated(x.ReplacementReturnItemResponse, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(x.Problem_getting_ReturnItemResponses, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            for (GenericValue returnItemResponse : returnItemResponses) {
                GenericValue returnItem = null;
                GenericValue returnHeader = null;
                try {
                    returnItem = EntityUtil.getFirst(returnItemResponse.getRelated(x.ReturnItem, null, null, false));
                    returnHeader = returnItem.getRelatedOne(x.ReturnHeader, false);
                } catch (GenericEntityException e) {
                    Debug.logError(x.Problem_getting_ReturnItem, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (x.RETURN_RECEIVED.equals(returnHeader.getString(x.statusId))) {
                    String returnId = returnItem.getString(x.returnId);
                    String returnTypeId = returnItem.getString(x.returnTypeId);
                    try {
                        serviceResult = dispatcher.runSync(x.processRefundReturn, UtilMisc.toMap(x.returnId, returnId, x.returnTypeId,
                                returnTypeId, x.userLogin, userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Problem_running_the_processRefundReturn_service, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemsWithTheRefundSeeLogs, locale));
                    }
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        }
        return serviceResult;
    }

    // refund (cash/charge) return
    public static Map<String, Object> processRefundReturn(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String returnId = (String) context.get(x.returnId);
        String returnTypeId = (String) context.get(x.returnTypeId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated(x.ReturnItem, UtilMisc.toMap(x.returnTypeId, returnTypeId), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_return_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorGettingReturnHeaderItemInformation, locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap(x.returnId, returnId, x.returnTypeId, returnTypeId));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            Map<String, List<GenericValue>> itemsByOrder = new HashMap<>();
            Map<String, BigDecimal> totalByOrder = new HashMap<>();

            // make sure total refunds on a return don't exceed amount of returned orders
            Map<String, Object> serviceResult = null;
            try {
                serviceResult = dispatcher.runSync(x.checkPaymentAmountForRefund, UtilMisc.toMap(x.returnId, returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_running_the_checkPaymentAmountForRefund_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.OrderProblemsWithCheckPaymentAmountForRefund, locale));
            }
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }

            groupReturnItemsByOrder(returnItems, itemsByOrder, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            for (Map.Entry<String, List<GenericValue>> entry : itemsByOrder.entrySet()) {
                String orderId = entry.getKey();
                List<GenericValue> items = entry.getValue();
                BigDecimal orderTotal = totalByOrder.get(orderId);

                // get order header & payment prefs
                GenericValue orderHeader = null;
                List<GenericValue> orderPayPrefs = null;
                try {
                    OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
                    orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
                    // sort these desending by maxAmount
                    orderPayPrefs = orderHeader.getRelated(x.OrderPaymentPreference, null, UtilMisc.toList(x.maxAmount_7b034ae5), false);

                    List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.PAYMENT_SETTLED),
                            EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.PAYMENT_RECEIVED));
                    orderPayPrefs = EntityUtil.filterByOr(orderPayPrefs, exprs);

                    // Check for replacement order
                    if (UtilValidate.isEmpty(orderPayPrefs)) {
                        OrderItemAssocDao orderItemAssocDao = DaoRegistry.getDao(delegator, x.OrderItemAssoc, OrderItemAssocDao.class);
                        GenericValue orderItemAssoc = orderItemAssocDao.findFirstByCondition(delegator, x.OrderItemAssoc,
                                EntityCondition.makeCondition(UtilMisc.toMap(x.toOrderId, orderId, x.orderItemAssocTypeId, x.REPLACEMENT)),
                                null, null, false);
                        if (orderItemAssoc != null) {
                            String originalOrderId = orderItemAssoc.getString(x.orderId);
                            orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, originalOrderId), false);
                            orderPayPrefs = orderHeader.getRelated(x.OrderPaymentPreference, null, UtilMisc.toList(x.maxAmount_7b034ae5), false);
                            orderPayPrefs = EntityUtil.filterByOr(orderPayPrefs, exprs);
                            orderId = originalOrderId;
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Cannot_get_Order_details_for + orderId, MODULE);
                    continue;
                }
                OrderReadHelper orderReadHelper = new OrderReadHelper(delegator, orderId);

                // Determine the fall-through refund paymentMethodId from the PartyAcctgPreference of the owner of the productStore for the order
                GenericValue productStore = orderReadHelper.getProductStore();
                if (UtilValidate.isEmpty(productStore) || UtilValidate.isEmpty(productStore.get(x.payToPartyId))) {
                    Debug.logError(x.No_payToPartyId_found_for_orderId + orderId, MODULE);
                } else {
                    GenericValue orgAcctgPref = null;
                    Map<String, Object> acctgPreferencesResult = null;
                    try {
                        acctgPreferencesResult = dispatcher.runSync(x.getPartyAccountingPreferences, UtilMisc.toMap(x.organizationPartyId,
                                productStore.get(x.payToPartyId), x.userLogin, userLogin));
                        if (ServiceUtil.isError(acctgPreferencesResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(acctgPreferencesResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Error_retrieving_PartyAcctgPreference_for_partyId + productStore.get(x.payToPartyId), MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemsWithGetPartyAcctgPreferences, locale));
                    }
                    orgAcctgPref = (GenericValue) acctgPreferencesResult.get(x.partyAccountingPreference);

                    if (orgAcctgPref != null) {
                        try {
                            orgAcctgPref.getRelatedOne(x.PaymentMethod, false);
                        } catch (GenericEntityException e) {
                            Debug.logError(x.Error_retrieving_related_refundPaymentMethod_from_PartyAcctgPreference_for_partyId
                                    + productStore.get(x.payToPartyId), MODULE);
                        }
                    }
                }

                // now; for all timestamps
                Timestamp now = UtilDateTime.nowTimestamp();

                // Assemble a map of orderPaymentPreferenceId -> list of maps of (OPP and availableAmountForRefunding)
                //     where availableAmountForRefunding = receivedAmount - alreadyRefundedAmount
                // We break the OPPs down this way because we need to process the refunds to payment methods in a particular order
                Map<String, BigDecimal> receivedPaymentTotalsByPaymentMethod = orderReadHelper.getReceivedPaymentTotalsByPaymentMethod();
                Map<String, BigDecimal> refundedTotalsByPaymentMethod = orderReadHelper.getReturnedTotalsByPaymentMethod();

                // getOrderPaymentPreferenceTotalByType has been called because getReceivedPaymentTotalsByPaymentMethod does not
                // return payments captured from Billing Account.This is because when payment is captured from Billing Account
                // then no entry is maintained in Payment entity.
                BigDecimal receivedPaymentTotalsByBillingAccount = orderReadHelper.getOrderPaymentPreferenceTotalByType(x.EXT_BILLACT);

                /*
                 * Go through the OrderPaymentPreferences and determine how much remains to be refunded for each.
                 * Then group these refund amounts and orderPaymentPreferences by paymentMethodTypeId.  That is,
                 * the intent is to get the refundable amounts per orderPaymentPreference, grouped by payment method type.
                 */
                Map<String, List<Map<String, Object>>> prefSplitMap = new HashMap<>();
                for (GenericValue orderPayPref : orderPayPrefs) {
                    String paymentMethodTypeId = orderPayPref.getString(x.paymentMethodTypeId);
                    String orderPayPrefKey = orderPayPref.getString(x.paymentMethodId) != null ? orderPayPref.getString(x.paymentMethodId)
                            : orderPayPref.getString(x.paymentMethodTypeId);

                    // See how much we can refund to the payment method
                    BigDecimal orderPayPrefReceivedTotal = ZERO;
                    if (receivedPaymentTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add(receivedPaymentTotalsByPaymentMethod.get(orderPayPrefKey))
                                .setScale(DECIMALS, ROUNDING);
                    }

                    if (receivedPaymentTotalsByBillingAccount != null) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add(receivedPaymentTotalsByBillingAccount);
                    }
                    BigDecimal orderPayPrefRefundedTotal = ZERO;
                    if (refundedTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefRefundedTotal = orderPayPrefRefundedTotal.add(refundedTotalsByPaymentMethod.get(orderPayPrefKey))
                                .setScale(DECIMALS, ROUNDING);
                    }
                    BigDecimal orderPayPrefAvailableTotal = orderPayPrefReceivedTotal.subtract(orderPayPrefRefundedTotal);

                    // add the refundable amount and orderPaymentPreference to the paymentMethodTypeId map
                    if (orderPayPrefAvailableTotal.compareTo(ZERO) > 0) {
                        Map<String, Object> orderPayPrefDetails = new HashMap<>();
                        orderPayPrefDetails.put(x.orderPaymentPreference, orderPayPref);
                        orderPayPrefDetails.put(x.availableTotal, orderPayPrefAvailableTotal);
                        if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                            List<Map<String, Object>> paymentMethodTypeIds = prefSplitMap.get(paymentMethodTypeId);
                            paymentMethodTypeIds.add(orderPayPrefDetails);
                        } else {
                            prefSplitMap.put(paymentMethodTypeId, UtilMisc.toList(orderPayPrefDetails));
                        }
                    }
                }

                // Keep a decreasing total of the amount remaining to refund
                BigDecimal amountLeftToRefund = orderTotal.setScale(DECIMALS, ROUNDING);

                // This can be extended to support additional electronic types
                List<String> electronicTypes = UtilMisc.<String>toList(x.CREDIT_CARD, x.EFT_ACCOUNT, x.FIN_ACCOUNT, x.GIFT_CARD);

                // Figure out if EXT_PAYPAL should be considered as an electronic type
                if (productStore != null) {
                    ExpressCheckoutEvents.CheckoutType payPalType = ExpressCheckoutEvents.determineCheckoutType(delegator,
                            productStore.getString(x.productStoreId));
                    if (!payPalType.equals(ExpressCheckoutEvents.CheckoutType.NONE)) {
                        electronicTypes.add(x.EXT_PAYPAL);
                    }
                }
                // This defines the ordered part of the sequence of refund processing
                List<String> orderedRefundPaymentMethodTypes = new LinkedList<>();
                orderedRefundPaymentMethodTypes.add(x.EXT_BILLACT);
                orderedRefundPaymentMethodTypes.add(x.FIN_ACCOUNT);
                orderedRefundPaymentMethodTypes.add(x.GIFT_CARD);
                orderedRefundPaymentMethodTypes.add(x.CREDIT_CARD);
                orderedRefundPaymentMethodTypes.add(x.EFT_ACCOUNT);

                // Add all the other paymentMethodTypes, in no particular order
                List<GenericValue> otherPaymentMethodTypes;
                try {
                    PaymentMethodTypeDao paymentMethodTypeDao = DaoRegistry.getDao(delegator, x.PaymentMethodType, PaymentMethodTypeDao.class);
                    otherPaymentMethodTypes = paymentMethodTypeDao.findByCondition(delegator, x.PaymentMethodType,
                            EntityCondition.makeCondition(x.paymentMethodTypeId, EntityOperator.NOT_IN, orderedRefundPaymentMethodTypes), null,
                            null, null, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Cannot_get_PaymentMethodTypes, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.OrderOrderPaymentPreferencesCannotGetPaymentMethodTypes,
                            UtilMisc.toMap(x.errorString, e.toString()), locale));
                }
                List<String> fieldList = EntityUtil.getFieldListFromEntityList(otherPaymentMethodTypes, x.paymentMethodTypeId, true);
                orderedRefundPaymentMethodTypes.addAll(fieldList);

                // Iterate through the specified sequence of paymentMethodTypes, refunding to the correct OrderPaymentPreferences
                //    as long as there's a positive amount remaining to refund
                Iterator<String> orpmtit = orderedRefundPaymentMethodTypes.iterator();
                while (orpmtit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                    String paymentMethodTypeId = orpmtit.next();
                    if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                        List<Map<String, Object>> paymentMethodDetails = prefSplitMap.get(paymentMethodTypeId);

                        // Iterate through the OrderPaymentPreferences of this type
                        Iterator<Map<String, Object>> pmtppit = paymentMethodDetails.iterator();
                        while (pmtppit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                            Map<String, Object> orderPaymentPrefDetails = pmtppit.next();
                            GenericValue orderPaymentPreference = (GenericValue) orderPaymentPrefDetails.get(x.orderPaymentPreference);
                            BigDecimal orderPaymentPreferenceAvailable = (BigDecimal) orderPaymentPrefDetails.get(x.availableTotal);
                            GenericValue refundOrderPaymentPreference = null;

                            // Refund up to the maxAmount for the paymentPref, or whatever is left to refund if that's less than the maxAmount
                            BigDecimal amountToRefund = orderPaymentPreferenceAvailable.min(amountLeftToRefund);
                            // The amount actually refunded for the paymentPref, default to requested amount
                            BigDecimal amountRefunded = amountToRefund;

                            String paymentId = null;
                            String returnItemStatusId = x.RETURN_COMPLETED;  // generally, the return item will be considered complete after this
                            // Call the refund service to refund the payment
                            if (electronicTypes.contains(paymentMethodTypeId)) {
                                try {
                                    Map<String, Object> serviceContext = UtilMisc.toMap(x.orderId, orderId, x.userLogin, context.get(x.userLogin));
                                    serviceContext.put(x.paymentMethodId, orderPaymentPreference.getString(x.paymentMethodId));
                                    serviceContext.put(x.paymentMethodTypeId, orderPaymentPreference.getString(x.paymentMethodTypeId));
                                    serviceContext.put(x.statusId, orderPaymentPreference.getString(x.statusId));
                                    serviceContext.put(x.maxAmount, amountToRefund.setScale(DECIMALS, ROUNDING));
                                    String orderPaymentPreferenceNewId = null;
                                    Map<String, Object> result = dispatcher.runSync(x.createOrderPaymentPreference, serviceContext);
                                    if (ServiceUtil.isError(result)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                    }
                                    orderPaymentPreferenceNewId = (String) result.get(x.orderPaymentPreferenceId);
                                    try {
                                        OrderPaymentPreferenceDao orderPaymentPreferenceDao = DaoRegistry.getDao(delegator, x.OrderPaymentPreference,
                                                OrderPaymentPreferenceDao.class);
                                        refundOrderPaymentPreference = orderPaymentPreferenceDao.findOne(delegator, x.OrderPaymentPreference,
                                                UtilMisc.toMap(x.orderPaymentPreferenceId, orderPaymentPreferenceNewId), false);
                                    } catch (GenericEntityException e) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemsWithTheRefundSeeLogs,
                                                locale));
                                    }
                                    serviceResult = dispatcher.runSync(x.refundPayment, UtilMisc.<String, Object>toMap(x.orderPaymentPreference,
                                            refundOrderPaymentPreference, x.refundAmount, amountToRefund.setScale(DECIMALS, ROUNDING), x.userLogin,
                                            userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError(x.Error_in_refund_payment + ServiceUtil.getErrorMessage(serviceResult), MODULE);
                                        continue;
                                    }
                                    // for electronic types such as CREDIT_CARD and EFT_ACCOUNT, use refundPayment service
                                    paymentId = (String) serviceResult.get(x.paymentId);
                                    amountRefunded = (BigDecimal) serviceResult.get(x.refundAmount);
                                } catch (GenericServiceException e) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemsWithTheRefundSeeLogs, locale));
                                }
                            } else if (x.EXT_BILLACT.equals(paymentMethodTypeId)) {
                                try {
                                    // for Billing Account refunds
                                    serviceResult = dispatcher.runSync(x.refundBillingAccountPayment,
                                            UtilMisc.<String, Object>toMap(x.orderPaymentPreference, orderPaymentPreference, x.refundAmount,
                                                    amountToRefund.setScale(DECIMALS, ROUNDING), x.userLogin, userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError(x.Error_in_refund_payment + ServiceUtil.getErrorMessage(serviceResult), MODULE);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get(x.paymentId);
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, x.Problem_running_the_refundPayment_service, MODULE);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                            x.OrderProblemsWithTheRefundSeeLogs, locale));
                                }
                            } else {
                                // handle manual refunds
                                try {
                                    Map<String, Object> input = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.amount, amountLeftToRefund,
                                            x.statusId, x.PMNT_NOT_PAID);
                                    input.put(x.partyIdTo, returnHeader.get(x.fromPartyId));
                                    input.put(x.partyIdFrom, returnHeader.get(x.toPartyId));
                                    input.put(x.paymentTypeId, x.CUSTOMER_REFUND);
                                    input.put(x.paymentMethodId, orderPaymentPreference.get(x.paymentMethodId));
                                    input.put(x.paymentMethodTypeId, orderPaymentPreference.get(x.paymentMethodTypeId));
                                    input.put(x.paymentPreferenceId, orderPaymentPreference.get(x.orderPaymentPreferenceId));

                                    serviceResult = dispatcher.runSync(x.createPayment, input);

                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError(x.Error_in_refund_payment + ServiceUtil.getErrorMessage(serviceResult), MODULE);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get(x.paymentId);
                                    returnItemStatusId = x.RETURN_MAN_REFUND;    // however, in this case we should flag it as a manual refund
                                } catch (GenericServiceException e) {
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                            }

                            // Fill out the data for the new ReturnItemResponse
                            Map<String, Object> response = new HashMap<>();
                            if (refundOrderPaymentPreference != null) {
                                response.put(x.orderPaymentPreferenceId, refundOrderPaymentPreference.getString(x.orderPaymentPreferenceId));
                            } else {
                                response.put(x.orderPaymentPreferenceId, orderPaymentPreference.getString(x.orderPaymentPreferenceId));
                            }
                            response.put(x.responseAmount, amountRefunded.setScale(DECIMALS, ROUNDING));
                            response.put(x.responseDate, now);
                            response.put(x.userLogin, userLogin);
                            response.put(x.paymentId, paymentId);
                            if (x.EXT_BILLACT.equals(paymentMethodTypeId)) {
                                response.put(x.billingAccountId, orderReadHelper.getBillingAccount().getString(x.billingAccountId));
                            }
                            Map<String, Object> serviceResults = null;
                            try {
                                serviceResults = dispatcher.runSync(x.createReturnItemResponse, response);
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                            x.OrderProblemsCreatingReturnItemResponseEntity, locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, x.Problems_creating_new_ReturnItemResponse_entity, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderProblemsCreatingReturnItemResponseEntity, locale));
                            }
                            String responseId = (String) serviceResults.get(x.returnItemResponseId);

                            // Set the response on each item
                            for (GenericValue item : items) {
                                Map<String, Object> returnItemMap = UtilMisc.<String, Object>toMap(x.returnItemResponseId, responseId, x.returnId,
                                        item.get(x.returnId),
                                        x.returnItemSeqId, item.get(x.returnItemSeqId), x.statusId, returnItemStatusId, x.userLogin, userLogin);
                                try {
                                    serviceResults = dispatcher.runSync(x.updateReturnItem, returnItemMap);
                                    if (ServiceUtil.isError(serviceResults)) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                                x.OrderProblemUpdatingReturnItemReturnItemResponseId, locale), null, null, serviceResults);
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError(x.Problem_updating_the_ReturnItem_entity, MODULE);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                            x.OrderProblemUpdatingReturnItemReturnItemResponseId, locale));
                                }

                            }

                            // Create the payment applications for the return invoice
                            try {
                                serviceResults = dispatcher.runSync(x.createPaymentApplicationsFromReturnItemResponse,
                                        UtilMisc.<String, Object>toMap(x.returnItemResponseId, responseId, x.userLogin, userLogin));
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                            x.OrderProblemUpdatingReturnItemReturnItemResponseId, locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, x.Problem_creating_PaymentApplication_records_for_return_invoice, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderProblemUpdatingReturnItemReturnItemResponseId, locale));
                            }

                            // Update the amount necessary to refund
                            amountLeftToRefund = amountLeftToRefund.subtract(amountRefunded);
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> refundBillingAccountPayment(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        GenericValue paymentPref = (GenericValue) context.get(x.orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get(x.refundAmount);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Cannot_get_OrderHeader_from_OrderPaymentPreference, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderOrderPaymentCannotBeCreatedWithRelatedOrderHeader, locale) + e.toString());
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        String payFromPartyId = orh.getBillFromParty().getString(x.partyId);
        String payToPartyId = orh.getBillToParty().getString(x.partyId);

        // Create the PaymentGatewayResponse record
        String responseId = delegator.getNextSeqId(x.PaymentGatewayResponse);
        GenericValue response = delegator.makeValue(x.PaymentGatewayResponse);
        response.set(x.paymentGatewayResponseId, responseId);
        response.set(x.paymentServiceTypeEnumId, x.PRDS_PAY_REFUND);
        response.set(x.orderPaymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        response.set(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        response.set(x.transCodeEnumId, x.PGT_REFUND);
        response.set(x.amount, refundAmount);
        response.set(x.transactionDate, UtilDateTime.nowTimestamp());
        response.set(x.currencyUomId, orh.getCurrency());
        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderOrderPaymentGatewayResponseCannotBeCreated, locale));
        }

        // Create the Payment record (parties reversed)
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap(x.paymentTypeId, x.CUSTOMER_REFUND);
        paymentCtx.put(x.paymentMethodTypeId, paymentPref.get(x.paymentMethodTypeId));
        paymentCtx.put(x.paymentGatewayResponseId, responseId);
        paymentCtx.put(x.partyIdTo, payToPartyId);
        paymentCtx.put(x.partyIdFrom, payFromPartyId);
        paymentCtx.put(x.statusId, x.PMNT_CONFIRMED);
        paymentCtx.put(x.paymentPreferenceId, paymentPref.get(x.orderPaymentPreferenceId));
        paymentCtx.put(x.currencyUomId, orh.getCurrency());
        paymentCtx.put(x.amount, refundAmount);
        paymentCtx.put(x.userLogin, userLogin);
        paymentCtx.put(x.comments, x.Refund);

        String paymentId = null;
        try {
            Map<String, Object> paymentCreationResult = dispatcher.runSync(x.createPayment, paymentCtx);
            if (ServiceUtil.isError(paymentCreationResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(paymentCreationResult));
            }
            paymentId = (String) paymentCreationResult.get(x.paymentId);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderOrderPaymentFailed,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (paymentId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderOrderPaymentFailed, UtilMisc.toMap(x.errorString, x.emptyString), locale));
        }

        // if the original order was paid with a billing account, then go find the billing account from the order and associate this
        // refund with that billing account
        // thus returning value to the billing account
        if (x.EXT_BILLACT.equals(paymentPref.getString(x.paymentMethodTypeId))) {
            GenericValue billingAccount = orh.getBillingAccount();
            if (UtilValidate.isNotEmpty(billingAccount.getString(x.billingAccountId))) {
                try {
                    Map<String, Object> paymentApplResult = dispatcher.runSync(x.createPaymentApplication,
                            UtilMisc.<String, Object>toMap(x.paymentId, paymentId, x.billingAccountId, billingAccount.getString(x.billingAccountId),
                            x.amountApplied, refundAmount, x.userLogin, userLogin));
                    if (ServiceUtil.isError(paymentApplResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(paymentApplResult));
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.OrderOrderPaymentApplicationFailed,
                            UtilMisc.toMap(x.errorString, e.getMessage()), locale));
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.paymentId, paymentId);
        return result;
    }

    public static Map<String, Object> createPaymentApplicationsFromReturnItemResponse(DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        // the strategy for this service is to get a list of return invoices via the return items -> return item billing relationships
        // then split up the responseAmount among the invoices evenly
        String responseId = (String) context.get(x.returnItemResponseId);
        String errorMsg = x.Failed_to_create_payment_applications_for_return_item_response + responseId + x.str_aa6bf6f2;
        try {
            ReturnItemResponseDao returnItemResponseDao = DaoRegistry.getDao(delegator, x.ReturnItemResponse, ReturnItemResponseDao.class);
            GenericValue response = returnItemResponseDao.findOne(delegator, x.ReturnItemResponse,
                    UtilMisc.toMap(x.returnItemResponseId, responseId), false);
            if (response == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderReturnItemResponseNotFound,
                        UtilMisc.toMap(x.errorMsg, errorMsg, x.responseId, responseId), locale));
            }
            BigDecimal responseAmount = response.getBigDecimal(x.responseAmount).setScale(DECIMALS, ROUNDING);
            String paymentId = response.getString(x.paymentId);

            // for each return item in the response, get the list of return item billings and then a list of invoices
            Map<String, GenericValue> returnInvoices = new HashMap<>(); // key is invoiceId, value is Invoice GenericValue
            List<GenericValue> items = response.getRelated(x.ReturnItem, null, null, false);
            for (GenericValue item : items) {
                List<GenericValue> billings = item.getRelated(x.ReturnItemBilling, null, null, false);
                for (GenericValue billing : billings) {
                    GenericValue invoice = billing.getRelatedOne(x.Invoice, false);

                    // put the invoice in the map if it doesn't already exist (a very loopy way of doing group by invoiceId without creating a view)
                    if (returnInvoices.get(invoice.getString(x.invoiceId)) == null) {
                        returnInvoices.put(invoice.getString(x.invoiceId), invoice);
                    }
                }
            }

            // for each return invoice found, sum up the related billings
            Map<String, BigDecimal> invoiceTotals = new HashMap<>(); // key is invoiceId, value is the sum of all billings for that invoice
            BigDecimal grandTotal = ZERO; // The sum of all return invoice totals
            for (GenericValue invoice : returnInvoices.values()) {
                List<GenericValue> billings = invoice.getRelated(x.ReturnItemBilling, null, null, false);
                BigDecimal runningTotal = ZERO;
                for (GenericValue billing : billings) {
                    runningTotal = runningTotal.add(billing.getBigDecimal(x.amount).multiply(billing.getBigDecimal(x.quantity))
                            .setScale(DECIMALS, ROUNDING));
                }

                invoiceTotals.put(invoice.getString(x.invoiceId), runningTotal);
                grandTotal = grandTotal.add(runningTotal);
            }

            // now allocate responseAmount * invoiceTotal / grandTotal to each invoice
            for (GenericValue invoice : returnInvoices.values()) {
                String invoiceId = invoice.getString(x.invoiceId);
                BigDecimal invoiceTotal = invoiceTotals.get(invoiceId);

                BigDecimal amountApplied = responseAmount.multiply(invoiceTotal).divide(grandTotal, DECIMALS, ROUNDING).setScale(DECIMALS, ROUNDING);

                if (paymentId != null) {
                    // create a payment application for the invoice
                    Map<String, Object> input = UtilMisc.<String, Object>toMap(x.paymentId, paymentId, x.invoiceId, invoice.getString(x.invoiceId));
                    input.put(x.amountApplied, amountApplied);
                    input.put(x.userLogin, userLogin);
                    if (response.get(x.billingAccountId) != null) {
                        GenericValue billingAccount = response.getRelatedOne(x.BillingAccount, false);
                        if (billingAccount != null) {
                            input.put(x.billingAccountId, response.get(x.billingAccountId));
                        }
                    }
                    Map<String, Object> serviceResults = dispatcher.runSync(x.createPaymentApplication, input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                    if (Debug.verboseOn()) {
                        Debug.logInfo(x.Created_PaymentApplication_for_response_with_amountApplied + amountApplied.toString(), MODULE);
                    }
                }
            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), MODULE);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    // replacement return (create new order adjusted to be at no charge)
    public static Map<String, Object> processReplacementReturn(DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        String returnTypeId = (String) context.get(x.returnTypeId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated(x.ReturnItem, UtilMisc.toMap(x.returnTypeId, returnTypeId), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_return_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorGettingReturnHeaderItemInformation, locale));
        }
        List<String> createdOrderIds = new LinkedList<>();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            String returnHeaderTypeId = returnHeader.getString(x.returnHeaderTypeId);
            Map<String, List<GenericValue>> returnItemsByOrderId = new HashMap<>();
            Map<String, BigDecimal> totalByOrder = new HashMap<>();
            groupReturnItemsByOrder(returnItems, returnItemsByOrderId, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            for (Map.Entry<String, List<GenericValue>> entry : returnItemsByOrderId.entrySet()) {
                String orderId = entry.getKey();
                List<GenericValue> returnItemList = entry.getValue();

                // get order header & payment prefs
                GenericValue orderHeader = null;
                try {
                    OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
                    orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Cannot_get_Order_details_for + orderId, MODULE);
                    continue;
                }

                OrderReadHelper orh = new OrderReadHelper(orderHeader);

                // create the replacement order
                Map<String, Object> orderMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin);

                String placingPartyId = null;
                GenericValue placingParty = null;
                if (x.CUSTOMER_RETURN.equals(returnHeaderTypeId)) {
                    placingParty = orh.getPlacingParty();
                    if (placingParty != null) {
                        placingPartyId = placingParty.getString(x.partyId);
                    }
                    orderMap.put(x.orderTypeId, x.SALES_ORDER);
                } else {
                    placingParty = orh.getSupplierAgent();
                    if (placingParty != null) {
                        placingPartyId = placingParty.getString(x.partyId);
                    }
                    orderMap.put(x.orderTypeId, x.PURCHASE_ORDER);
                }
                orderMap.put(x.partyId, placingPartyId);
                orderMap.put(x.productStoreId, orderHeader.get(x.productStoreId));
                orderMap.put(x.webSiteId, orderHeader.get(x.webSiteId));
                orderMap.put(x.visitId, orderHeader.get(x.visitId));
                orderMap.put(x.currencyUom, orderHeader.get(x.currencyUom));
                orderMap.put(x.grandTotal, BigDecimal.ZERO);

                // make the contact mechs
                List<GenericValue> contactMechs = new LinkedList<>();
                List<GenericValue> orderCm = null;
                try {
                    orderCm = orderHeader.getRelated(x.OrderContactMech, null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (orderCm != null) {
                    for (GenericValue v : orderCm) {
                        contactMechs.add(GenericValue.create(v));
                    }
                    orderMap.put(x.orderContactMechs, contactMechs);
                }

                // make the order items
                BigDecimal orderPriceTotal = BigDecimal.ZERO;
                BigDecimal additionalItemTotal = BigDecimal.ZERO;
                List<GenericValue> orderItems = new LinkedList<>();
                List<GenericValue> orderItemShipGroupInfo = new LinkedList<>();
                List<String> orderItemShipGroupIds = new LinkedList<>(); // this is used to store the ship group ids of the groups already added
                // to the orderItemShipGroupInfo list
                List<GenericValue> orderItemAssocs = new LinkedList<>();
                if (returnItemList != null) {
                    int itemCount = 1;
                    for (GenericValue returnItem : returnItemList) {
                        GenericValue orderItem = null;
                        GenericValue product = null;
                        try {
                            orderItem = returnItem.getRelatedOne(x.OrderItem, false);
                            product = orderItem.getRelatedOne(x.Product, false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            continue;
                        }
                        BigDecimal quantity = returnItem.getBigDecimal(x.returnQuantity);
                        BigDecimal unitPrice = returnItem.getBigDecimal(x.returnPrice);
                        if (quantity != null && unitPrice != null) {
                            orderPriceTotal = orderPriceTotal.add(quantity.multiply(unitPrice));
                            // Check if the product being returned has a Refurbished Equivalent and if so
                            // (and there is inventory for the assoc product) use that product instead
                            GenericValue refurbItem = null;
                            if (x.CUSTOMER_RETURN.equals(returnHeaderTypeId)) {
                                try {
                                    if (product != null) {
                                        GenericValue refurbItemAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(
                                                product.getRelated(x.MainProductAssoc, UtilMisc.toMap(x.productAssocTypeId, x.PRODUCT_REFURB),
                                                        UtilMisc.toList(x.sequenceNum), false)));
                                        if (refurbItemAssoc != null) {
                                            refurbItem = refurbItemAssoc.getRelatedOne(x.AssocProduct, false);
                                        }
                                    }
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, MODULE);
                                }
                                if (refurbItem != null) {
                                    boolean inventoryAvailable = false;
                                    try {
                                        Map<String, Object> invReqResult = dispatcher.runSync(x.isStoreInventoryAvailable,
                                                UtilMisc.toMap(x.productStoreId, orderHeader.get(x.productStoreId),
                                                x.productId, refurbItem.getString(x.productId),
                                                x.product, refurbItem, x.quantity, quantity));
                                        if (ServiceUtil.isError(invReqResult)) {
                                            Debug.logError(x.Error_calling_isStoreInventoryAvailable_service_result_is + invReqResult, MODULE);
                                        } else {
                                            inventoryAvailable = x.Y.equals(invReqResult.get(x.available_7b231a50));
                                        }
                                    } catch (GenericServiceException e) {
                                        Debug.logError(e, x.Fatal_error_calling_inventory_checking_services + e.toString(), MODULE);
                                    }
                                    if (!inventoryAvailable) {
                                        // If the Refurbished Equivalent is not available,
                                        // then use the original product.
                                        refurbItem = null;
                                    }
                                }

                                GenericValue newItem = delegator.makeValue(x.OrderItem, UtilMisc.toMap(x.orderItemSeqId,
                                        UtilFormatOut.formatPaddedNumber(itemCount++, 5)));
                                if (UtilValidate.isEmpty(refurbItem)) {
                                    newItem.set(x.productId, orderItem.get(x.productId));
                                    newItem.set(x.itemDescription, orderItem.get(x.itemDescription));
                                } else {
                                    newItem.set(x.productId, refurbItem.get(x.productId));
                                    newItem.set(x.itemDescription, ProductContentWrapper.getProductContentAsText(refurbItem, x.PRODUCT_NAME, locale,
                                            dispatcher, x.html));
                                }
                                newItem.set(x.orderItemTypeId, orderItem.get(x.orderItemTypeId));
                                newItem.set(x.productFeatureId, orderItem.get(x.productFeatureId));
                                newItem.set(x.prodCatalogId, orderItem.get(x.prodCatalogId));
                                newItem.set(x.productCategoryId, orderItem.get(x.productCategoryId));
                                newItem.set(x.quantity, quantity);
                                newItem.set(x.unitPrice, unitPrice);
                                newItem.set(x.unitListPrice, orderItem.get(x.unitListPrice));
                                newItem.set(x.comments, orderItem.get(x.comments));
                                newItem.set(x.correspondingPoId, orderItem.get(x.correspondingPoId));
                                newItem.set(x.statusId, x.ITEM_CREATED);
                                orderItems.add(newItem);

                                // Set the order item ship group information
                                // TODO: only the first ship group associated to the item
                                //       of the original order is considered and cloned,
                                //       anIs there a better way to handle this?d the returned units are assigned to it.
                                //
                                GenericValue orderItemShipGroupAssoc = null;
                                try {
                                    orderItemShipGroupAssoc = EntityUtil.getFirst(orderItem.getRelated(x.OrderItemShipGroupAssoc, null, null, false));
                                    if (orderItemShipGroupAssoc != null) {
                                        if (!orderItemShipGroupIds.contains(orderItemShipGroupAssoc.getString(x.shipGroupSeqId))) {
                                            GenericValue orderItemShipGroup = orderItemShipGroupAssoc.getRelatedOne(x.OrderItemShipGroup, false);
                                            GenericValue newOrderItemShipGroup = (GenericValue) orderItemShipGroup.clone();
                                            newOrderItemShipGroup.set(x.orderId, null);
                                            orderItemShipGroupInfo.add(newOrderItemShipGroup);
                                            orderItemShipGroupIds.add(orderItemShipGroupAssoc.getString(x.shipGroupSeqId));
                                        }
                                        GenericValue newOrderItemShipGroupAssoc = delegator.makeValue(x.OrderItemShipGroupAssoc,
                                                UtilMisc.toMap(x.orderItemSeqId, newItem.getString(x.orderItemSeqId), x.shipGroupSeqId,
                                                        orderItemShipGroupAssoc.getString(x.shipGroupSeqId), x.quantity, quantity));
                                        orderItemShipGroupInfo.add(newOrderItemShipGroupAssoc);
                                    }
                                } catch (GenericEntityException e) {
                                    String errMsg = x.Problem_calling_the_approveRequirement_service;
                                    Debug.logError(e, errMsg, MODULE);
                                    return ServiceUtil.returnError(errMsg);
                                }
                                // Create an association between the replacement order item and the order item of the original order
                                GenericValue newOrderItemAssoc = delegator.makeValue(x.OrderItemAssoc, UtilMisc.toMap(x.orderId,
                                        orderHeader.getString(x.orderId),
                                        x.orderItemSeqId, orderItem.getString(x.orderItemSeqId), x.shipGroupSeqId, x.NA,
                                        x.toOrderItemSeqId, newItem.getString(x.orderItemSeqId), x.toShipGroupSeqId, x.NA, x.orderItemAssocTypeId,
                                        x.REPLACEMENT));
                                orderItemAssocs.add(newOrderItemAssoc);

                                // For repair replacement orders, add to the order also the repair items
                                if (x.RTN_REPAIR_REPLACE.equals(returnTypeId)) {
                                    List<GenericValue> repairItems = null;
                                    try {
                                        if (product != null) {
                                            repairItems = EntityUtil.filterByDate(product.getRelated(x.MainProductAssoc,
                                                    UtilMisc.toMap(x.productAssocTypeId, x.PRODUCT_REPAIR_SRV), UtilMisc.toList(x.sequenceNum),
                                                    false));
                                        }
                                    } catch (GenericEntityException e) {
                                        Debug.logError(e, MODULE);
                                        continue;
                                    }
                                    if (UtilValidate.isNotEmpty(repairItems)) {
                                        for (GenericValue repairItem : repairItems) {
                                            GenericValue repairItemProduct = null;
                                            try {
                                                repairItemProduct = repairItem.getRelatedOne(x.AssocProduct, false);
                                            } catch (GenericEntityException e) {
                                                Debug.logError(e, MODULE);
                                                continue;
                                            }
                                            if (repairItemProduct != null) {
                                                BigDecimal repairUnitQuantity = repairItem.getBigDecimal(x.quantity);
                                                if (UtilValidate.isEmpty(repairUnitQuantity)) {
                                                    repairUnitQuantity = BigDecimal.ONE;
                                                }
                                                BigDecimal repairQuantity = quantity.multiply(repairUnitQuantity);
                                                newItem = delegator.makeValue(x.OrderItem, UtilMisc.toMap(x.orderItemSeqId,
                                                        UtilFormatOut.formatPaddedNumber(itemCount++, 5)));

                                                // price
                                                Map<String, Object> priceContext = new HashMap<>();
                                                priceContext.put(x.currencyUomId, orderHeader.get(x.currencyUom));
                                                if (placingPartyId != null) {
                                                    priceContext.put(x.partyId, placingPartyId);
                                                }
                                                priceContext.put(x.quantity, repairUnitQuantity);
                                                priceContext.put(x.product, repairItemProduct);
                                                priceContext.put(x.webSiteId, orderHeader.get(x.webSiteId));
                                                priceContext.put(x.productStoreId, orderHeader.get(x.productStoreId));
                                                // TODO: prodCatalogId, agreementId
                                                priceContext.put(x.productPricePurposeId, x.PURCHASE);
                                                priceContext.put(x.checkIncludeVat, x.Y);
                                                Map<String, Object> priceResult = null;
                                                try {
                                                    priceResult = dispatcher.runSync(x.calculateProductPrice, priceContext);
                                                } catch (GenericServiceException gse) {
                                                    Debug.logError(gse, MODULE);
                                                    continue;
                                                }
                                                if (ServiceUtil.isError(priceResult)) {
                                                    Debug.logError(ServiceUtil.getErrorMessage(priceResult), MODULE);
                                                    continue;
                                                }
                                                Boolean validPriceFound = (Boolean) priceResult.get(x.validPriceFound);
                                                if (Boolean.FALSE.equals(validPriceFound)) {
                                                    Debug.logError(x.Could_not_find_a_valid_price_for_the_product_with_ID
                                                            + repairItemProduct.get(x.productId) + x.str_76d00394, MODULE);
                                                    continue;
                                                }

                                                if (priceResult.get(x.listPrice) != null) {
                                                    newItem.set(x.unitListPrice, priceResult.get(x.listPrice));
                                                }

                                                BigDecimal repairUnitPrice = null;
                                                if (priceResult.get(x.basePrice) != null) {
                                                    repairUnitPrice = (BigDecimal) priceResult.get(x.basePrice);
                                                } else {
                                                    repairUnitPrice = BigDecimal.ZERO;
                                                }
                                                newItem.set(x.unitPrice, repairUnitPrice);

                                                newItem.set(x.productId, repairItemProduct.get(x.productId));
                                                // TODO: orderItemTypeId, prodCatalogId, productCategoryId
                                                newItem.set(x.quantity, repairQuantity);
                                                newItem.set(x.itemDescription, ProductContentWrapper.getProductContentAsText(repairItemProduct,
                                                        x.PRODUCT_NAME, locale, dispatcher, x.html));
                                                newItem.set(x.statusId, x.ITEM_CREATED);
                                                orderItems.add(newItem);
                                                additionalItemTotal = additionalItemTotal.add(repairQuantity.multiply(repairUnitPrice));
                                                if (orderItemShipGroupAssoc != null) {
                                                    GenericValue newOrderItemShipGroupAssoc = delegator.makeValue(x.OrderItemShipGroupAssoc,
                                                            UtilMisc.toMap(x.orderItemSeqId, newItem.getString(x.orderItemSeqId), x.shipGroupSeqId,
                                                                    orderItemShipGroupAssoc.getString(x.shipGroupSeqId), x.quantity, repairQuantity));
                                                    orderItemShipGroupInfo.add(newOrderItemShipGroupAssoc);
                                                }
                                                // Create an association between the repair order item and the order item of the original order
                                                newOrderItemAssoc = delegator.makeValue(x.OrderItemAssoc, UtilMisc.toMap(x.orderId,
                                                        orderHeader.getString(x.orderId),
                                                        x.orderItemSeqId, orderItem.getString(x.orderItemSeqId), x.shipGroupSeqId, x.NA,
                                                        x.toOrderItemSeqId, newItem.getString(x.orderItemSeqId), x.toShipGroupSeqId, x.NA,
                                                        x.orderItemAssocTypeId, x.REPLACEMENT));
                                                orderItemAssocs.add(newOrderItemAssoc);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    orderMap.put(x.orderItems, orderItems);
                    if (!orderItemShipGroupInfo.isEmpty()) {
                        orderMap.put(x.orderItemShipGroupInfo, orderItemShipGroupInfo);
                    }
                    if (!orderItemAssocs.isEmpty()) {
                        orderMap.put(x.orderItemAssociations, orderItemAssocs);
                    }
                } else {
                    Debug.logError(x.No_return_items_found, MODULE);
                    continue;
                }

                // create the replacement adjustment
                GenericValue adj = delegator.makeValue(x.OrderAdjustment);
                adj.set(x.orderAdjustmentTypeId, x.REPLACE_ADJUSTMENT);
                adj.set(x.amount, orderPriceTotal.negate());
                adj.set(x.comments, x.Replacement_Item_Return + returnId);
                adj.set(x.createdDate, nowTimestamp);
                adj.set(x.createdByUserLogin, userLogin.getString(x.userLoginId));
                orderMap.put(x.orderAdjustments, UtilMisc.toList(adj));

                // Payment preference
                if ((additionalItemTotal.compareTo(BigDecimal.ZERO) > 0)
                        || (x.RTN_CSREPLACE.equals(returnTypeId) && orderPriceTotal.compareTo(ZERO) > 0)) {
                    GenericValue paymentMethod = null;
                    try {
                        paymentMethod = returnHeader.getRelatedOne(x.PaymentMethod, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (paymentMethod != null) {
                        String paymentMethodId = paymentMethod.getString(x.paymentMethodId);
                        String paymentMethodTypeId = paymentMethod.getString(x.paymentMethodTypeId);
                        GenericValue opp = delegator.makeValue(x.OrderPaymentPreference);
                        opp.set(x.paymentMethodTypeId, paymentMethodTypeId);
                        opp.set(x.paymentMethodId, paymentMethodId);
                        // TODO: manualRefNum, manualAuthCode, securityCode, presentFlag, overflowFlag
                        if (paymentMethodId != null || x.FIN_ACCOUNT.equals(paymentMethodTypeId)) {
                            opp.set(x.statusId, x.PAYMENT_NOT_AUTH);
                        } else if (paymentMethodTypeId != null) {
                            // external payment method types require notification when received
                            // internal payment method types are assumed to be in-hand
                            if (paymentMethodTypeId.startsWith(x.EXT)) {
                                opp.set(x.statusId, x.PAYMENT_NOT_RECEIVED);
                            } else {
                                opp.set(x.statusId, x.PAYMENT_RECEIVED);
                            }
                        }
                        if (x.RTN_CSREPLACE.equals(returnTypeId)) {
                            opp.set(x.maxAmount, orderPriceTotal);
                        }
                        orderMap.put(x.orderPaymentInfo, UtilMisc.toList(opp));
                    }
                }

                // we'll assume new order is under same terms as original.  note orderTerms is a required parameter of storeOrder
                try {
                    orderMap.put(x.orderTerms, orderHeader.getRelated(x.OrderTerm, null, null, false));
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Cannot_create_replacement_order_because_order_terms_for_original_order_are_not_available, MODULE);
                }
                // we'll assume the new order has the same order roles of the original one
                try {
                    List<GenericValue> orderRoles = orderHeader.getRelated(x.OrderRole, null, null, false);
                    Map<String, List<String>> orderRolesMap = new HashMap<>();
                    if (orderRoles != null) {
                        for (GenericValue orderRole : orderRoles) {
                            List<String> parties = orderRolesMap.get(orderRole.getString(x.roleTypeId));
                            if (parties == null) {
                                parties = new LinkedList<>();
                                orderRolesMap.put(orderRole.getString(x.roleTypeId), parties);
                            }
                            parties.add(orderRole.getString(x.partyId));
                        }
                    }
                    if (!orderRolesMap.isEmpty()) {
                        orderMap.put(x.orderAdditionalPartyRoleMap, orderRolesMap);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Cannot_create_replacement_order_because_order_roles_for_original_order_are_not_available, MODULE);
                }

                // create the order
                String createdOrderId = null;
                Map<String, Object> orderResult = null;
                try {
                    orderResult = dispatcher.runSync(x.storeOrder, orderMap);
                    if (ServiceUtil.isError(orderResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(orderResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logInfo(e, x.Problem_creating_the_order, MODULE);
                }
                if (orderResult != null) {
                    createdOrderId = (String) orderResult.get(x.orderId);
                    createdOrderIds.add(createdOrderId);
                }

                // since there is no payments required; order is ready for processing/shipment
                if (createdOrderId != null) {
                    if (x.RETURN_ACCEPTED.equals(returnHeader.get(x.statusId)) && x.RTN_WAIT_REPLACE_RES.equals(returnTypeId)) {
                        Map<String, Object> serviceResult = null;
                        try {
                            serviceResult = dispatcher.runSync(x.changeOrderStatus, UtilMisc.toMap(x.orderId, createdOrderId, x.statusId,
                                    x.ORDER_HOLD, x.userLogin, userLogin));
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, x.Service_invocation_error_status_changes_were_not_updated_for_order + createdOrderId, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } else {
                        if (x.CUSTOMER_RETURN.equals(returnHeaderTypeId)) {
                            OrderChangeHelper.approveOrder(dispatcher, userLogin, createdOrderId);
                        } else {
                            try {
                                OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, createdOrderId, x.ORDER_APPROVED, null,
                                        x.ITEM_APPROVED, null);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, x.Service_invocation_error_status_changes_were_not_updated_for_order + createdOrderId, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderErrorCannotStoreStatusChanges, locale) + createdOrderId);
                            }
                        }
                    }

                    // create a ReturnItemResponse and attach to each ReturnItem
                    Map<String, Object> itemResponse = new HashMap<>();
                    itemResponse.put(x.replacementOrderId, createdOrderId);
                    itemResponse.put(x.responseAmount, orderPriceTotal);
                    itemResponse.put(x.responseDate, nowTimestamp);
                    itemResponse.put(x.userLogin, userLogin);
                    String returnItemResponseId = null;
                    try {
                        Map<String, Object> createReturnItemResponseResult = dispatcher.runSync(x.createReturnItemResponse, itemResponse);
                        if (ServiceUtil.isError(createReturnItemResponseResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.OrderProblemCreatingReturnItemResponseRecord, locale),
                                    null, null, createReturnItemResponseResult);
                        }
                        returnItemResponseId = (String) createReturnItemResponseResult.get(x.returnItemResponseId);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Problem_creating_ReturnItemResponse_record, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.OrderProblemCreatingReturnItemResponseRecord, locale));
                    }

                    for (GenericValue returnItem : returnItemList) {
                        Map<String, Object> updateReturnItemCtx = new HashMap<>();
                        updateReturnItemCtx.put(x.returnId, returnId);
                        updateReturnItemCtx.put(x.returnItemSeqId, returnItem.get(x.returnItemSeqId));
                        updateReturnItemCtx.put(x.returnItemResponseId, returnItemResponseId);
                        updateReturnItemCtx.put(x.userLogin, userLogin);
                        try {
                            Map<String, Object> updateReturnItemResult = dispatcher.runSync(x.updateReturnItem, updateReturnItemCtx);
                            if (ServiceUtil.isError(updateReturnItemResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                        x.OrderProblemStoringReturnItemUpdates, locale), null, null, updateReturnItemResult);
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, x.Could_not_update_ReturnItem_record, MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.OrderProblemStoringReturnItemUpdates, locale));
                        }
                    }
                }
            }
        }

        // create a return message AND create ReturnItemResponse record(s)
        StringBuilder successMessage = new StringBuilder();
        if (!createdOrderIds.isEmpty()) {
            successMessage.append(x.The_following_new_orders_have_been_created);
            Iterator<String> i = createdOrderIds.iterator();
            while (i.hasNext()) {
                successMessage.append(i.next());
                if (i.hasNext()) {
                    successMessage.append(x.str_d3bc9a37);
                }
            }
        } else {
            successMessage.append(x.No_orders_were_created);
        }

        return ServiceUtil.returnSuccess(successMessage.toString());
    }

    public static Map<String, Object> processSubscriptionReturn(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        Timestamp now = UtilDateTime.nowTimestamp();

        GenericValue returnHeader;
        List<GenericValue> returnItems = null;
        try {
            ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
            returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated(x.ReturnItem, UtilMisc.toMap(x.returnTypeId, x.RTN_REFUND), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (returnItems != null) {
            for (GenericValue returnItem : returnItems) {
                String orderItemSeqId = returnItem.getString(x.orderItemSeqId);
                String orderId = returnItem.getString(x.orderId);

                // lookup subscriptions
                List<GenericValue> subscriptions;
                try {
                    SubscriptionDao subscriptionDao = DaoRegistry.getDao(delegator, x.Subscription, SubscriptionDao.class);
                    subscriptions = subscriptionDao.findByAnd(delegator, x.Subscription,
                            UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId), null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // cancel all current subscriptions
                if (subscriptions != null) {
                    for (GenericValue subscription : subscriptions) {
                        Timestamp thruDate = subscription.getTimestamp(x.thruDate);
                        if (thruDate == null || thruDate.after(now)) {
                            subscription.set(x.thruDate, now);
                            try {
                                delegator.store(subscription);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Takes a List of returnItems and returns a Map of orderId -&gt; items and a Map of orderId -&gt; orderTotal
     * @param returnItems          a List of return items
     * @param returnItemsByOrderId the return items by order id
     * @param totalByOrder         the total by order id
     * @param delegator            the delegator
     * @param returnId             the return id
     * @param returnTypeId         the return type id
     */
    public static void groupReturnItemsByOrder(List<GenericValue> returnItems, Map<String, List<GenericValue>> returnItemsByOrderId,
                                               Map<String, BigDecimal> totalByOrder, Delegator delegator, String returnId, String returnTypeId) {
        for (GenericValue returnItem : returnItems) {
            String orderId = returnItem.getString(x.orderId);
            if (orderId != null) {
                if (returnItemsByOrderId != null) {
                    BigDecimal totalForOrder = null;
                    if (totalByOrder != null) {
                        totalForOrder = totalByOrder.get(orderId);
                    }

                    List<GenericValue> returnItemList = returnItemsByOrderId.get(orderId);
                    if (returnItemList == null) {
                        returnItemList = new LinkedList<>();
                    }
                    if (totalForOrder == null) {
                        totalForOrder = BigDecimal.ZERO;
                    }

                    // add to the items list
                    returnItemList.add(returnItem);
                    returnItemsByOrderId.put(orderId, returnItemList);

                    if (totalByOrder != null) {
                        // add on the total for this line
                        BigDecimal quantity = returnItem.getBigDecimal(x.returnQuantity);
                        BigDecimal amount = returnItem.getBigDecimal(x.returnPrice);
                        if (quantity == null) {
                            quantity = BigDecimal.ZERO;
                        }
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }
                        BigDecimal thisTotal = amount.multiply(quantity);
                        BigDecimal existingTotal = totalForOrder;
                        Map<String, Object> condition = UtilMisc.toMap(x.returnId, returnItem.get(x.returnId), x.returnItemSeqId,
                                returnItem.get(x.returnItemSeqId));
                        BigDecimal newTotal = existingTotal.add(thisTotal).add(getReturnAdjustmentTotal(delegator, condition));
                        totalByOrder.put(orderId, newTotal);
                    }
                }
            }
        }

        // We may also have some order-level adjustments, so we need to go through each order again and add those as well
        if ((totalByOrder != null) && (totalByOrder.entrySet() != null)) {
            for (Entry<String, BigDecimal> orderId : totalByOrder.entrySet()) {
                // find returnAdjustment for returnHeader
                Map<String, Object> condition = UtilMisc.<String, Object>toMap(x.returnId, returnId,
                        x.returnItemSeqId, org.apache.ofbiz.common.DataModelConstants.SEQ_ID_NA,
                        x.returnTypeId, returnTypeId);
                BigDecimal existingTotal = (totalByOrder.get(orderId.getKey()).add(getReturnAdjustmentTotal(delegator, condition)));
                totalByOrder.put(orderId.getKey(), existingTotal);
            }
        }
    }


    public static Map<String, Object> getReturnAmountByOrder(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        Locale locale = (Locale) context.get(x.locale);
        List<GenericValue> returnItems = null;
        Map<String, Object> returnAmountByOrder = new HashMap<>();
        try {
            ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
            returnItems = returnItemDao.findByAnd(delegator, x.ReturnItem, UtilMisc.toMap(x.returnId, returnId), null, false);

        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_return_information, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderErrorGettingReturnHeaderItemInformation, locale));
        }
        if ((returnItems != null) && (!returnItems.isEmpty())) {
            List<String> paymentList = new LinkedList<>();
            for (GenericValue returnItem : returnItems) {
                String orderId = returnItem.getString(x.orderId);
                try {
                    GenericValue returnItemResponse = returnItem.getRelatedOne(x.ReturnItemResponse, false);
                    if ((returnItemResponse != null) && (orderId != null)) {
                        // TODO should we filter on payment's status (PMNT_SENT, PMNT_RECEIVED)
                        GenericValue payment = returnItemResponse.getRelatedOne(x.Payment, false);
                        if ((payment != null) && (payment.getBigDecimal(x.amount) != null)
                                && !paymentList.contains(payment.get(x.paymentId))) {
                            UtilMisc.addToBigDecimalInMap(returnAmountByOrder, orderId, payment.getBigDecimal(x.amount));
                            paymentList.add(payment.getString(x.paymentId));  // make sure we don't add duplicated payment amount
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Problems_looking_up_return_item_related_information, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderErrorGettingReturnHeaderItemInformation, locale));
                }
            }
        }
        return UtilMisc.<String, Object>toMap(x.orderReturnAmountMap, returnAmountByOrder);
    }

    public static Map<String, Object> checkPaymentAmountForRefund(DispatchContext dctx, OrderReturnServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get(x.returnId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, BigDecimal> returnAmountByOrder = null;
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.getReturnAmountByOrder, org.apache.ofbiz.base.util.UtilMisc.toMap(x.returnId, returnId));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_running_the_getReturnAmountByOrder_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.OrderProblemsWithGetReturnAmountByOrder, locale));
        }
        returnAmountByOrder = UtilGenerics.cast(serviceResult.get(x.orderReturnAmountMap));

        if ((returnAmountByOrder != null) && (returnAmountByOrder.entrySet() != null)) {
            for (Entry<String, BigDecimal> orderId : returnAmountByOrder.entrySet()) {
                BigDecimal returnAmount = returnAmountByOrder.get(orderId.getKey());
                if (returnAmount == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderNoReturnAmountFound,
                            UtilMisc.toMap(x.orderId, orderId), locale));
                }
                if (returnAmount.abs().compareTo(new BigDecimal(x._0_000001)) < 0) {
                    Debug.logError(x.Order_486a9084 + orderId + x.refund_amount + returnAmount + x.less_than_zero, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderReturnTotalCannotLessThanZero, locale));
                }
                OrderReadHelper helper = new OrderReadHelper(delegator, orderId.getKey());
                BigDecimal grandTotal = helper.getOrderGrandTotal();
                if (returnAmount.subtract(grandTotal).compareTo(new BigDecimal(x._0_01)) > 0) {
                    Debug.logError(x.Order_486a9084 + orderId + x.refund_amount + returnAmount + x.exceeds_order_total + grandTotal + x.str_4ff447b8, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.OrderRefundAmountExceedsOrderTotal, locale));
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createReturnAdjustment(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String orderAdjustmentId = (String) context.get(x.orderAdjustmentId);
        String returnAdjustmentTypeId = (String) context.get(x.returnAdjustmentTypeId);
        String returnId = (String) context.get(x.returnId);
        String returnItemSeqId = (String) context.get(x.returnItemSeqId);
        String description = (String) context.get(x.description);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue returnItemTypeMap = null;
        GenericValue orderAdjustment = null;
        GenericValue returnAdjustmentType = null;
        GenericValue orderItem = null;
        GenericValue returnItem = null;
        GenericValue returnHeader = null;

        // if orderAdjustment is not empty, then copy most return adjustment information from orderAdjustment's
        if (orderAdjustmentId != null) {
            try {
                OrderAdjustmentDao orderAdjustmentDao = DaoRegistry.getDao(delegator, x.OrderAdjustment, OrderAdjustmentDao.class);
                orderAdjustment = orderAdjustmentDao.findOne(delegator, x.OrderAdjustment,
                        UtilMisc.toMap(x.orderAdjustmentId, orderAdjustmentId), false);
                if (orderAdjustment == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.OrderCreateReturnAdjustmentNotFoundOrderAdjustment,
                            UtilMisc.toMap(x.orderAdjustmentId, orderAdjustmentId), locale));
                }
                // get returnHeaderTypeId from ReturnHeader and then use it to figure out return item type mapping
                ReturnHeaderDao returnHeaderDao = DaoRegistry.getDao(delegator, x.ReturnHeader, ReturnHeaderDao.class);
                returnHeader = returnHeaderDao.findOne(delegator, x.ReturnHeader, UtilMisc.toMap(x.returnId, returnId), false);
                String returnHeaderTypeId = ((returnHeader != null) && (returnHeader.getString(x.returnHeaderTypeId) != null))
                        ? returnHeader.getString(x.returnHeaderTypeId) : x.CUSTOMER_RETURN;
                ReturnItemTypeMapDao returnItemTypeMapDao = DaoRegistry.getDao(delegator, x.ReturnItemTypeMap, ReturnItemTypeMapDao.class);
                returnItemTypeMap = returnItemTypeMapDao.findOne(delegator, x.ReturnItemTypeMap,
                        UtilMisc.toMap(x.returnHeaderTypeId, returnHeaderTypeId, x.returnItemMapKey, orderAdjustment.get(x.orderAdjustmentTypeId)),
                        false);
                returnAdjustmentType = returnItemTypeMap.getRelatedOne(x.ReturnAdjustmentType, false);
                if (returnAdjustmentType != null && UtilValidate.isEmpty(description)) {
                    description = returnAdjustmentType.getString(x.description);
                }
                if ((returnItemSeqId != null) && !(x.NA.equals(returnItemSeqId))) {
                    ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
                    returnItem = returnItemDao.findOne(delegator, x.ReturnItem,
                            UtilMisc.toMap(x.returnId, returnId, x.returnItemSeqId, returnItemSeqId), false);
                    Debug.logInfo(x.returnId_da51b28b + returnId + x.returnItemSeqId_196de87b + returnItemSeqId, MODULE);
                    orderItem = returnItem.getRelatedOne(x.OrderItem, false);
                } else {
                    // we don't have the returnItemSeqId but before we consider this
                    // an header adjustment we try to get a return item in this return
                    // associated to the same order item to which the adjustments refers (if any)
                    if (UtilValidate.isNotEmpty(orderAdjustment.getString(x.orderItemSeqId))
                            && !x.NA.equals(orderAdjustment.getString(x.orderItemSeqId))) {
                        ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
                        returnItem = returnItemDao.findFirstByCondition(delegator, x.ReturnItem, EntityCondition.makeCondition(
                                UtilMisc.toMap(x.returnId, returnId, x.orderId, orderAdjustment.getString(x.orderId), x.orderItemSeqId,
                                        orderAdjustment.getString(x.orderItemSeqId))), null, null, false);
                        if (returnItem != null) {
                            orderItem = returnItem.getRelatedOne(x.OrderItem, false);
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                throw new GeneralRuntimeException(e.getMessage());
            }
            context.putAll(orderAdjustment.getAllFields());
            if (UtilValidate.isNotEmpty(amount)) {
                context.put(x.amount, amount);
            }
        }

        // if orderAdjustmentTypeId is empty, ie not found from orderAdjustmentId, then try to get returnAdjustmentTypeId from returnItemTypeMap,
        // if still empty, use default RET_MAN_ADJ
        if (returnAdjustmentTypeId == null) {
            String mappingTypeId = returnItemTypeMap != null ? returnItemTypeMap.get(x.returnItemTypeId).toString() : null;
            returnAdjustmentTypeId = mappingTypeId != null ? mappingTypeId : x.RET_MAN_ADJ;
        }
        // calculate the returnAdjustment amount
        if (returnItem != null) {  // returnAdjustment for returnItem
            if (needRecalculate(returnAdjustmentTypeId)) {
                Debug.logInfo(x.returnPrice_03c73434 + returnItem.getBigDecimal(x.returnPrice) + x.returnQuantity_b5687dd9
                        + returnItem.getBigDecimal(x.returnQuantity) + x.sourcePercentage_b50c195c + orderAdjustment.getBigDecimal(x.sourcePercentage),
                        MODULE);
                BigDecimal returnTotal = returnItem.getBigDecimal(x.returnPrice).multiply(returnItem.getBigDecimal(x.returnQuantity));
                BigDecimal orderTotal = orderItem.getBigDecimal(x.quantity).multiply(orderItem.getBigDecimal(x.unitPrice));
                amount = getAdjustmentAmount(x.RET_SALES_TAX_ADJ.equals(returnAdjustmentTypeId), returnTotal, orderTotal,
                        orderAdjustment.getBigDecimal(x.amount));
            } else {
                amount = (BigDecimal) context.get(x.amount);
            }
        } else { // returnAdjustment for returnHeader
            amount = (BigDecimal) context.get(x.amount);
        }

        // store the return adjustment
        String seqId = delegator.getNextSeqId(x.ReturnAdjustment);
        GenericValue newReturnAdjustment = delegator.makeValue(x.ReturnAdjustment,
                UtilMisc.toMap(x.returnAdjustmentId, seqId));

        try {
            newReturnAdjustment.setNonPKFields(context);
            if (orderAdjustment != null && orderAdjustment.get(x.taxAuthorityRateSeqId) != null) {
                newReturnAdjustment.set(x.taxAuthorityRateSeqId, orderAdjustment.getString(x.taxAuthorityRateSeqId));
            }
            newReturnAdjustment.set(x.amount, amount == null ? BigDecimal.ZERO : amount);
            newReturnAdjustment.set(x.returnAdjustmentTypeId, returnAdjustmentTypeId);
            newReturnAdjustment.set(x.description, description);
            newReturnAdjustment.set(x.returnItemSeqId, UtilValidate.isEmpty(returnItemSeqId) ? x.NA : returnItemSeqId);

            delegator.create(newReturnAdjustment);
            Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                    x.OrderCreateReturnAdjustment, UtilMisc.toMap(x.seqId, seqId), locale));
            result.put(x.returnAdjustmentId, seqId);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Failed_to_store_returnAdjustment, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderCreateReturnAdjustmentFailed, locale));
        }
    }

    public static Map<String, Object> updateReturnAdjustment(DispatchContext dctx, OrderReturnServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue returnItem = null;
        GenericValue returnAdjustment = null;
        String returnAdjustmentTypeId = null;
        BigDecimal amount;


        try {
            ReturnAdjustmentDao returnAdjustmentDao = DaoRegistry.getDao(delegator, x.ReturnAdjustment, ReturnAdjustmentDao.class);
            returnAdjustment = returnAdjustmentDao.findOne(delegator, x.ReturnAdjustment,
                    UtilMisc.toMap(x.returnAdjustmentId, context.get(x.returnAdjustmentId)), false);
            if (returnAdjustment != null) {
                ReturnItemDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, ReturnItemDao.class);
                returnItem = returnItemDao.findOne(delegator, x.ReturnItem,
                        UtilMisc.toMap(x.returnId, returnAdjustment.get(x.returnId), x.returnItemSeqId, returnAdjustment.get(x.returnItemSeqId)),
                        false);
                returnAdjustmentTypeId = returnAdjustment.getString(x.returnAdjustmentTypeId);
            }

            // calculate the returnAdjustment amount
            if (returnItem != null) {  // returnAdjustment for returnItem
                BigDecimal originalReturnPrice = (context.get(x.originalReturnPrice) != null) ? ((BigDecimal) context.get(x.originalReturnPrice))
                        : returnItem.getBigDecimal(x.returnPrice);
                BigDecimal originalReturnQuantity = (context.get(x.originalReturnQuantity) != null)
                        ? ((BigDecimal) context.get(x.originalReturnQuantity)) : returnItem.getBigDecimal(x.returnQuantity);

                if (needRecalculate(returnAdjustmentTypeId)) {
                    BigDecimal returnTotal = returnItem.getBigDecimal(x.returnPrice).multiply(returnItem.getBigDecimal(x.returnQuantity));
                    BigDecimal originalReturnTotal = originalReturnPrice.multiply(originalReturnQuantity);
                    amount = getAdjustmentAmount(x.RET_SALES_TAX_ADJ.equals(returnAdjustmentTypeId), returnTotal, originalReturnTotal,
                            returnAdjustment.getBigDecimal(x.amount));
                } else {
                    amount = (BigDecimal) context.get(x.amount);
                }
            } else { // returnAdjustment for returnHeader
                amount = (BigDecimal) context.get(x.amount);
            }

            Map<String, Object> result = null;
            if (UtilValidate.isNotEmpty(amount)) {
                returnAdjustment.setNonPKFields(context);
                returnAdjustment.set(x.amount, amount);
                delegator.store(returnAdjustment);
                Debug.logInfo(x.Update_ReturnAdjustment_with_Id + context.get(x.returnAdjustmentId) + x.to_amount + amount + x.successfully,
                        MODULE);
                result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                        x.OrderUpdateReturnAdjustment,
                        UtilMisc.toMap(x.returnAdjustmentId, context.get(x.returnAdjustmentId), x.amount, amount), locale));
            } else {
                result = ServiceUtil.returnSuccess();
            }
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Failed_to_store_returnAdjustment, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.OrderCreateReturnAdjustmentFailed, locale));
        }
    }

    //  used as a dispatch service, invoke different service based on the parameters passed in
    public static Map<String, Object> createReturnItemOrAdjustment(DispatchContext dctx, OrderReturnServicesContext context) {
        Debug.logInfo(x.createReturnItemOrAdjustment_s_context + context, MODULE);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);
        Debug.logInfo(x.orderItemSeqId_730db3eb + orderItemSeqId + x.str_d08f88df, MODULE);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isNotEmpty(orderItemSeqId) ? x.createReturnItem : x.createReturnAdjustment;
        Debug.logInfo(x.serviceName_b0aed370 + serviceName, MODULE);
        try {
            Map<String, Object> inMap = dctx.makeValidContext(serviceName, ModelService.IN_PARAM, context);
            if (x.createReturnItem.equals(serviceName)) {
                // we don't want to automatically include the adjustments
                // when the return item is created because they are selectable by the user
                inMap.put(x.includeAdjustments, x.N);
            }
            Map<String, Object> serviceResult = dispatcher.runSync(serviceName, inMap);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            return serviceResult;
        } catch (org.apache.ofbiz.service.GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    //  used as a dispatch service, invoke different service based on the parameters passed in
    public static Map<String, Object> updateReturnItemOrAdjustment(DispatchContext dctx, OrderReturnServicesContext context) {
        Debug.logInfo(x.updateReturnItemOrAdjustment_s_context + context, MODULE);
        String returnAdjustmentId = (String) context.get(x.returnAdjustmentId);
        Debug.logInfo(x.returnAdjustmentId_3474551c + returnAdjustmentId + x.str_d08f88df, MODULE);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isEmpty(returnAdjustmentId) ? x.updateReturnItem : x.updateReturnAdjustment;
        Debug.logInfo(x.serviceName_b0aed370 + serviceName, MODULE);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(serviceName, dctx.makeValidContext(serviceName, ModelService.IN_PARAM, context));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            return serviceResult;
        } catch (org.apache.ofbiz.service.GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * These return adjustment types need to be recalculated when the return item is updated
     * @param returnAdjustmentTypeId the return adjustment type id
     * @return returns if the returnn adjustment need to be recalculated
     */
    public static boolean needRecalculate(String returnAdjustmentTypeId) {
        return x.RET_PROMOTION_ADJ.equals(returnAdjustmentTypeId)
                || x.RET_DISCOUNT_ADJ.equals(returnAdjustmentTypeId) || x.RET_SALES_TAX_ADJ.equals(returnAdjustmentTypeId);
    }

    /**
     * Get the total return adjustments for a set of key -&gt; value condition pairs.  Done for code efficiency.
     * @param delegator the delegator
     * @param condition the conditions to use
     * @return return the total return adjustments
     */
    public static BigDecimal getReturnAdjustmentTotal(Delegator delegator, Map<String, ? extends Object> condition) {
        BigDecimal total = BigDecimal.ZERO;
        List<GenericValue> adjustments;
        try {
            // TODO: find on a view-entity with a sum is probably more efficient
            ReturnAdjustmentDao returnAdjustmentDao = DaoRegistry.getDao(delegator, x.ReturnAdjustment, ReturnAdjustmentDao.class);
            adjustments = returnAdjustmentDao.findByCondition(delegator, x.ReturnAdjustment, EntityCondition.makeCondition(condition), null, null,
                    null, false);
            if (adjustments != null) {
                for (GenericValue returnAdjustment : adjustments) {
                    if ((returnAdjustment != null) && (returnAdjustment.get(x.amount) != null)) {
                        total = total.add(returnAdjustment.getBigDecimal(x.amount));
                    }
                }
            }
        } catch (org.apache.ofbiz.entity.GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return total;
    }

    /**
     * Get rid of unnecessary parameters based on the given service name
     * @param dctx        Service DispatchContext
     * @param serviceName the service name
     * @param context     context before clean up
     * @return filtered context
     * @throws GenericServiceException
     * @deprecated - Use DispatchContext.makeValidContext(String, String, Map) instead
     */
    @Deprecated
    public static Map<String, Object> filterServiceContext(DispatchContext dctx, String serviceName, OrderReturnServicesContext context)
            throws GenericServiceException {
        return dctx.makeValidContext(serviceName, ModelService.IN_PARAM, context);
    }

    /**
     * Calculate new returnAdjustment amount and set scale and rounding mode based on returnAdjustmentType: RET_SALES_TAX_ADJ use sales.tax.
     * @param isSalesTax    if returnAdjustmentType is SaleTax
     * @param returnTotal
     * @param originalTotal
     * @param amount
     * @return new returnAdjustment amount
     */
    public static BigDecimal getAdjustmentAmount(boolean isSalesTax, BigDecimal returnTotal, BigDecimal originalTotal, BigDecimal amount) {
        String settingPrefix = isSalesTax ? x.salestax : x.order;
        String decimalsPrefix = isSalesTax ? x.calc : x.emptyString;
        int decimals = UtilNumber.getBigDecimalScale(settingPrefix + decimalsPrefix + x.decimals);
        RoundingMode rounding = UtilNumber.getRoundingMode(settingPrefix + x.rounding_357a8387);
        returnTotal = returnTotal.setScale(decimals, rounding);
        originalTotal = originalTotal.setScale(decimals, rounding);
        BigDecimal newAmount = null;
        if (ZERO.compareTo(originalTotal) != 0) {
            newAmount = returnTotal.multiply(amount).divide(originalTotal, decimals, rounding);
        } else {
            newAmount = ZERO;
        }
        return newAmount;
    }
}
