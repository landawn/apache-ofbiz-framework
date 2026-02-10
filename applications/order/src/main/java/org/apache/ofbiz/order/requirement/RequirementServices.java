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
package org.apache.ofbiz.order.requirement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
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
import org.apache.ofbiz.persistence.dao.*;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.RequirementServicesContext;
/**
 * Requirement Services
 */

public class RequirementServices {

    private static final String MODULE = RequirementServices.class.getName();
    private static final String RES_ERROR = x.OrderErrorUiLabels;

    public static Map<String, Object> getRequirementsForSupplier(DispatchContext ctx, RequirementServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        EntityCondition requirementConditions = (EntityCondition) context.get(x.requirementConditions);
        String partyId = (String) context.get(x.partyId);
        String unassignedRequirements = (String) context.get(x.unassignedRequirements);
        List<String> statusIds = UtilGenerics.cast(context.get(x.statusIds));
        //TODO currencyUomId still not used
        try {
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition(x.requirementTypeId, EntityOperator.EQUALS, x.PRODUCT_REQUIREMENT),
                    EntityUtil.getFilterByDateExpr());
            if (UtilValidate.isNotEmpty(statusIds)) {
                conditions.add(EntityCondition.makeCondition(x.statusId, EntityOperator.IN, statusIds));
            } else {
                conditions.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.REQ_APPROVED));
            }
            if (requirementConditions != null) conditions.add(requirementConditions);

            // we're either getting the requirements for a given supplier, unassigned requirements, or requirements for all suppliers
            if (UtilValidate.isNotEmpty(partyId)) {
                conditions.add(EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
                conditions.add(EntityCondition.makeCondition(x.roleTypeId, EntityOperator.EQUALS, x.SUPPLIER));
            } else if (UtilValidate.isNotEmpty(unassignedRequirements)) {
                conditions.add(EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, null));
            } else {
                conditions.add(EntityCondition.makeCondition(x.roleTypeId, EntityOperator.EQUALS, x.SUPPLIER));
            }

            List<GenericValue> requirementAndRoles = DaoRegistry.getDao(delegator, x.RequirementAndRole, RequirementAndRoleDao.class).findListByWhere(delegator, x.RequirementAndRole, conditions, null, UtilMisc.toList(x.partyId, x.requirementId), false);

            // maps to cache the associated suppliers and products data, so we don't do redundant DB and service requests
            Map<String, GenericValue> suppliers = new HashMap<>();
            Map<String, GenericValue> gids = new HashMap<>();
            Map<String, Map<String, Object>> inventories = new HashMap<>();
            Map<String, BigDecimal> productsSold = new HashMap<>();

            // to count quantity, running total, and distinct products in list
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal amountTotal = BigDecimal.ZERO;
            Set<String> products = new HashSet<>();

            // time period to count products ordered from, six months ago and the 1st of that month
            Timestamp timePeriodStart = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, -6);

            // join in fields with extra data about the suppliers and products
            List<Map<String, Object>> requirements = new LinkedList<>();
            for (GenericValue requirement : requirementAndRoles) {
                Map<String, Object> union = new HashMap<>();
                String productId = requirement.getString(x.productId);
                partyId = requirement.getString(x.partyId);
                String facilityId = requirement.getString(x.facilityId);
                BigDecimal requiredQuantity = requirement.getBigDecimal(x.quantity);

                // get an available supplier product, preferably the one with the smallest minimum quantity to order, followed by price
                String supplierKey = partyId + x.str_5e6f80a3 + productId;
                GenericValue supplierProduct = suppliers.get(supplierKey);
                if (supplierProduct == null) {
                    // TODO: it is possible to restrict to quantity > minimumOrderQuantity, but then the entire requirement must be skipped
                    EntityCondition supplierProductCond = EntityCondition.makeCondition(UtilMisc.toMap(x.partyId, partyId, x.productId, productId));
                    supplierProductCond = EntityCondition.makeCondition(supplierProductCond,
                            EntityUtil.getFilterByDateExpr(x.availableFromDate, x.availableThruDate));
                    supplierProduct = DaoRegistry.getDao(delegator, x.SupplierProduct, SupplierProductDao.class)
                            .findFirstByCondition(delegator, x.SupplierProduct, supplierProductCond, null,
                                    UtilMisc.toList(x.minimumOrderQuantity, x.lastPrice), false);
                    suppliers.put(supplierKey, supplierProduct);
                }

                // add our supplier product and cost of this line to the data
                if (supplierProduct != null) {
                    union.putAll(supplierProduct.getAllFields());
                    BigDecimal lastPrice = supplierProduct.getBigDecimal(x.lastPrice);
                    amountTotal = amountTotal.add(lastPrice.multiply(requiredQuantity));
                }

                // for good identification, get the UPCA type (UPC code)
                GenericValue gid = gids.get(productId);
                if (gid == null) {
                    gid = DaoRegistry.getDao(delegator, x.GoodIdentification, GoodIdentificationDao.class).findOneByWhere(delegator, x.GoodIdentification, UtilMisc.toMap(x.goodIdentificationTypeId, x.UPCA, x.productId,
                            requirement.get(x.productId)), null, null, false);
                    gids.put(productId, gid);
                }
                if (gid != null) union.put(x.idValue, gid.get(x.idValue));

                // the ATP and QOH quantities
                if (UtilValidate.isNotEmpty(facilityId)) {
                    String inventoryKey = facilityId + x.str_5e6f80a3 + productId;
                    Map<String, Object> inventory = inventories.get(inventoryKey);
                    if (inventory == null) {
                        inventory = dispatcher.runSync(x.getInventoryAvailableByFacility, UtilMisc.toMap(x.productId, productId, x.facilityId,
                                facilityId));
                        if (ServiceUtil.isError(inventory)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(inventory));
                        }
                        inventories.put(inventoryKey, inventory);
                    }
                    if (inventory != null) {
                        union.put(x.qoh, inventory.get(x.quantityOnHandTotal));
                        union.put(x.atp, inventory.get(x.availableToPromiseTotal));
                    }
                }

                // how many of the products were sold (note this is for a fixed time period across all product stores)
                BigDecimal sold = productsSold.get(productId);
                if (sold == null) {
                    EntityCondition prodConditions = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition(x.orderTypeId, EntityOperator.EQUALS, x.SALES_ORDER),
                            EntityCondition.makeCondition(x.orderStatusId, EntityOperator.NOT_IN, UtilMisc.toList(x.ORDER_REJECTED,
                                    x.ORDER_CANCELLED)),
                            EntityCondition.makeCondition(x.orderItemStatusId, EntityOperator.NOT_IN, UtilMisc.toList(x.ITEM_REJECTED,
                                    x.ITEM_CANCELLED)),
                            EntityCondition.makeCondition(x.orderDate, EntityOperator.GREATER_THAN_EQUAL_TO, timePeriodStart)), EntityOperator.AND);
                    GenericValue count = DaoRegistry.getDao(delegator, x.OrderItemQuantityReportGroupByProduct, OrderItemQuantityReportGroupByProductDao.class).findFirstByWhere(delegator, x.OrderItemQuantityReportGroupByProduct, prodConditions, UtilMisc.toList(x.quantityOrdered), null, false);
                    if (count != null) {
                        sold = count.getBigDecimal(x.quantityOrdered);
                        if (sold != null) productsSold.put(productId, sold);
                    }
                }
                if (sold != null) {
                    union.put(x.qtySold, sold);
                }

                // keep a running total of distinct products and quantity to order
                if (requirement.getBigDecimal(x.quantity) == null) requirement.put(x.quantity, BigDecimal.ONE); // default quantity = 1
                quantity = quantity.add(requiredQuantity);
                products.add(productId);

                // add all the requirement fields last, to overwrite any conflicting fields
                union.putAll(requirement.getAllFields());
                requirements.add(union);
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put(x.requirementsForSupplier, requirements);
            results.put(x.distinctProductCount, products.size());
            results.put(x.quantityTotal, quantity);
            results.put(x.amountTotal, amountTotal);
            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderServiceExceptionSeeLogs, locale));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderEntityExceptionSeeLogs, locale));
        }
    }

    // note that this service is designed to work only when a sales order status changes from CREATED -> APPROVED because HOLD -> APPROVED is too
    // complex
    public static Map<String, Object> createAutoRequirementsForOrder(DispatchContext ctx, RequirementServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String orderId = (String) context.get(x.orderId);
        try {
            GenericValue order = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class).findOneByWhere(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), null, null, false);
            GenericValue productStore = order.getRelatedOne(x.ProductStore, true);
            if (productStore == null) {
                Debug.logInfo(x.ProductStore_for_order_ID + orderId + x.not_found_requirements_not_created, MODULE);
                return ServiceUtil.returnSuccess();
            }
            List<GenericValue> orderItemAndShipGroups = DaoRegistry.getDao(delegator, x.OrderItemAndShipGroupAssoc, OrderItemAndShipGroupAssocDao.class).findListByWhere(delegator, x.OrderItemAndShipGroupAssoc, UtilMisc.toMap(x.orderId, orderId), UtilMisc.toList(x.orderId, x.shipGroupSeqId, x.orderItemSeqId), null, false, true);
            for (GenericValue orderItemAndShipGroup : orderItemAndShipGroups) {
                GenericValue item = DaoRegistry.getDao(delegator, x.OrderItem, OrderItemDao.class).findOneByWhere(delegator, x.OrderItem, UtilMisc.toMap(x.orderId, orderItemAndShipGroup.getString(x.orderId),
                        x.orderItemSeqId, orderItemAndShipGroup.getString(x.orderItemSeqId)), null, null, false);
                GenericValue product = item.getRelatedOne(x.Product, false);
                if (product == null) continue;
                if ((!x.PRODRQM_AUTO.equals(product.get(x.requirementMethodEnumId))
                        && !x.PRODRQM_AUTO.equals(productStore.get(x.requirementMethodEnumId)))
                        || (product.get(x.requirementMethodEnumId) == null
                        && !x.PRODRQM_AUTO.equals(productStore.get(x.requirementMethodEnumId)))) {
                    continue;
                }
                BigDecimal quantity = item.getBigDecimal(x.quantity);
                BigDecimal cancelQuantity = item.getBigDecimal(x.cancelQuantity);
                BigDecimal required = quantity.subtract(cancelQuantity == null ? BigDecimal.ZERO : cancelQuantity);
                if (required.compareTo(BigDecimal.ZERO) <= 0) continue;
                GenericValue orderItemShipGroup = DaoRegistry.getDao(delegator, x.OrderItemShipGroup, OrderItemShipGroupDao.class).findOneByWhere(delegator, x.OrderItemShipGroup, UtilMisc.toMap(x.orderId, orderId, x.shipGroupSeqId,
                        orderItemAndShipGroup.getString(x.shipGroupSeqId)), null, null, true);
                Map<String, Object> input = UtilMisc.toMap(x.userLogin, userLogin, x.facilityId, orderItemShipGroup.getString(x.facilityId),
                        x.productId, product.get(x.productId), x.quantity, required, x.requirementTypeId, x.PRODUCT_REQUIREMENT);
                Map<String, Object> results = dispatcher.runSync(x.createRequirement, input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                String requirementId = (String) results.get(x.requirementId);

                input = UtilMisc.toMap(x.userLogin, userLogin, x.orderId, order.get(x.orderId), x.orderItemSeqId, item.get(x.orderItemSeqId),
                        x.requirementId, requirementId, x.quantity, required);
                results = dispatcher.runSync(x.createOrderRequirementCommitment, input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    // note that this service is designed to work only when a sales order status changes from CREATED -> APPROVED because HOLD -> APPROVED is too
    // complex
    public static Map<String, Object> createATPRequirementsForOrder(DispatchContext ctx, RequirementServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        /*
         * The strategy in this service is to begin making requirements when the product falls below the
         * ProductFacility.minimumStock.  Because the minimumStock is an upper bound, the quantity to be required
         * is either that required to bring the ATP back up to the minimumStock level or the amount ordered,
         * whichever is less.
         * If there is a way to support reorderQuantity without losing the order item -> requirement association data,
         * then this service should be updated.
         * The result is that this service generates many small requirements when stock levels are low for a product,
         * which is perfectly fine since the system is capable of creating POs in bulk from aggregate requirements.
         * The only concern would be a UI to manage numerous requirements with ease, preferrably by aggregating
         * on productId.
         */
        String orderId = (String) context.get(x.orderId);
        try {
            GenericValue order = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class).findOneByWhere(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), null, null, false);
            GenericValue productStore = order.getRelatedOne(x.ProductStore, true);
            if (productStore == null) {
                Debug.logInfo(x.ProductStore_for_order_ID + orderId + x.not_found_ATP_requirements_not_created, MODULE);
                return ServiceUtil.returnSuccess();
            }
            String facilityId = productStore.getString(x.inventoryFacilityId);
            List<GenericValue> orderItems = order.getRelated(x.OrderItem, null, null, false);
            for (GenericValue item : orderItems) {
                GenericValue product = item.getRelatedOne(x.Product, false);
                if (product == null) {
                    continue;
                }

                if (!(x.PRODRQM_ATP.equals(product.get(x.requirementMethodEnumId))
                        || (x.PRODRQM_ATP.equals(productStore.get(x.requirementMethodEnumId)) && product.get(x.requirementMethodEnumId) == null))) {
                    continue;
                }

                BigDecimal quantity = item.getBigDecimal(x.quantity);
                BigDecimal cancelQuantity = item.getBigDecimal(x.cancelQuantity);
                BigDecimal ordered = quantity.subtract(cancelQuantity == null ? BigDecimal.ZERO : cancelQuantity);
                if (ordered.compareTo(BigDecimal.ZERO) <= 0) continue;

                // get the minimum stock for this facility (if not configured assume a minimum of zero, ie create requirements when it goes into
                // backorder)
                GenericValue productFacility = DaoRegistry.getDao(delegator, x.ProductFacility, ProductFacilityDao.class).findOneByWhere(delegator, x.ProductFacility, UtilMisc.toMap(x.facilityId, facilityId, x.productId,
                        product.get(x.productId)), null, null, false);
                BigDecimal minimumStock = BigDecimal.ZERO;
                if (productFacility != null && productFacility.get(x.minimumStock) != null) {
                    minimumStock = productFacility.getBigDecimal(x.minimumStock);
                }

                // get the facility ATP for product, which should be updated for this item's reservation
                Map<String, Object> results = dispatcher.runSync(x.getInventoryAvailableByFacility, UtilMisc.toMap(x.userLogin, userLogin,
                        x.productId, product.get(x.productId), x.facilityId, facilityId));
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                BigDecimal atp = ((BigDecimal) results.get(x.availableToPromiseTotal)); // safe since this is a required OUT param

                // count all current requirements for this product
                BigDecimal pendingRequirements = BigDecimal.ZERO;
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, product.get(x.productId)),
                        EntityCondition.makeCondition(x.requirementTypeId, EntityOperator.EQUALS, x.PRODUCT_REQUIREMENT),
                        EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.REQ_ORDERED),
                        EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.REQ_REJECTED)),
                        EntityOperator.AND);
                List<GenericValue> requirements = DaoRegistry.getDao(delegator, x.Requirement, RequirementDao.class).findListByWhere(delegator, x.Requirement, ecl, null, null, false);
                for (GenericValue requirement : requirements) {
                    pendingRequirements = pendingRequirements.add(requirement.get(x.quantity) == null ? BigDecimal.ZERO
                            : requirement.getBigDecimal(x.quantity));
                }

                // the minimum stock is an upper bound, therefore we either require up to the minimum stock or the input required quantity,
                // whichever is less
                BigDecimal shortfall = minimumStock.subtract(atp).subtract(pendingRequirements);
                BigDecimal required = ordered.compareTo(shortfall) < 0 ? ordered : shortfall;
                if (required.compareTo(BigDecimal.ZERO) <= 0) continue;

                Map<String, Object> input = UtilMisc.toMap(x.userLogin, userLogin, x.facilityId, facilityId, x.productId, product.get(x.productId),
                        x.quantity, required, x.requirementTypeId, x.PRODUCT_REQUIREMENT);
                results = dispatcher.runSync(x.createRequirement, input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                String requirementId = (String) results.get(x.requirementId);

                input = UtilMisc.toMap(x.userLogin, userLogin, x.orderId, order.get(x.orderId), x.orderItemSeqId, item.get(x.orderItemSeqId),
                        x.requirementId, requirementId, x.quantity, required);
                results = dispatcher.runSync(x.createOrderRequirementCommitment, input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateRequirementsToOrdered(DispatchContext ctx, RequirementServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderId = (String) context.get(x.orderId);
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        try {
            for (GenericValue orderItem : orh.getOrderItems()) {
                GenericValue orderRequirementCommitment = DaoRegistry.getDao(delegator, x.OrderRequirementCommitment, OrderRequirementCommitmentDao.class).findFirstByWhere(delegator, x.OrderRequirementCommitment, UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItem.getString(x.orderItemSeqId)), null, null, false);
                if (orderRequirementCommitment != null) {
                    String requirementId = orderRequirementCommitment.getString(x.requirementId);
                    /* Change status of requirement to ordered */
                    Map<String, Object> inputMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.requirementId, requirementId, x.statusId,
                            x.REQ_ORDERED, x.quantity, orderItem.getBigDecimal(x.quantity));
                    // TODO: check service result for an error return
                    Map<String, Object> results = dispatcher.runSync(x.updateRequirement, inputMap);
                    if (ServiceUtil.isError(results)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}

