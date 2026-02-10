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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

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
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.manufacturing.bom.BOMNode;
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
import org.apache.ofbiz.model.MrpServicesContext;
/**
 * Services for running MRP
 */
public class MrpServices {

    private static final String MODULE = MrpServices.class.getName();
    private static final String RESOURCE = x.ManufacturingUiLabels;

    public static Map<String, Object> initMrpEvents(DispatchContext ctx, MrpServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String facilityId = (String) context.get(x.facilityId);
        Integer defaultYearsOffset = (Integer) context.get(x.defaultYearsOffset);
        String mrpId = (String) context.get(x.mrpId);

        //Erases the old table for the moment and initializes it with the new orders,
        //Does not modify the old one now.

        List<GenericValue> listResult = null;
        try {
            listResult = DaoRegistry.getDao(delegator, x.MrpEvent, UserLoginDao.class).findByCondition(delegator, x.MrpEvent, null, null, null,
                    null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_findList_MrpEvent_null_null_null_null_false, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        if (listResult != null) {
            try {
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Error_removeAll_listResult_listResult + listResult, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventRemoveError, locale));
            }
        }

        // Proposed requirements are deleted
        List<GenericValue> listResultRoles = new LinkedList<>();
        try {
            listResult = DaoRegistry.getDao(delegator, x.Requirement, UserLoginDao.class).findByAnd(delegator, x.Requirement,
                    UtilMisc.toMap(x.requirementTypeId, x.PRODUCT_REQUIREMENT, x.facilityId, facilityId, x.statusId, x.REQ_PROPOSED), null,
                    false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        List<GenericValue> requirementStatus = new ArrayList<>();
        if (listResult != null) {
            try {
                for (GenericValue tmpRequirement : listResult) {
                    listResultRoles.addAll(tmpRequirement.getRelated(x.RequirementRole, null, null, false));
                    requirementStatus.addAll(tmpRequirement.getRelated(x.RequirementStatus, null, null, false));
                }
                delegator.removeAll(listResultRoles);
                delegator.removeAll(requirementStatus);
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventRemoveError, locale));
            }
        }
        try {
            listResult = DaoRegistry.getDao(delegator, x.Requirement, UserLoginDao.class).findByAnd(delegator, x.Requirement,
                    UtilMisc.toMap(x.requirementTypeId, x.INTERNAL_REQUIREMENT, x.facilityId, facilityId, x.statusId, x.REQ_PROPOSED), null,
                    false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        if (listResult != null) {
            try {
                for (GenericValue tempRequirement : listResult) {
                    requirementStatus.addAll(tempRequirement.getRelated(x.RequirementStatus, null, null, false));
                }
                delegator.removeAll(requirementStatus);
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventRemoveError, locale));
            }
        }

        Map<String, Object> parameters = null;
        List<GenericValue> resultList = null;
        // ----------------------------------------
        // Loads all the approved sales order items and purchase order items
        // ----------------------------------------
        // This is the default required date for orders without dates specified:
        // by convention it is a date far in the future of 100 years.
        Timestamp notAssignedDate = null;
        if (UtilValidate.isEmpty(defaultYearsOffset)) {
            notAssignedDate = now;
        } else {
            Calendar calendar = UtilDateTime.toCalendar(now);
            calendar.add(Calendar.YEAR, defaultYearsOffset);
            notAssignedDate = new Timestamp(calendar.getTimeInMillis());
        }
        try {
            resultList = DaoRegistry.getDao(delegator, x.OrderHeaderItemAndShipGroup, UserLoginDao.class).findByAnd(delegator,
                    x.OrderHeaderItemAndShipGroup,
                    UtilMisc.toMap(x.orderTypeId, x.SALES_ORDER, x.oiStatusId, x.ITEM_APPROVED, x.facilityId, facilityId),
                    UtilMisc.toList(x.orderId), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId = genericResult.getString(x.productId);
            BigDecimal reservedQuantity = genericResult.getBigDecimal(x.reservedQuantity);
            BigDecimal shipGroupQuantity = genericResult.getBigDecimal(x.quantity);
            BigDecimal cancelledQuantity = genericResult.getBigDecimal(x.cancelQuantity);
            BigDecimal eventQuantityTmp = BigDecimal.ZERO;

            if (UtilValidate.isNotEmpty(reservedQuantity)) {
                eventQuantityTmp = reservedQuantity.negate();
            } else {
                if (UtilValidate.isNotEmpty(cancelledQuantity)) {
                    shipGroupQuantity = shipGroupQuantity.subtract(cancelledQuantity);
                }
                eventQuantityTmp = shipGroupQuantity.negate();
            }

            if (eventQuantityTmp.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // This is the order in which order dates are considered:
            //   OrderItemShipGroup.shipByDate
            //   OrderItemShipGroup.shipAfterDate
            //   OrderItem.shipBeforeDate
            //   OrderItem.shipAfterDate
            //   OrderItem.estimatedDeliveryDate
            Timestamp requiredByDate = genericResult.getTimestamp(x.shipByDate);
            if (UtilValidate.isEmpty(requiredByDate)) {
                requiredByDate = genericResult.getTimestamp(x.shipAfterDate);
                if (UtilValidate.isEmpty(requiredByDate)) {
                    requiredByDate = genericResult.getTimestamp(x.oiShipBeforeDate);
                    if (UtilValidate.isEmpty(requiredByDate)) {
                        requiredByDate = genericResult.getTimestamp(x.oiShipAfterDate);
                        if (UtilValidate.isEmpty(requiredByDate)) {
                            requiredByDate = genericResult.getTimestamp(x.oiEstimatedDeliveryDate);
                            if (requiredByDate == null) {
                                requiredByDate = notAssignedDate;
                            }
                        }
                    }
                }
            }
            parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, requiredByDate, x.mrpEventTypeId, x.SALES_ORDER_SHIP);
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null,
                        genericResult.getString(x.orderId) + x.str_3bc15c8a + genericResult.getString(x.orderItemSeqId), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                        x.mrpEventTypeId, x.SALES_ORDER_SHIP), locale));
            }
        }
        // ----------------------------------------
        // Loads all the approved product requirements (po requirements)
        // ----------------------------------------
        try {
            resultList = DaoRegistry.getDao(delegator, x.Requirement, UserLoginDao.class).findByAnd(delegator, x.Requirement,
                    UtilMisc.toMap(x.requirementTypeId, x.PRODUCT_REQUIREMENT, x.statusId, x.REQ_APPROVED, x.facilityId, facilityId), null,
                    false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId = genericResult.getString(x.productId);
            BigDecimal eventQuantityTmp = genericResult.getBigDecimal(x.quantity);
            if (productId == null || eventQuantityTmp == null) {
                continue;
            }
            Timestamp estimatedShipDate = genericResult.getTimestamp(x.requiredByDate);
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }

            parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, estimatedShipDate, x.mrpEventTypeId, x.PROD_REQ_RECP);
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString(x.requirementId),
                        false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                        x.mrpEventTypeId, x.PROD_REQ_RECP), locale));
            }
        }

        // ----------------------------------------
        // Loads all the approved purchase order items
        // ----------------------------------------
        String orderId = null;
        GenericValue orderDeliverySchedule = null;
        try {
            List<GenericValue> facilityContactMechs = DaoRegistry.getDao(delegator, x.FacilityContactMech, UserLoginDao.class).findByAnd(
                    delegator, x.FacilityContactMech, UtilMisc.toMap(x.facilityId, facilityId), null, false);
            facilityContactMechs = EntityUtil.filterByDate(facilityContactMechs);
            List<String> facilityContactMechIds = EntityUtil.getFieldListFromEntityList(facilityContactMechs, x.contactMechId, true);

            resultList = DaoRegistry.getDao(delegator, x.OrderHeaderItemAndShipGroup, UserLoginDao.class).findByCondition(delegator,
                    x.OrderHeaderItemAndShipGroup, EntityCondition.makeCondition(
                            EntityCondition.makeCondition(x.orderTypeId, EntityOperator.EQUALS, x.PURCHASE_ORDER),
                            EntityCondition.makeCondition(x.oiStatusId, EntityOperator.EQUALS, x.ITEM_APPROVED),
                            EntityCondition.makeCondition(x.contactMechId, EntityOperator.IN, facilityContactMechIds)),
                    UtilMisc.toList(x.orderId, x.orderItemSeqId, x.productId, x.quantity, x.cancelQuantity, x.oiEstimatedDeliveryDate),
                    UtilMisc.toList(x.orderDate), null, false);

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        for (GenericValue genericResult : resultList) {
            try {
                String newOrderId = genericResult.getString(x.orderId);
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    orderDeliverySchedule = DaoRegistry.getDao(delegator, x.OrderDeliverySchedule, UserLoginDao.class).findOne(delegator,
                            x.OrderDeliverySchedule, UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, x.NA), false);
                }
                String productId = genericResult.getString(x.productId);
                BigDecimal shipGroupQuantity = genericResult.getBigDecimal(x.quantity);
                BigDecimal cancelledQuantity = genericResult.getBigDecimal(x.cancelQuantity);
                if (UtilValidate.isEmpty(shipGroupQuantity)) {
                    shipGroupQuantity = BigDecimal.ZERO;
                }
                if (UtilValidate.isNotEmpty(cancelledQuantity)) {
                    shipGroupQuantity = shipGroupQuantity.subtract(cancelledQuantity);
                }

                try {
                    List<GenericValue> shipmentReceipts = DaoRegistry.getDao(delegator, x.ShipmentReceipt, UserLoginDao.class).findByCondition(
                            delegator, x.ShipmentReceipt,
                            EntityCondition.makeCondition(UtilMisc.toMap(x.orderId, genericResult.getString(x.orderId), x.orderItemSeqId,
                                    genericResult.getString(x.orderItemSeqId))),
                            UtilMisc.toList(x.quantityAccepted, x.quantityRejected), null, null, false);
                    for (GenericValue shipmentReceipt : shipmentReceipts) {
                        shipGroupQuantity = shipGroupQuantity.subtract(shipmentReceipt.getBigDecimal(x.quantityAccepted));
                        shipGroupQuantity = shipGroupQuantity.subtract(shipmentReceipt.getBigDecimal(x.quantityRejected));
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                GenericValue orderItemDeliverySchedule = null;
                orderItemDeliverySchedule = DaoRegistry.getDao(delegator, x.OrderDeliverySchedule, UserLoginDao.class).findOne(delegator,
                        x.OrderDeliverySchedule,
                        UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, genericResult.getString(x.orderItemSeqId)), false);
                Timestamp estimatedShipDate = null;
                if (orderItemDeliverySchedule != null && orderItemDeliverySchedule.get(x.estimatedReadyDate) != null) {
                    estimatedShipDate = orderItemDeliverySchedule.getTimestamp(x.estimatedReadyDate);
                } else if (orderDeliverySchedule != null && orderDeliverySchedule.get(x.estimatedReadyDate) != null) {
                    estimatedShipDate = orderDeliverySchedule.getTimestamp(x.estimatedReadyDate);
                } else {
                    estimatedShipDate = genericResult.getTimestamp(x.oiEstimatedDeliveryDate);
                }
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, estimatedShipDate, x.mrpEventTypeId,
                        x.PUR_ORDER_RECP);
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, shipGroupQuantity, null,
                        genericResult.getString(x.orderId) + x.str_3bc15c8a + genericResult.getString(x.orderItemSeqId), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                        x.mrpEventTypeId, x.PUR_ORDER_RECP), locale));
            }
        }

        // ----------------------------------------
        // PRODUCTION Run: components
        // ----------------------------------------
        try {
            resultList = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, UserLoginDao.class).findByAnd(delegator, x.WorkEffortAndGoods,
                    UtilMisc.toMap(x.workEffortGoodStdTypeId, x.PRUNT_PROD_NEEDED, x.statusId, x.WEGS_CREATED, x.facilityId, facilityId), null,
                    false);
            for (GenericValue genericResult : resultList) {
                if (x.PRUN_CLOSED.equals(genericResult.getString(x.currentStatusId))
                        || x.PRUN_COMPLETED.equals(genericResult.getString(x.currentStatusId))
                        || x.PRUN_CANCELLED.equals(genericResult.getString(x.currentStatusId))) {
                    continue;
                }
                String productId = genericResult.getString(x.productId);
                // get the inventory already consumed
                BigDecimal consumedInventoryTotal = BigDecimal.ZERO;
                List<GenericValue> consumedInventoryItems = DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryAssign, UserLoginDao.class)
                        .findByAnd(delegator, x.WorkEffortAndInventoryAssign,
                                UtilMisc.toMap(x.workEffortId, genericResult.get(x.workEffortId), x.productId, productId), null, false);
                for (GenericValue consumedInventoryItem : consumedInventoryItems) {
                    consumedInventoryTotal = consumedInventoryTotal.add(consumedInventoryItem.getBigDecimal(x.quantity));
                }
                BigDecimal eventQuantityTmp = consumedInventoryTotal.subtract(genericResult.getBigDecimal(x.estimatedQuantity));
                Timestamp estimatedShipDate = genericResult.getTimestamp(x.estimatedStartDate);
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, estimatedShipDate, x.mrpEventTypeId,
                        x.MANUF_ORDER_REQ);
                String eventName = (UtilValidate.isEmpty(genericResult.getString(x.workEffortParentId)) ? genericResult.getString(x.workEffortId)
                        : genericResult.getString(x.workEffortParentId) + x.str_3bc15c8a + genericResult.getString(x.workEffortId));
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, eventName, false, delegator);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                    x.mrpEventTypeId, x.MANUF_ORDER_REQ), locale) + x.str_b858cb28 + e.getMessage());
        }

        // ----------------------------------------
        // PRODUCTION Run: product produced
        // ----------------------------------------
        try {
            resultList = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, UserLoginDao.class).findByAnd(delegator, x.WorkEffortAndGoods,
                    UtilMisc.toMap(x.workEffortGoodStdTypeId, x.PRUN_PROD_DELIV, x.statusId, x.WEGS_CREATED, x.workEffortTypeId,
                            x.PROD_ORDER_HEADER, x.facilityId, facilityId),
                    null, false);
            for (GenericValue genericResult : resultList) {
                if (x.PRUN_CLOSED.equals(genericResult.getString(x.currentStatusId))
                        || x.PRUN_COMPLETED.equals(genericResult.getString(x.currentStatusId))
                        || x.PRUN_CANCELLED.equals(genericResult.getString(x.currentStatusId))) {
                    continue;
                }
                BigDecimal qtyToProduce = genericResult.getBigDecimal(x.quantityToProduce);
                if (qtyToProduce == null) {
                    qtyToProduce = BigDecimal.ZERO;
                }
                BigDecimal qtyProduced = genericResult.getBigDecimal(x.quantityProduced);
                if (qtyProduced == null) {
                    qtyProduced = BigDecimal.ZERO;
                }
                if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                    continue;
                }
                BigDecimal qtyDiff = qtyToProduce.subtract(qtyProduced);
                String productId = genericResult.getString(x.productId);
                BigDecimal eventQuantityTmp = qtyDiff;
                Timestamp estimatedShipDate = genericResult.getTimestamp(x.estimatedCompletionDate);
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, estimatedShipDate, x.mrpEventTypeId,
                        x.MANUF_ORDER_RECP);
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString(x.workEffortId),
                        false, delegator);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                    x.mrpEventTypeId, x.MANUF_ORDER_RECP), locale) + x.str_b858cb28 + e.getMessage());
        }

        // ----------------------------------------
        // Products without upcoming events but that are already under minimum quantity in warehouse
        // ----------------------------------------
        try {
            resultList = DaoRegistry.getDao(delegator, x.ProductFacility, UserLoginDao.class).findByAnd(delegator, x.ProductFacility,
                    UtilMisc.toMap(x.facilityId, facilityId), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_retrieve_ProductFacility_records, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindProductFacility, locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId = genericResult.getString(x.productId);
            BigDecimal minimumStock = genericResult.getBigDecimal(x.minimumStock);
            if (minimumStock == null) {
                minimumStock = BigDecimal.ZERO;
            }
            try {
                long numOfEvents = DaoRegistry.getDao(delegator, x.MrpEvent, UserLoginDao.class).countByCondition(delegator, x.MrpEvent,
                        EntityCondition.makeCondition(UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId)), null, null);
                if (numOfEvents > 0) {
                    continue;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Unable_to_count_MrpEvent_records, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotCountRecords, locale));
            }
            BigDecimal qoh = findProductMrpQoh(mrpId, productId, facilityId, dispatcher, delegator);
            if (qoh.compareTo(minimumStock) >= 0) {
                continue;
            }
            parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, now, x.mrpEventTypeId, x.REQUIRED_MRP);
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, BigDecimal.ZERO, null, null, false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing, UtilMisc.toMap(
                        x.mrpEventTypeId, x.REQUIRED_MRP), locale));
            }
        }

        // ----------------------------------------
        // SALES FORECASTS
        // ----------------------------------------
        GenericValue facility = null;
        try {
            facility = DaoRegistry.getDao(delegator, x.Facility, UserLoginDao.class).findOne(delegator, x.Facility,
                    UtilMisc.toMap(x.facilityId, facilityId), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventFindError, locale));
        }
        String partyId = (String) facility.get(x.ownerPartyId);
        try {
            resultList = DaoRegistry.getDao(delegator, x.SalesForecast, UserLoginDao.class).findByAnd(delegator, x.SalesForecast,
                    UtilMisc.toMap(x.organizationPartyId, partyId), null, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindSalesForecasts, locale));
        }
        for (GenericValue genericResult : resultList) {
            String customTimePeriodId = genericResult.getString(x.customTimePeriodId);
            GenericValue customTimePeriod = null;
            try {
                customTimePeriod = DaoRegistry.getDao(delegator, x.CustomTimePeriod, UserLoginDao.class).findOne(delegator, x.CustomTimePeriod,
                        UtilMisc.toMap(x.customTimePeriodId, customTimePeriodId), false);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindCustomTimePeriod, locale));
            }
            if (customTimePeriod != null) {
                if (UtilValidate.isNotEmpty(customTimePeriod.getTimestamp(x.thruDate))
                        && customTimePeriod.getTimestamp(x.thruDate).before(UtilDateTime.nowTimestamp())) {
                    continue;
                } else {
                    List<GenericValue> salesForecastDetails = null;
                    try {
                        salesForecastDetails = DaoRegistry.getDao(delegator, x.SalesForecastDetail, UserLoginDao.class).findByAnd(delegator,
                                x.SalesForecastDetail, UtilMisc.toMap(x.salesForecastId, genericResult.get(x.salesForecastId)), null, false);
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindSalesForecastDetails, locale));
                    }
                    for (GenericValue sfd : salesForecastDetails) {
                        String productId = sfd.getString(x.productId);
                        BigDecimal eventQuantityTmp = sfd.getBigDecimal(x.quantity);
                        if (productId == null || eventQuantityTmp == null) {
                            continue;
                        }
                        eventQuantityTmp = eventQuantityTmp.negate();
                        parameters = UtilMisc.toMap(x.mrpId, mrpId, x.productId, productId, x.eventDate, customTimePeriod.getTimestamp(x.fromDate),
                                x.mrpEventTypeId, x.SALES_FORECAST);
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, sfd.getString(
                                    x.salesForecastDetailId), false, delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpEventProblemInitializing,
                                    UtilMisc.toMap(x.mrpEventTypeId, x.SALES_FORECAST), locale));
                        }
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo(x.return_from_initMrpEvent, MODULE);
        return result;
    }

    /**
     * Find the quantity on hand of products for MRP.
     * <ul>
     * <li>PreConditions : none</li>
     * <li>Result : We get the quantity of product available in the stocks.</li>
     * </ul>
     * @param product the product for which the Quantity Available is required
     * @return the sum of all the totalAvailableToPromise of the inventoryItem related to the product, if the related facility is Mrp available
     * (not yet implemented!!)
     */
    public static BigDecimal findProductMrpQoh(String mrpId, GenericValue product, String facilityId, LocalDispatcher dispatcher,
                                               Delegator delegator) {
        return findProductMrpQoh(mrpId, product.getString(x.productId), facilityId, dispatcher, delegator);
    }

    public static BigDecimal findProductMrpQoh(String mrpId, String productId, String facilityId, LocalDispatcher dispatcher, Delegator delegator) {
        Map<String, Object> resultMap = null;
        try {
            if (facilityId == null) {
                resultMap = dispatcher.runSync(x.getProductInventoryAvailable, UtilMisc.toMap(x.productId, productId));
            } else {
                resultMap = dispatcher.runSync(x.getInventoryAvailableByFacility, UtilMisc.toMap(x.productId, productId, x.facilityId, facilityId));
            }
            if (ServiceUtil.isError(resultMap)) {
                String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                Debug.logError(errorMessage, MODULE);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_calling_getProductInventoryAvailableByFacility_service, MODULE);
            logMrpError(mrpId, productId, x.Unable_to_count_inventory, delegator);
            return BigDecimal.ZERO;
        }
        return ((BigDecimal) resultMap.get(x.quantityOnHandTotal));
    }

    public static void logMrpError(String mrpId, String productId, String errorMessage, Delegator delegator) {
        logMrpError(mrpId, productId, UtilDateTime.nowTimestamp(), errorMessage, delegator);
    }

    public static void logMrpError(String mrpId, String productId, Timestamp eventDate, String errorMessage, Delegator delegator) {
        try {
            if (UtilValidate.isNotEmpty(productId) && UtilValidate.isNotEmpty(errorMessage)) {
                GenericValue inventoryEventError = delegator.makeValue(x.MrpEvent, UtilMisc.toMap(x.productId, productId,
                        x.mrpId, mrpId,
                        x.eventDate, eventDate,
                        x.mrpEventTypeId, x.ERROR,
                        x.eventName, errorMessage));
                delegator.createOrStore(inventoryEventError);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_calling_logMrpError_for_productId + productId + x.and_errorMessage + errorMessage + x.str_4ff447b8, MODULE);
        }
    }

    /**
     * Process the bill of material (bom) of the product  to insert components in the MrpEvent table.
     * Before inserting in the entity, test if there is the record already existing to add quantity rather to create a new one.
     * @param mrpId                the mrp id
     * @param product              GenericValue oject of the product
     * @param eventQuantity        the product quantity needed
     * @param startDate            the startDate of the productionRun which will used to produce the product
     * @param routingTaskStartDate Map with all the routingTask as keys and startDate of each of them
     * @param listComponent        a List with all the components
     */

    public static void processBomComponent(String mrpId, GenericValue product, BigDecimal eventQuantity, Timestamp startDate,
                                           Map<String, Object> routingTaskStartDate, List<BOMNode> listComponent) {
        // TODO : change the return type to boolean to be able to test if all is ok or if it have had a exception
        Delegator delegator = product.getDelegator();

        if (UtilValidate.isNotEmpty(listComponent)) {
            for (BOMNode node : listComponent) {
                GenericValue productComponent = node.getProductAssoc();
                // read the startDate for the component
                String routingTask = node.getProductAssoc().getString(x.routingWorkEffortId);
                Timestamp eventDate = (routingTask == null || !routingTaskStartDate.containsKey(routingTask)) ? startDate
                        : (Timestamp) routingTaskStartDate.get(routingTask);
                // if the components is valid at the event Date create the Mrp requirement in the M entity
                if (EntityUtil.isValueActive(productComponent, eventDate)) {
                    Map<String, Object> parameters = UtilMisc.<String, Object>toMap(x.productId, node.getProduct().getString(x.productId));
                    parameters.put(x.mrpId, mrpId);
                    parameters.put(x.eventDate, eventDate);
                    parameters.put(x.mrpEventTypeId, x.MRP_REQUIREMENT);
                    BigDecimal componentEventQuantity = node.getQuantity();
                    try {
                        InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, componentEventQuantity.negate(), null, product.get(
                                x.productId) + x.str_ceca32e9 + eventDate, false, delegator);
                    } catch (GenericEntityException e) {
                        Debug.logError(x.Error_findOne_MrpEvent_parameters_aaa5dd12 + parameters + x.str_e6a9fc04 + e.getMessage(), MODULE);
                        logMrpError(mrpId, node.getProduct().getString(x.productId), x.Unable_to_create_event_processBomComponent, delegator);
                    }
                }
            }
        }
    }

    /**
     * Launch the MRP.
     * <ul>
     * <li>PreConditions : none</li>
     * <li>Result : The date when we must order or begin to build the products and subproducts we need are calculated</li>
     * <li>INPUT : parameters to get from the context: <ul><li>String mrpName</li></ul></li>
     * <li>OUTPUT : Result to put in the map : <ul><li>none</li></ul></li>
     * </ul>
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId routingId, quantity, startDate.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> executeMrp(DispatchContext ctx, MrpServicesContext context) {
        Debug.logInfo(x.executeMrp_called, MODULE);
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String mrpName = (String) context.get(x.mrpName);
        Integer defaultYearsOffset = (Integer) context.get(x.defaultYearsOffset);
        String facilityGroupId = (String) context.get(x.facilityGroupId);
        String facilityId = (String) context.get(x.facilityId);
        String manufacturingFacilityId = null;
        if (UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(facilityGroupId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpFacilityNotAvailable, locale));
        }
        if (UtilValidate.isEmpty(facilityId)) {
            try {
                GenericValue facilityGroup = DaoRegistry.getDao(delegator, x.FacilityGroup, UserLoginDao.class).findOne(delegator, x.FacilityGroup,
                        UtilMisc.toMap(x.facilityGroupId, facilityGroupId), false);
                if (UtilValidate.isEmpty(facilityGroup)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpFacilityGroupIsNotValid, UtilMisc.toMap(
                            x.facilityGroupId, facilityGroupId), locale));
                }
                List<GenericValue> facilities = facilityGroup.getRelated(x.FacilityGroupMember, null, UtilMisc.toList(x.sequenceNum), false);
                if (UtilValidate.isEmpty(facilities)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpFacilityGroupIsNotAssociatedToFacility,
                            UtilMisc.toMap(x.facilityGroupId, facilityGroupId), locale));
                }
                for (GenericValue facilityMember : facilities) {
                    GenericValue facility = facilityMember.getRelatedOne(x.Facility, false);
                    if (x.WAREHOUSE.equals(facility.getString(x.facilityTypeId)) && UtilValidate.isEmpty(facilityId)) {
                        facilityId = facility.getString(x.facilityId);
                    }
                    if (x.PLANT.equals(facility.getString(x.facilityTypeId)) && UtilValidate.isEmpty(manufacturingFacilityId)) {
                        manufacturingFacilityId = facility.getString(x.facilityId);
                    }
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpFacilityGroupCannotBeLoad, UtilMisc.toMap(
                        x.errorString, e.getMessage()), locale));
            }
        } else {
            manufacturingFacilityId = facilityId;
        }

        if (UtilValidate.isEmpty(facilityId) || UtilValidate.isEmpty(manufacturingFacilityId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpFacilityOrManufacturingFacilityNotAvailable,
                    locale));
        }

        int bomLevelWithNoEvent = 0;
        BigDecimal stockTmp = BigDecimal.ZERO;
        String oldProductId = null;
        String productId = null;
        GenericValue product = null;
        GenericValue productFacility = null;
        BigDecimal eventQuantity = BigDecimal.ZERO;
        Timestamp eventDate = null;
        BigDecimal reorderQuantity = BigDecimal.ZERO;
        BigDecimal minimumStock = BigDecimal.ZERO;
        int daysToShip = 0;
        List<BOMNode> components = null;
        boolean isBuilt = false;
        GenericValue routing = null;

        String mrpId = delegator.getNextSeqId(x.MrpEvent);

        Map<String, Object> result = null;
        Map<String, Object> parameters = null;
        List<GenericValue> listInventoryEventForMRP = null;
        ListIterator<GenericValue> iteratorListInventoryEventForMRP = null;

        // Initialization of the MrpEvent table, This table will contain the products we want to buy or build.
        parameters = UtilMisc.<String, Object>toMap(x.mrpId, mrpId, x.reInitialize, Boolean.TRUE, x.defaultYearsOffset, defaultYearsOffset,
                x.userLogin, userLogin);
        parameters.put(x.facilityId, facilityId);
        parameters.put(x.manufacturingFacilityId, manufacturingFacilityId);
        try {
            result = dispatcher.runSync(x.initMrpEvents, parameters);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpErrorRunningInitMrpEvents, UtilMisc.toMap(
                    x.errorString, e.getMessage()), locale));
        }
        long bomLevel = 0;
        do {
            // Find all products in MrpEventView, ordered by bom and eventDate
            EntityCondition filterByConditions = null;
            if (bomLevel == 0) {
                filterByConditions = EntityCondition.makeCondition(EntityCondition.makeCondition(x.billOfMaterialLevel, EntityOperator.EQUALS, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(x.billOfMaterialLevel, EntityOperator.EQUALS, bomLevel));
            } else {
                filterByConditions = EntityCondition.makeCondition(x.billOfMaterialLevel, EntityOperator.EQUALS, bomLevel);
            }
            try {
                listInventoryEventForMRP = DaoRegistry.getDao(delegator, x.MrpEventView, UserLoginDao.class).findByCondition(delegator,
                        x.MrpEventView, filterByConditions, null, UtilMisc.toList(x.productId, x.eventDate), null, false);
            } catch (GenericEntityException e) {
                Long bomLevelToString = bomLevel;
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpErrorForBomLevel, UtilMisc.toMap(x.bomLevel,
                        bomLevelToString.toString(), x.errorString, e.getMessage()), locale));
            }

            if (UtilValidate.isNotEmpty(listInventoryEventForMRP)) {
                bomLevelWithNoEvent = 0;

                oldProductId = x.emptyString;
                int eventCount = 0;
                for (GenericValue inventoryEventForMRP : listInventoryEventForMRP) {
                    eventCount++;

                    productId = inventoryEventForMRP.getString(x.productId);
                    boolean isLastEvent = (eventCount == listInventoryEventForMRP.size()
                            || !productId.equals(listInventoryEventForMRP.get(eventCount).getString(x.productId)));
                    eventQuantity = inventoryEventForMRP.getBigDecimal(x.quantity);

                    if (!productId.equals(oldProductId)) {
                        BigDecimal positiveEventQuantity = eventQuantity.compareTo(BigDecimal.ZERO) > 0 ? eventQuantity : eventQuantity.negate();
                        // It's a new product, so it's necessary to  read the MrpQoh
                        try {
                            product = inventoryEventForMRP.getRelatedOne(x.Product, true);
                            productFacility = EntityUtil.getFirst(product.getRelated(x.ProductFacility, UtilMisc.toMap(x.facilityId, facilityId),
                                    null, true));
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindProductForEvent, locale));
                        }
                        stockTmp = findProductMrpQoh(mrpId, product, facilityId, dispatcher, delegator);
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(UtilMisc.<String, Object>toMap(x.mrpId, mrpId,
                                    x.productId, product.getString(x.productId),
                                    x.mrpEventTypeId, x.INITIAL_QOH, x.eventDate, now),
                                    stockTmp, facilityId, null, false, delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCreateOrUpdateEvent,
                                    UtilMisc.toMap(x.parameters, parameters), locale));
                        }
                        // days to ship is only relevant for sales order to plan for preparatory days to ship.  Otherwise MRP will push event dates
                        // for manufacturing parts
                        // as well and cause problems
                        daysToShip = 0;
                        if (productFacility != null) {
                            reorderQuantity = (productFacility.getBigDecimal(x.reorderQuantity) != null ? productFacility.getBigDecimal(
                                    x.reorderQuantity) : BigDecimal.ONE.negate());
                            minimumStock = (productFacility.getBigDecimal(x.minimumStock) != null ? productFacility.getBigDecimal(x.minimumStock)
                                    : BigDecimal.ZERO);
                            if (x.SALES_ORDER_SHIP.equals(inventoryEventForMRP.getString(x.mrpEventTypeId))) {
                                daysToShip = (productFacility.getLong(x.daysToShip) != null ? productFacility.getLong(x.daysToShip).intValue() : 0);
                            }
                        } else {
                            minimumStock = BigDecimal.ZERO;
                            reorderQuantity = BigDecimal.ONE.negate();
                        }
                        // -----------------------------------------------------
                        // The components are also loaded thru the configurator
                        Map<String, Object> serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync(x.getManufacturingComponents, UtilMisc.<String, Object>toMap(x.productId,
                                    product.getString(x.productId), x.quantity, positiveEventQuantity, x.excludeWIPs, Boolean.FALSE, x.userLogin,
                                    userLogin));
                            if (ServiceUtil.isError(serviceResponse)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResponse));
                            }
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpErrorExplodingProduct,
                                    UtilMisc.toMap(x.productId, product.getString(x.productId)), locale));
                        }
                        components = UtilGenerics.cast(serviceResponse.get(x.components));
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = (components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        oldProductId = productId;
                    }

                    stockTmp = stockTmp.add(eventQuantity);
                    if (stockTmp.compareTo(minimumStock) < 0 && (eventQuantity.compareTo(BigDecimal.ZERO) < 0 || isLastEvent)) { // No need to
                        // create a supply event/requirement if the current event is not a demand and there are other events to process
                        BigDecimal qtyToStock = minimumStock.subtract(stockTmp);
                        //need to buy or build the product as we have not enough stock
                        eventDate = inventoryEventForMRP.getTimestamp(x.eventDate);
                        // to be just before the requirement
                        eventDate.setTime(eventDate.getTime() - 1);
                        ProposedOrder proposedOrder = new ProposedOrder(product, facilityId, manufacturingFacilityId, isBuilt, eventDate, qtyToStock);
                        proposedOrder.setMrpName(mrpName);
                        // calculate the ProposedOrder quantity and update the quantity object property.
                        proposedOrder.calculateQuantityToSupply(reorderQuantity, minimumStock, iteratorListInventoryEventForMRP);

                        // -----------------------------------------------------
                        // The components are also loaded thru the configurator
                        Map<String, Object> serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync(x.getManufacturingComponents, UtilMisc.<String, Object>toMap(x.productId,
                                    product.getString(x.productId), x.quantity, proposedOrder.getQuantity(), x.excludeWIPs, Boolean.FALSE,
                                    x.userLogin, userLogin));
                            if (ServiceUtil.isError(serviceResponse)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResponse));
                            }
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpErrorExplodingProduct,
                                    UtilMisc.toMap(x.productId, product.getString(x.productId)), locale));
                        }
                        components = UtilGenerics.cast(serviceResponse.get(x.components));
                        String routingId = (String) serviceResponse.get(x.workEffortId);
                        if (routingId != null) {
                            try {
                                routing = DaoRegistry.getDao(delegator, x.WorkEffort, UserLoginDao.class).findOne(delegator, x.WorkEffort,
                                        UtilMisc.toMap(x.workEffortId, routingId), false);
                            } catch (GenericEntityException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCannotFindProductForEvent,
                                        locale));
                            }
                        } else {
                            routing = null;
                        }
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = (components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        // calculate the ProposedOrder requirementStartDate and update the requirementStartDate object property.
                        Map<String, Object> routingTaskStartDate = proposedOrder.calculateStartDate(daysToShip, routing, delegator, dispatcher,
                                userLogin);
                        if (isBuilt) {
                            // process the product components
                            processBomComponent(mrpId, product, proposedOrder.getQuantity(), proposedOrder.getRequirementStartDate(),
                                    routingTaskStartDate, components);
                        }
                        // create the  ProposedOrder (only if the product is warehouse managed), and the MrpEvent associated
                        String requirementId = null;
                        if (productFacility != null) {
                            requirementId = proposedOrder.create(ctx, userLogin);
                        }
                        if (UtilValidate.isEmpty(productFacility) && !isBuilt) {
                            logMrpError(mrpId, productId, now, x.No_ProductFacility_record_for + facilityId + x.no_requirement_created,
                                    delegator);
                        }
                        String eventName = null;
                        if (UtilValidate.isNotEmpty(requirementId)) {
                            eventName = x.str_df58248c + requirementId + x.str_d21048c5 + proposedOrder.getRequirementStartDate() + x.str_83626d31;
                        }
                        Map<String, Object> eventMap = UtilMisc.<String, Object>toMap(x.productId, product.getString(x.productId),
                                x.mrpId, mrpId,
                                x.eventDate, eventDate,
                                x.mrpEventTypeId, (isBuilt ? x.PROP_MANUF_O_RECP : x.PROP_PUR_O_RECP));
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(eventMap, proposedOrder.getQuantity(), null,
                                    eventName, (proposedOrder.getRequirementStartDate().compareTo(now) < 0), delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingMrpCreateOrUpdateEvent,
                                    UtilMisc.toMap(x.parameters, parameters), locale));
                        }
                        //
                        stockTmp = stockTmp.add(proposedOrder.getQuantity());
                    }
                }
            } else {
                bomLevelWithNoEvent += 1;
            }

            bomLevel += 1;
            // if there are 3 levels with no inventoryEvenPanned we stop
        } while (bomLevelWithNoEvent < 3);

        result = new HashMap<>();
        List<Object> msgResult = new LinkedList<>();
        result.put(x.msgResult, msgResult);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo(x.return_from_executeMrp, MODULE);
        return result;
    }
}
