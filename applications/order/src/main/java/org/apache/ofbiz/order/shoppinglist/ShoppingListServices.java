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
package org.apache.ofbiz.order.shoppinglist;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.calendar.RecurrenceInfo;
import org.apache.ofbiz.service.calendar.RecurrenceInfoException;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;

import javax.transaction.Transaction;
import org.apache.ofbiz.base.util.collections.PagedList;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ShoppingListServicesContext;
/**
 * Shopping List Services
 */
public class ShoppingListServices {

    private static final String MODULE = ShoppingListServices.class.getName();
    private static final String RES_ERROR = x.OrderErrorUiLabels;

    public static Map<String, Object> setShoppingListRecurrence(DispatchContext dctx, ShoppingListServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp startDate = (Timestamp) context.get(x.startDateTime);
        Timestamp endDate = (Timestamp) context.get(x.endDateTime);
        Integer frequency = (Integer) context.get(x.frequency);
        Integer interval = (Integer) context.get(x.intervalNumber);
        Locale locale = (Locale) context.get(x.locale);

        if (frequency == null || interval == null) {
            Debug.logWarning(UtilProperties.getMessage(RES_ERROR, x.OrderFrequencyOrIntervalWasNotSpecified, locale), MODULE);
            return ServiceUtil.returnSuccess();
        }

        if (startDate == null) {
            switch (frequency) {
            case 5:
                startDate = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            case 6:
                startDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            case 7:
                startDate = UtilDateTime.getYearStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            default:
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderInvalidFrequencyForShoppingListRecurrence, locale));
            }
        }

        long startTime = startDate.getTime();
        long endTime = 0;
        if (endDate != null) {
            endTime = endDate.getTime();
        }

        RecurrenceInfo recInfo = null;
        try {
            recInfo = RecurrenceInfo.makeInfo(delegator, startTime, frequency, interval, -1, endTime);
        } catch (RecurrenceInfoException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToCreateShoppingListRecurrenceInformation, locale));
        }

        Debug.logInfo(x.Next_Recurrence + UtilDateTime.getTimestamp(recInfo.next()), MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.recurrenceInfoId, recInfo.getID());

        return result;
    }

    public static Map<String, Object> createListReorders(DispatchContext dctx, ShoppingListServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        boolean beganTransaction = false;
        UserLoginDao shoppingListDao = DaoRegistry.getDao(delegator, x.ShoppingList, UserLoginDao.class);
        EntityCondition shoppingListCond = EntityCondition.makeCondition(UtilMisc.toMap(x.shoppingListTypeId, x.SLT_AUTO_REODR, x.isActive, x.Y));
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException e1) {
            Debug.logError(e1, x.Delegator_Could_not_begin_transaction + e1.toString(), MODULE);
        }

        try (EntityListIterator eli = shoppingListDao.findIteratorByCondition(delegator, x.ShoppingList, shoppingListCond, null,
                UtilMisc.toList(x.lastOrderedDate_761028db),
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true))) {
            if (eli != null) {
                GenericValue shoppingList;
                while (((shoppingList = eli.next()) != null)) {
                    Timestamp lastOrder = shoppingList.getTimestamp(x.lastOrderedDate);
                    RecurrenceInfo recurrence = null;

                    GenericValue recurrenceInfo = shoppingList.getRelatedOne(x.RecurrenceInfo, false);
                    Timestamp startDateTime = recurrenceInfo.getTimestamp(x.startDateTime);

                    try {
                        recurrence = new RecurrenceInfo(recurrenceInfo);
                    } catch (RecurrenceInfoException e) {
                        Debug.logError(e, MODULE);
                    }


                    // check the next recurrence
                    if (recurrence != null) {
                        long next = lastOrder == null ? recurrence.next(startDateTime.getTime()) : recurrence.next(lastOrder.getTime());
                        Timestamp now = UtilDateTime.nowTimestamp();
                        Timestamp nextOrder = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(next));

                        if (nextOrder.after(now)) {
                            continue;
                        }
                    } else {
                        continue;
                    }

                    ShoppingCart listCart = makeShoppingListCart(dispatcher, shoppingList, locale);
                    CheckOutHelper helper = new CheckOutHelper(dispatcher, delegator, listCart);

                    // store the order
                    Map<String, Object> createResp = helper.createOrder(userLogin);
                    if (createResp == null || ServiceUtil.isError(createResp)) {
                        Debug.logError(x.Cannot_create_order_for_shopping_list + shoppingList, MODULE);
                    } else {

                        String orderId = (String) createResp.get(x.orderId);

                        // authorize the payments
                        Map<String, Object> payRes = null;
                        try {
                            payRes = helper.processPayment(ProductStoreWorker.getProductStore(listCart.getProductStoreId(), delegator), userLogin);
                        } catch (GeneralException e) {
                            Debug.logError(e, MODULE);
                        }

                        if (payRes != null && ServiceUtil.isError(payRes)) {
                            Debug.logError(x.Payment_processing_problems_with_shopping_list + shoppingList, MODULE);
                        }

                        shoppingList.set(x.lastOrderedDate, UtilDateTime.nowTimestamp());
                        shoppingList.store();

                        // send notification
                        try {
                            dispatcher.runAsync(x.sendOrderPayRetryNotification, UtilMisc.toMap(x.orderId, orderId));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }

                        // increment the recurrence
                        recurrence.incrementCurrentCount();
                    }
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, x.Error_creating_shopping_list_auto_reorders, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, x.Delegator_Could_not_rollback_transaction + e2.toString(), MODULE);
            }

            String errMsg = UtilProperties.getMessage(RES_ERROR, x.OrderErrorWhileCreatingNewShoppingListBasedAutomaticReorder, UtilMisc.toMap(
                    x.errorString, e.toString()), locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Could_not_commit_transaction_for_creating_new_shopping_list_based_automatic_reorder, MODULE);
            }
        }
    }

    public static Map<String, Object> splitShipmentMethodString(DispatchContext dctx, ShoppingListServicesContext context) {
        String shipmentMethodString = (String) context.get(x.shippingMethodString);
        Map<String, Object> result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(shipmentMethodString)) {
            int delimiterPos = shipmentMethodString.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                result.put(x.shipmentMethodTypeId, shipmentMethodTypeId);
                result.put(x.carrierPartyId, carrierPartyId);
            }
        }
        return result;
    }

    public static Map<String, Object> makeListFromOrder(DispatchContext dctx, ShoppingListServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String shoppingListTypeId = (String) context.get(x.shoppingListTypeId);
        String shoppingListId = (String) context.get(x.shoppingListId);
        String orderId = (String) context.get(x.orderId);
        String partyId = (String) context.get(x.partyId);

        Timestamp startDate = (Timestamp) context.get(x.startDateTime);
        Timestamp endDate = (Timestamp) context.get(x.endDateTime);
        Integer frequency = (Integer) context.get(x.frequency);
        Integer interval = (Integer) context.get(x.intervalNumber);

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            GenericValue orderHeader = null;
            UserLoginDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class);
            orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);

            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToLocateOrder, UtilMisc.toMap(x.orderId, orderId),
                        locale));
            }
            String productStoreId = orderHeader.getString(x.productStoreId);

            if (UtilValidate.isEmpty(shoppingListId)) {
                // create a new shopping list
                if (partyId == null) {
                    partyId = userLogin.getString(x.partyId);
                }

                Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.partyId, partyId,
                        x.productStoreId, productStoreId, x.listName, x.List_Created_From_Order + orderId);

                if (UtilValidate.isNotEmpty(shoppingListTypeId)) {
                    serviceCtx.put(x.shoppingListTypeId, shoppingListTypeId);
                }

                Map<String, Object> newListResult = null;
                try {

                    newListResult = dispatcher.runSync(x.createShoppingList, serviceCtx, 90, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problems_creating_new_ShoppingList, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToCreateNewShoppingList, locale));
                }

                // check for errors
                if (ServiceUtil.isError(newListResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(newListResult));
                }

                // get the new list id
                if (newListResult != null) {
                    shoppingListId = (String) newListResult.get(x.shoppingListId);
                }
            }

            GenericValue shoppingList = null;
            UserLoginDao shoppingListDao = DaoRegistry.getDao(delegator, x.ShoppingList, UserLoginDao.class);
            shoppingList = shoppingListDao.findOne(delegator, x.ShoppingList, UtilMisc.toMap(x.shoppingListId, shoppingListId), false);

            if (shoppingList == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderNoShoppingListAvailable, locale));
            }
            shoppingListTypeId = shoppingList.getString(x.shoppingListTypeId);

            OrderReadHelper orh;
            try {
                orh = new OrderReadHelper(orderHeader);
            } catch (IllegalArgumentException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToLoadOrderReadHelper, UtilMisc.toMap(x.orderId,
                        orderId), locale));
            }

            List<GenericValue> orderItems = orh.getOrderItems();
            for (GenericValue orderItem : orderItems) {
                String productId = orderItem.getString(x.productId);
                if (UtilValidate.isNotEmpty(productId)) {
                    Map<String, Object> ctx = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.shoppingListId, shoppingListId, x.productId,
                            orderItem.get(x.productId), x.quantity, orderItem.get(x.quantity));
                    if (EntityTypeUtil.hasParentType(delegator, x.ProductType, x.productTypeId, ProductWorker.getProductTypeId(delegator,
                            productId), x.parentTypeId, x.AGGREGATED)) {
                        try {
                            UserLoginDao productDao = DaoRegistry.getDao(delegator, x.Product, UserLoginDao.class);
                            GenericValue instanceProduct = productDao.findOne(delegator, x.Product, UtilMisc.toMap(x.productId, productId), false);
                            String configId = instanceProduct.getString(x.configId);
                            ctx.put(x.configId, configId);
                            String aggregatedProductId = ProductWorker.getInstanceAggregatedId(delegator, productId);
                            //override the instance productId with aggregated productId
                            ctx.put(x.productId, aggregatedProductId);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync(x.createShoppingListItem, ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (serviceResult == null || ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToAddItemToShoppingList, UtilMisc.toMap(
                                x.shoppingListId, shoppingListId), locale));
                    }
                }
            }

            if (x.SLT_AUTO_REODR.equals(shoppingListTypeId)) {
                GenericValue paymentPref = EntityUtil.getFirst(orh.getPaymentPreferences());
                GenericValue shipGroup = EntityUtil.getFirst(orh.getOrderItemShipGroups());

                Map<String, Object> slCtx = new HashMap<>();
                slCtx.put(x.shipmentMethodTypeId, shipGroup.get(x.shipmentMethodTypeId));
                slCtx.put(x.carrierRoleTypeId, shipGroup.get(x.carrierRoleTypeId));
                slCtx.put(x.carrierPartyId, shipGroup.get(x.carrierPartyId));
                slCtx.put(x.contactMechId, shipGroup.get(x.contactMechId));
                slCtx.put(x.paymentMethodId, paymentPref.get(x.paymentMethodId));
                slCtx.put(x.currencyUom, orh.getCurrency());
                slCtx.put(x.startDateTime, startDate);
                slCtx.put(x.endDateTime, endDate);
                slCtx.put(x.frequency, frequency);
                slCtx.put(x.intervalNumber, interval);
                slCtx.put(x.isActive, x.Y);
                slCtx.put(x.shoppingListId, shoppingListId);
                slCtx.put(x.userLogin, userLogin);

                Map<String, Object> slUpResp = null;
                try {
                    slUpResp = dispatcher.runSync(x.updateShoppingList, slCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }

                if (slUpResp == null || ServiceUtil.isError(slUpResp)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToUpdateShoppingListInformation,
                            UtilMisc.toMap(x.shoppingListId, shoppingListId), locale));
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.shoppingListId, shoppingListId);
            return result;

        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, x.Error_making_shopping_list_from_order, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, x.Delegator_Could_not_rollback_transaction + e2.toString(), MODULE);
            }

            String errMsg = UtilProperties.getMessage(RES_ERROR, x.OrderErrorWhileCreatingNewShoppingListBasedOnOrder, UtilMisc.toMap(x.errorString,
                    e.toString()), locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Could_not_commit_transaction_for_creating_new_shopping_list_based_on_order, MODULE);
            }
        }
    }

    /**
     * Create a new shoppingCart form a shoppingList
     * @param dispatcher   the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale       the locale in use
     * @return returns a new shopping cart form a shopping list
     */
    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        return makeShoppingListCart(null, dispatcher, shoppingList, locale);
    }

    /**
     * Add a shoppinglist to an existing shoppingcart
     * @param listCart     the shopping cart list
     * @param dispatcher   the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale       the locale in use
     * @return the modified shopping cart adding the shopping list elements
     */
    public static ShoppingCart makeShoppingListCart(ShoppingCart listCart, LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        if (shoppingList != null && shoppingList.get(x.productStoreId) != null) {
            String productStoreId = shoppingList.getString(x.productStoreId);
            String currencyUom = shoppingList.getString(x.currencyUom);
            if (currencyUom == null) {
                GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                if (productStore == null) {
                    return null;
                }
                currencyUom = productStore.getString(x.defaultCurrencyUomId);
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }

            List<GenericValue> items = null;
            try {
                items = shoppingList.getRelated(x.ShoppingListItem, null, UtilMisc.toList(x.shoppingListItemSeqId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            if (UtilValidate.isNotEmpty(items)) {
                if (listCart == null) {
                    listCart = new ShoppingCart(delegator, productStoreId, locale, currencyUom);
                    listCart.setOrderPartyId(shoppingList.getString(x.partyId));
                    listCart.setAutoOrderShoppingListId(shoppingList.getString(x.shoppingListId));
                } else {
                    if (!listCart.getPartyId().equals(shoppingList.getString(x.partyId))) {
                        Debug.logError(x.CANNOT_add_shoppingList + shoppingList.getString(x.shoppingListId)
                                + x.of_partyId + shoppingList.getString(x.partyId)
                                + x.to_a_shoppingcart_with_a_different_orderPartyId
                                + listCart.getPartyId(), MODULE);
                        return listCart;
                    }
                }


                ProductConfigWrapper configWrapper = null;
                for (GenericValue shoppingListItem : items) {
                    String productId = shoppingListItem.getString(x.productId);
                    BigDecimal quantity = shoppingListItem.getBigDecimal(x.quantity);
                    Timestamp reservStart = shoppingListItem.getTimestamp(x.reservStart);
                    BigDecimal reservLength = null;
                    String configId = shoppingListItem.getString(x.configId);

                    if (shoppingListItem.get(x.reservLength) != null) {
                        reservLength = shoppingListItem.getBigDecimal(x.reservLength);
                    }
                    BigDecimal reservPersons = null;
                    if (shoppingListItem.get(x.reservPersons) != null) {
                        reservPersons = shoppingListItem.getBigDecimal(x.reservPersons);
                    }
                    if (UtilValidate.isNotEmpty(productId) && quantity != null) {
                        if (UtilValidate.isNotEmpty(configId)) {
                            configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId,
                                    listCart.getProductStoreId(), null, listCart.getWebSiteId(), listCart.getCurrency(), listCart.getLocale(),
                                    listCart.getAutoUserLogin());
                        }
                        // list items are noted in the shopping cart
                        String listId = shoppingListItem.getString(x.shoppingListId);
                        String itemId = shoppingListItem.getString(x.shoppingListItemSeqId);
                        Map<String, Object> attributes = UtilMisc.<String, Object>toMap(x.shoppingListId, listId, x.shoppingListItemSeqId, itemId);

                        try {
                            listCart.addOrIncreaseItem(productId, null, quantity, reservStart, reservLength, reservPersons, null, null, null, null,
                                    null, attributes, null, configWrapper, null, null, null, dispatcher);
                        } catch (CartItemModifyException e) {
                            Debug.logError(e, x.Unable_to_add_product_to_List_Cart + productId, MODULE);
                        } catch (ItemNotFoundException e) {
                            Debug.logError(e, x.Product_not_found + productId, MODULE);
                        }
                    }
                }

                if (listCart.size() > 0) {
                    if (UtilValidate.isNotEmpty(shoppingList.get(x.paymentMethodId))) {
                        listCart.addPayment(shoppingList.getString(x.paymentMethodId));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get(x.contactMechId))) {
                        listCart.setAllShippingContactMechId(shoppingList.getString(x.contactMechId));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get(x.shipmentMethodTypeId))) {
                        listCart.setAllShipmentMethodTypeId(shoppingList.getString(x.shipmentMethodTypeId));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get(x.carrierPartyId))) {
                        listCart.setAllCarrierPartyId(shoppingList.getString(x.carrierPartyId));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.getString(x.productPromoCodeId))) {
                        listCart.addProductPromoCode(shoppingList.getString(x.productPromoCodeId), dispatcher);
                    }
                }
            }
        }
        return listCart;
    }

    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, String shoppingListId, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue shoppingList = null;
        try {
            UserLoginDao shoppingListDao = DaoRegistry.getDao(delegator, x.ShoppingList, UserLoginDao.class);
            shoppingList = shoppingListDao.findOne(delegator, x.ShoppingList, UtilMisc.toMap(x.shoppingListId, shoppingListId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return makeShoppingListCart(dispatcher, shoppingList, locale);
    }

    /**
     * Given an orderId, this service will look through all its OrderItems and for each shoppingListItemId
     * and shoppingListItemSeqId, update the quantity purchased in the ShoppingListItem entity.  Used for
     * tracking how many of shopping list items are purchased.  This service is mounted as a seca on storeOrder.
     * @param ctx     - The DispatchContext that this service is operating in
     * @param context - Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateShoppingListQuantitiesFromOrder(DispatchContext ctx, ShoppingListServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get(x.orderId);
        try {
            UserLoginDao orderItemDao = DaoRegistry.getDao(delegator, x.OrderItem, UserLoginDao.class);
            List<GenericValue> orderItems = orderItemDao.findByAnd(delegator, x.OrderItem, UtilMisc.toMap(x.orderId, orderId), null, false);
            for (GenericValue orderItem : orderItems) {
                String shoppingListId = orderItem.getString(x.shoppingListId);
                String shoppingListItemSeqId = orderItem.getString(x.shoppingListItemSeqId);
                if (UtilValidate.isNotEmpty(shoppingListId)) {
                    UserLoginDao shoppingListItemDao = DaoRegistry.getDao(delegator, x.ShoppingListItem, UserLoginDao.class);
                    GenericValue shoppingListItem = shoppingListItemDao.findOne(delegator, x.ShoppingListItem,
                            UtilMisc.toMap(x.shoppingListId, shoppingListId, x.shoppingListItemSeqId, shoppingListItemSeqId), false);
                    if (shoppingListItem != null) {
                        BigDecimal quantityPurchased = shoppingListItem.getBigDecimal(x.quantityPurchased);
                        BigDecimal orderQuantity = orderItem.getBigDecimal(x.quantity);
                        if (quantityPurchased != null) {
                            shoppingListItem.set(x.quantityPurchased, orderQuantity.add(quantityPurchased));
                        } else {
                            shoppingListItem.set(x.quantityPurchased, orderQuantity);
                        }
                        shoppingListItem.store();
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logInfo(x.updateShoppingListQuantitiesFromOrder_error + gee.getMessage(), MODULE);
        }
        return result;
    }

    public static Map<String, Object> autoDeleteAutoSaveShoppingList(DispatchContext dctx, ShoppingListServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        //set an upper limit for the number of pages to delete each run
        final int maxDeletePages = 50;

        int slProcessed = 0;
        int deleted = 0;
        int totPages = 0;
        int page = 1;
        int currentPage = page;

        //page size
        int limit;
        String limitStr = EntityUtilProperties.getPropertyValue(x.order, x.autosave_delete_viewsize, x._500, delegator);
        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_get_limit_init_it_to_500, MODULE);
            limit = 500;
        }
        int viewSize = limit;

        int maxDays = 0;
        String maxDaysStr = EntityUtilProperties.getPropertyValue(x.order, x.autosave_max_age, x._30, delegator);
        try {
            maxDays = Integer.parseInt(maxDaysStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_get_maxDays, MODULE);
        }
        if (maxDays <= 0) {
            return ServiceUtil.returnFailure(x.MaxDays_define_to + maxDays + x.nothing_todo);
        }

        EntityListIterator iterator = null;
        Transaction parent = null;
        Timestamp deleteAllBefore = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -maxDays);
        EntityCondition condDate = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(
                                EntityCondition.makeCondition(x.lastAdminModified, null),
                                EntityOperator.AND,
                                EntityCondition.makeCondition(x.lastUpdatedStamp, EntityOperator.LESS_THAN_EQUAL_TO, deleteAllBefore)),
                        EntityCondition.makeCondition(x.lastAdminModified, EntityOperator.LESS_THAN_EQUAL_TO, deleteAllBefore))),
                EntityOperator.OR);
        EntityCondition condParty = EntityCondition.makeConditionMap(x.partyId, null,
                        x.shoppingListTypeId, x.SLT_SPEC_PURP);
        EntityCondition cond = EntityCondition.makeCondition(condParty, condDate);

        try {
            UserLoginDao shoppingListDao = DaoRegistry.getDao(delegator, x.ShoppingList, UserLoginDao.class);
            EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                    EntityFindOptions.CONCUR_READ_ONLY, true);
            iterator = shoppingListDao.findIteratorByCondition(delegator, x.ShoppingList, cond, null, null, findOptions);

            PagedList<GenericValue> shoppingListsPaged = null;
            List<GenericValue> shoppingLists = null;

            //initial values: upper limits for pages
            if (iterator != null) {

                shoppingListsPaged = EntityUtil.getPagedList(iterator, page, viewSize);
                int pagedListSize = shoppingListsPaged.getSize();

                totPages = pagedListSize / viewSize;
                if ((pagedListSize % viewSize) > 0) {
                    totPages++;
                }

                //close the iterator
                iterator.close();
                iterator = null;
            }

            if (shoppingListsPaged != null) {

                //suspend the current transaction; use an  internal one to commit the deletion of each page
                if (TransactionUtil.getStatus() != TransactionUtil.STATUS_NO_TRANSACTION) {
                    parent = TransactionUtil.suspend();
                }

                shoppingLists = shoppingListsPaged.getData();

                while (page <= maxDeletePages && page <= totPages) {
                    boolean beganTx = false;
                    try {

                        // begin transaction for this page: set a timeout of 300 (default is 60)
                        beganTx = TransactionUtil.begin(300);
                        if (page > currentPage) {

                            //Retrieve another page
                            iterator = shoppingListDao.findIteratorByCondition(delegator, x.ShoppingList, cond, null, null, findOptions);

                            shoppingListsPaged = EntityUtil.getPagedList(iterator, page, viewSize);
                            shoppingLists = shoppingListsPaged.getData();

                            iterator.close();
                            iterator = null;
                        }

                        //processing shopping lists
                        for (GenericValue sl : shoppingLists) {

                            List<GenericValue> shoppingListItems = null;
                            try {
                                shoppingListItems = sl.getRelated(x.ShoppingListItem, null, null, false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e.getMessage(), MODULE);
                                TransactionUtil.rollback();
                                break;
                            }

                            for (GenericValue sli : shoppingListItems) {
                                try {
                                    dispatcher.runSync(x.removeShoppingListItem,
                                            UtilMisc.toMap(x.shoppingListId, sl.getString(x.shoppingListId),
                                                    x.shoppingListItemSeqId, sli.getString(x.shoppingListItemSeqId),
                                                    x.userLogin, userLogin));
                                } catch (GenericServiceException e) {
                                    Debug.logError(e.getMessage(), MODULE);
                                    TransactionUtil.rollback();
                                    break;
                                }
                            }
                            try {
                                dispatcher.runSync(x.removeShoppingList,
                                        UtilMisc.toMap(x.shoppingListId, sl.getString(x.shoppingListId),
                                                x.userLogin, userLogin));
                                deleted++;
                            } catch (GenericServiceException e) {
                                Debug.logError(e.getMessage(), MODULE);
                                TransactionUtil.rollback();
                                break;
                            }
                            slProcessed++;
                        }
                    } catch (GenericTransactionException gte) {
                        Debug.logError(gte.getMessage(), MODULE);
                        TransactionUtil.rollback();
                        break;
                    } finally {
                        //commit this page
                        if (beganTx) {
                            try {
                                TransactionUtil.commit(beganTx);
                            } catch (GenericTransactionException gte) {
                                Debug.logError(gte, x.Unable_to_commit_page + page, MODULE);
                                TransactionUtil.rollback();
                                break;
                            }
                        }
                    }
                    currentPage = page;
                    page++;

                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            if (iterator != null) {
                try {
                    iterator.close();
                } catch (GenericEntityException ex) {
                    Debug.logError(ex, x.Error_occured_in_closing_iterator, MODULE);
                }
            }

            try {
                TransactionUtil.rollback();
            } catch (GenericTransactionException ex) {
                Debug.logError(ex, x.Error_in_rolling_back_transaction, MODULE);
            }

            return ServiceUtil.returnError(e.getMessage());
        } finally {

            if (iterator != null) {
                try {
                    iterator.close();
                } catch (GenericEntityException ex) {
                    Debug.logError(ex, x.Error_occured_in_closing_iterator, MODULE);
                }
            }

            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }
        return ServiceUtil.returnSuccess(
                x.Total_shopping_list_processed + slProcessed + x.str_94718d75
                        + x.shopping_list_deleted + deleted + x.str_76d00394);
    }
}
