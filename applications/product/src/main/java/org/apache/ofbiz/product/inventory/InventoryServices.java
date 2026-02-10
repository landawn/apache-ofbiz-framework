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
package org.apache.ofbiz.product.inventory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FacilityDao;
import org.apache.ofbiz.persistence.dao.InventoryItemDao;
import org.apache.ofbiz.persistence.dao.InventoryTransferDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGroupAssocDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGroupDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.ProductPriceDao;
import org.apache.ofbiz.persistence.entity.FacilityEntity;
import org.apache.ofbiz.persistence.entity.InventoryItemEntity;
import org.apache.ofbiz.persistence.entity.InventoryTransferEntity;
import org.apache.ofbiz.persistence.entity.OrderItemShipGroupAssocEntity;
import org.apache.ofbiz.persistence.entity.OrderItemShipGroupEntity;
import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.persistence.entity.ProductPriceEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.InventoryServicesContext;
/**
 * Inventory Services
 */
public class InventoryServices {

    private static final String MODULE = InventoryServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final MathContext GEN_ROUNDING = new MathContext(10);

    public static Map<String, Object> prepareInventoryTransfer(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        BigDecimal xferQty = (BigDecimal) context.get(x.xferQty);
        GenericValue inventoryItem = null;
        GenericValue newItem = null;
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        try {
            inventoryItem = getInventoryItemValue(delegator, inventoryItemId);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductNotFindInventoryItemWithId, locale) + inventoryItemId);
        }

        if (inventoryItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductNotFindInventoryItemWithId, locale) + inventoryItemId);
        }

        try {
            Map<String, Object> results = ServiceUtil.returnSuccess();

            String inventoryType = inventoryItem.getString(x.inventoryItemTypeId);
            if (x.NON_SERIAL_INV_ITEM.equals(inventoryType)) {
                BigDecimal atp = inventoryItem.getBigDecimal(x.availableToPromiseTotal);
                BigDecimal qoh = inventoryItem.getBigDecimal(x.quantityOnHandTotal);

                if (atp == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductInventoryItemATPNotAvailable,
                            UtilMisc.toMap(x.inventoryItemId, inventoryItem.getString(x.inventoryItemId)), locale));
                }
                if (qoh == null) {
                    qoh = atp;
                }

                // first make sure we have enough to cover the request transfer amount
                if (xferQty.compareTo(atp) > 0) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductInventoryItemATPIsNotSufficient,
                            UtilMisc.toMap(x.inventoryItemId, inventoryItem.getString(x.inventoryItemId),
                                    x.atp, atp, x.xferQty, xferQty), locale));
                }

                /*
                 * atp < qoh - split and save the qoh - atp
                 * xferQty < atp - split and save atp - xferQty
                 * atp < qoh && xferQty < atp - split and save qoh - atp + atp - xferQty
                 */

                // at this point we have already made sure that the xferQty is less than or equals to the atp,
                // so if less that just create a new inventory record for the quantity to be moved
                // NOTE: atp should always be <= qoh, so if xfer < atp, then xfer < qoh, so no need to check/handle that
                // however, if atp < qoh && atp == xferQty, then we still need to split; oh, but no need to check atp == xferQty in the second part
                // because if it isn't greater and isn't less, then it is equal
                if (xferQty.compareTo(atp) < 0 || atp.compareTo(qoh) < 0) {
                    BigDecimal negXferQty = xferQty.negate();
                    // NOTE: new inventory items should always be created calling the
                    //       createInventoryItem service because in this way we are sure
                    //       that all the relevant fields are filled with default values.
                    //       However, the code here should work fine because all the values
                    //       for the new inventory item are inerited from the existing item.
                    newItem = GenericValue.create(inventoryItem);
                    newItem.set(x.availableToPromiseTotal, BigDecimal.ZERO);
                    newItem.set(x.quantityOnHandTotal, BigDecimal.ZERO);

                    delegator.createSetNextSeqId(newItem);

                    results.put(x.inventoryItemId, newItem.get(x.inventoryItemId));

                    // TODO: how do we get this here: "inventoryTransferId", inventoryTransferId
                    Map<String, Object> createNewDetailMap = UtilMisc.toMap(x.availableToPromiseDiff, xferQty, x.quantityOnHandDiff, xferQty,
                            x.accountingQuantityDiff, xferQty, x.inventoryItemId, newItem.get(x.inventoryItemId), x.userLogin, userLogin);
                    Map<String, Object> createUpdateDetailMap = UtilMisc.toMap(x.availableToPromiseDiff, negXferQty, x.quantityOnHandDiff,
                            negXferQty, x.accountingQuantityDiff, negXferQty, x.inventoryItemId, inventoryItem.get(x.inventoryItemId), x.userLogin,
                            userLogin);
                    try {
                        Map<String, Object> resultNew = dctx.getDispatcher().runSync(x.createInventoryItemDetail, createNewDetailMap);
                        if (ServiceUtil.isError(resultNew)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    x.ProductInventoryItemDetailCreateProblem,
                                    UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, resultNew);
                        }
                        Map<String, Object> resultUpdate = dctx.getDispatcher().runSync(x.createInventoryItemDetail, createUpdateDetailMap);
                        if (ServiceUtil.isError(resultUpdate)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    x.ProductInventoryItemDetailCreateProblem,
                                    UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, resultUpdate);
                        }
                    } catch (GenericServiceException e1) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductInventoryItemDetailCreateProblem,
                                UtilMisc.toMap(x.errorString, e1.getMessage()), locale));
                    }
                } else {
                    results.put(x.inventoryItemId, inventoryItem.get(x.inventoryItemId));
                }
            } else if (x.SERIALIZED_INV_ITEM.equals(inventoryType)) {
                if (!x.INV_AVAILABLE.equals(inventoryItem.getString(x.statusId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductSerializedInventoryNotAvailable, locale));
                }
            }

            // setup values so that no one will grab the inventory during the move
            // if newItem is not null, it is the item to be moved, otherwise the original inventoryItem is the one to be moved
            if (x.NON_SERIAL_INV_ITEM.equals(inventoryType)) {
                // set the transfered inventory item's atp to 0 and the qoh to the xferQty; at this point atp and qoh will always be the same,
                // so we can safely zero the atp for now
                GenericValue inventoryItemToClear = newItem == null ? inventoryItem : newItem;

                inventoryItemToClear.refresh();
                BigDecimal atp = inventoryItemToClear.get(x.availableToPromiseTotal) == null ? BigDecimal.ZERO
                        : inventoryItemToClear.getBigDecimal(x.availableToPromiseTotal);
                if (atp.compareTo(BigDecimal.ZERO) != 0) {
                    Map<String, Object> createDetailMap = UtilMisc.toMap(x.availableToPromiseDiff, atp.negate(),
                            x.inventoryItemId, inventoryItemToClear.get(x.inventoryItemId), x.userLogin, userLogin);
                    try {
                        Map<String, Object> result = dctx.getDispatcher().runSync(x.createInventoryItemDetail, createDetailMap);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    x.ProductInventoryItemDetailCreateProblem,
                                    UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, result);
                        }
                    } catch (GenericServiceException e1) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductInventoryItemDetailCreateProblem,
                                UtilMisc.toMap(x.errorString, e1.getMessage()), locale));
                    }
                }
            } else if (x.SERIALIZED_INV_ITEM.equals(inventoryType)) {
                // set the status to avoid re-moving or something
                if (newItem != null) {
                    newItem.refresh();
                    newItem.set(x.statusId, x.INV_BEING_TRANSFERED);
                    newItem.store();
                    results.put(x.inventoryItemId, newItem.get(x.inventoryItemId));
                } else {
                    inventoryItem.refresh();
                    inventoryItem.set(x.statusId, x.INV_BEING_TRANSFERED);
                    inventoryItem.store();
                    results.put(x.inventoryItemId, inventoryItem.get(x.inventoryItemId));
                }
            }

            return results;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemStoreProblem,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
    }

    public static Map<String, Object> completeInventoryTransfer(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String inventoryTransferId = (String) context.get(x.inventoryTransferId);
        Timestamp receiveDate = (Timestamp) context.get(x.receiveDate);
        GenericValue inventoryTransfer = null;
        GenericValue inventoryItem = null;
        GenericValue destinationFacility = null;
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        try {
            inventoryTransfer = getInventoryTransferValue(delegator, inventoryTransferId);
            inventoryItem = inventoryTransfer.getRelatedOne(x.InventoryItem, false);
            destinationFacility = inventoryTransfer.getRelatedOne(x.ToFacility, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemLookupProblem,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (inventoryItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemLookupProblem,
                    UtilMisc.toMap(x.errorString, x.emptyString), locale));
        }

        String inventoryType = inventoryItem.getString(x.inventoryItemTypeId);

        // set the fields on the transfer record
        if (inventoryTransfer.get(x.receiveDate) == null) {
            if (receiveDate != null) {
                inventoryTransfer.set(x.receiveDate, receiveDate);
            } else {
                inventoryTransfer.set(x.receiveDate, UtilDateTime.nowTimestamp());
            }
        }

        if (x.NON_SERIAL_INV_ITEM.equals(inventoryType)) {
            // add an adjusting InventoryItemDetail so set ATP back to QOH: ATP = ATP + (QOH - ATP), diff = QOH - ATP
            BigDecimal atp = inventoryItem.get(x.availableToPromiseTotal) == null ? BigDecimal.ZERO
                    : inventoryItem.getBigDecimal(x.availableToPromiseTotal);
            BigDecimal qoh = inventoryItem.get(x.quantityOnHandTotal) == null ? BigDecimal.ZERO : inventoryItem.getBigDecimal(x.quantityOnHandTotal);
            Map<String, Object> createDetailMap = UtilMisc.toMap(x.availableToPromiseDiff, qoh.subtract(atp),
                    x.inventoryItemId, inventoryItem.get(x.inventoryItemId), x.userLogin, userLogin);
            try {
                Map<String, Object> result = dctx.getDispatcher().runSync(x.createInventoryItemDetail, createDetailMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductInventoryItemDetailCreateProblem,
                            UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, result);
                }
            } catch (GenericServiceException e1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemDetailCreateProblem,
                        UtilMisc.toMap(x.errorString, e1.getMessage()), locale));
            }
            try {
                inventoryItem.refresh();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemRefreshProblem,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
        }

        // set the fields on the item
        Map<String, Object> updateInventoryItemMap = UtilMisc.toMap(x.inventoryItemId, inventoryItem.getString(x.inventoryItemId),
                                                    x.facilityId, inventoryTransfer.get(x.facilityIdTo),
                                                    x.containerId, inventoryTransfer.get(x.containerIdTo),
                                                    x.locationSeqId, inventoryTransfer.get(x.locationSeqIdTo),
                                                    x.userLogin, userLogin);

        // for serialized items, automatically make them available
        if (x.SERIALIZED_INV_ITEM.equals(inventoryType)) {
            updateInventoryItemMap.put(x.statusId, x.INV_AVAILABLE);
        }

        // if the destination facility's owner is different
        // from the inventory item's ownwer,
        // the inventory item is assigned to the new owner.
        if (destinationFacility != null && destinationFacility.get(x.ownerPartyId) != null) {
            String fromPartyId = inventoryItem.getString(x.ownerPartyId);
            String toPartyId = destinationFacility.getString(x.ownerPartyId);
            if (fromPartyId == null || !fromPartyId.equals(toPartyId)) {
                updateInventoryItemMap.put(x.ownerPartyId, toPartyId);
            }
        }
        try {
            Map<String, Object> result = dctx.getDispatcher().runSync(x.updateInventoryItem, updateInventoryItemMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemStoreProblem,
                        UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, result);
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemStoreProblem,
                    UtilMisc.toMap(x.errorString, exc.getMessage()), locale));
        }

        // set the inventory transfer record to complete
        inventoryTransfer.set(x.statusId, x.IXF_COMPLETE);

        // store the entities
        try {
            inventoryTransfer.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemStoreProblem,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> cancelInventoryTransfer(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String inventoryTransferId = (String) context.get(x.inventoryTransferId);
        GenericValue inventoryTransfer = null;
        GenericValue inventoryItem = null;
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        try {
            inventoryTransfer = getInventoryTransferValue(delegator, inventoryTransferId);
            if (UtilValidate.isEmpty(inventoryTransfer)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemTransferNotFound,
                        UtilMisc.toMap(x.inventoryTransferId, inventoryTransferId), locale));
            }
            inventoryItem = inventoryTransfer.getRelatedOne(x.InventoryItem, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemLookupProblem,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (inventoryItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemLookupProblem,
                    UtilMisc.toMap(x.errorString, x.emptyString), locale));
        }

        String inventoryType = inventoryItem.getString(x.inventoryItemTypeId);

        // re-set the fields on the item
        if (x.NON_SERIAL_INV_ITEM.equals(inventoryType)) {
            // add an adjusting InventoryItemDetail so set ATP back to QOH: ATP = ATP + (QOH - ATP), diff = QOH - ATP
            BigDecimal atp = inventoryItem.get(x.availableToPromiseTotal) == null ? BigDecimal.ZERO
                    : inventoryItem.getBigDecimal(x.availableToPromiseTotal);
            BigDecimal qoh = inventoryItem.get(x.quantityOnHandTotal) == null ? BigDecimal.ZERO
                    : inventoryItem.getBigDecimal(x.quantityOnHandTotal);
            Map<String, Object> createDetailMap = UtilMisc.toMap(x.availableToPromiseDiff, qoh.subtract(atp),
                                                 x.inventoryItemId, inventoryItem.get(x.inventoryItemId),
                                                 x.userLogin, userLogin);
            try {
                Map<String, Object> result = dctx.getDispatcher().runSync(x.createInventoryItemDetail, createDetailMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductInventoryItemDetailCreateProblem,
                            UtilMisc.toMap(x.errorString, x.emptyString), locale), null, null, result);
                }
            } catch (GenericServiceException e1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemDetailCreateProblem,
                        UtilMisc.toMap(x.errorString, e1.getMessage()), locale));
            }
        } else if (x.SERIALIZED_INV_ITEM.equals(inventoryType)) {
            inventoryItem.set(x.statusId, x.INV_AVAILABLE);
            // store the entity
            try {
                inventoryItem.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductInventoryItemStoreProblem,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
        }

        // set the inventory transfer record to complete
        inventoryTransfer.set(x.statusId, x.IXF_CANCELLED);

        // store the entities
        try {
            inventoryTransfer.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductInventoryItemStoreProblem,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /** In spite of the generic name this does the very specific task of checking availability of all back-ordered items and sends notices, etc */
    public static Map<String, Object> checkInventoryAvailability(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Map<String, Timestamp>> ordersToUpdate = new HashMap<>();
        Map<String, Map<String, Timestamp>> ordersToCancel = new HashMap<>();

        // find all inventory items w/ a negative ATP
        List<GenericValue> inventoryItems = null;
        try {
            inventoryItems = getInventoryItemsWithNegativeAtp(delegator);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Trouble_getting_inventory_items, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPriceCannotRetrieveInventoryItem, locale));
        }

        if (inventoryItems == null) {
            Debug.logInfo(x.No_items_out_of_stock_no_backorders_to_worry_about, MODULE);
            return ServiceUtil.returnSuccess();
        }

        Debug.logInfo(x.OOS_Inventory_Items + inventoryItems.size(), MODULE);

        for (GenericValue inventoryItem: inventoryItems) {
            // get the incomming shipment information for the item
            List<GenericValue> shipmentAndItems = null;
            try {
                List<EntityExpr> exprs = new ArrayList<>();
                exprs.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, inventoryItem.get(x.productId)));
                exprs.add(EntityCondition.makeCondition(x.destinationFacilityId, EntityOperator.EQUALS, inventoryItem.get(x.facilityId)));
                exprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.SHIPMENT_DELIVERED));
                exprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.SHIPMENT_CANCELLED));

                shipmentAndItems = listShipmentAndItems(delegator, EntityCondition.makeCondition(exprs, EntityOperator.AND));
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Problem_getting_ShipmentAndItem_records, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotRetrieveShipmentAndItem, locale));
            }

            // get the reservations in order of newest first
            List<GenericValue> reservations = null;
            try {
                reservations = inventoryItem.getRelated(x.OrderItemShipGrpInvRes, null, UtilMisc.toList(x.reservedDatetime_f0dc0ea0), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Problem_getting_related_reservations, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotRetrieveRelativeReservation, locale));
            }

            if (reservations == null) {
                Debug.logWarning(x.No_outstanding_reservations_for_this_inventory_item_why_is_it_negative_then, MODULE);
                continue;
            }

            Debug.logInfo(x.Reservations_for_item + reservations.size(), MODULE);

            // available at the time of order
            BigDecimal availableBeforeReserved = inventoryItem.getBigDecimal(x.availableToPromiseTotal);

            // go through all the reservations in order
            for (GenericValue reservation: reservations) {
                String orderId = reservation.getString(x.orderId);
                String orderItemSeqId = reservation.getString(x.orderItemSeqId);
                Timestamp promisedDate = reservation.getTimestamp(x.promisedDatetime);
                Timestamp currentPromiseDate = reservation.getTimestamp(x.currentPromisedDate);
                Timestamp actualPromiseDate = currentPromiseDate;
                if (actualPromiseDate == null) {
                    if (promisedDate != null) {
                        actualPromiseDate = promisedDate;
                    } else {
                        // fall back if there is no promised date stored
                        actualPromiseDate = reservation.getTimestamp(x.reservedDatetime);
                    }
                }

                Debug.logInfo(x.Promised_Date + actualPromiseDate, MODULE);

                // find the next possible ship date
                Timestamp nextShipDate = null;
                BigDecimal availableAtTime = BigDecimal.ZERO;
                for (GenericValue shipmentItem: shipmentAndItems) {
                    availableAtTime = availableAtTime.add(shipmentItem.getBigDecimal(x.quantity));
                    if (availableAtTime.compareTo(availableBeforeReserved) >= 0) {
                        nextShipDate = shipmentItem.getTimestamp(x.estimatedArrivalDate);
                        break;
                    }
                }

                Debug.logInfo(x.Next_Ship_Date + nextShipDate, MODULE);

                // create a modified promise date (promise date - 1 day)
                Calendar pCal = Calendar.getInstance();
                pCal.setTimeInMillis(actualPromiseDate.getTime());
                pCal.add(Calendar.DAY_OF_YEAR, -1);
                Timestamp modifiedPromisedDate = new Timestamp(pCal.getTimeInMillis());
                Timestamp now = UtilDateTime.nowTimestamp();

                Debug.logInfo(x.Promised_Date_1 + modifiedPromisedDate, MODULE);
                Debug.logInfo(x.Now_32d9803b + now, MODULE);

                // check the promised date vs the next ship date
                if (nextShipDate == null || nextShipDate.after(actualPromiseDate)) {
                    if (nextShipDate == null && modifiedPromisedDate.after(now)) {
                        // do nothing; we are okay to assume it will be shipped on time
                        Debug.logInfo(x.No_ship_date_known_yet_but_promised_date_hasn_t_approached_assuming_it_will_be_here_on_time, MODULE);
                    } else {
                        // we cannot ship by the promised date; need to notify the customer
                        Debug.logInfo(x.We_won_t_ship_on_time_getting_notification_info, MODULE);
                        Map<String, Timestamp> notifyItems = ordersToUpdate.get(orderId);
                        if (notifyItems == null) {
                            notifyItems = new HashMap<>();
                        }
                        notifyItems.put(orderItemSeqId, nextShipDate);
                        ordersToUpdate.put(orderId, notifyItems);

                        // need to know if nextShipDate is more then 30 days after promised
                        Calendar sCal = Calendar.getInstance();
                        sCal.setTimeInMillis(actualPromiseDate.getTime());
                        sCal.add(Calendar.DAY_OF_YEAR, 30);
                        Timestamp farPastPromised = new Timestamp(sCal.getTimeInMillis());

                        // check to see if this is >30 days or second run, if so flag to cancel
                        boolean needToCancel = false;
                        if (nextShipDate == null || nextShipDate.after(farPastPromised)) {
                            // we cannot ship until >30 days after promised; using cancel rule
                            Debug.logInfo(x.Ship_date_is_30_past_the_promised_date, MODULE);
                            needToCancel = true;
                        } else if (currentPromiseDate != null && actualPromiseDate.equals(currentPromiseDate)) {
                            // this is the second notification; using cancel rule
                            needToCancel = true;
                        }

                        // add the info to the cancel map if we need to schedule a cancel
                        if (needToCancel) {
                            // queue the item to be cancelled
                            Debug.logInfo(x.Flagging_the_item_to_auto_cancel, MODULE);
                            Map<String, Timestamp> cancelItems = ordersToCancel.get(orderId);
                            if (cancelItems == null) {
                                cancelItems = new HashMap<>();
                            }
                            cancelItems.put(orderItemSeqId, farPastPromised);
                            ordersToCancel.put(orderId, cancelItems);
                        }

                        // store the updated promiseDate as the nextShipDate
                        try {
                            reservation.set(x.currentPromisedDate, nextShipDate);
                            reservation.store();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, x.Problem_storing_reservation + reservation, MODULE);
                        }
                    }
                }

                // subtract our qty from reserved to get the next value
                availableBeforeReserved = availableBeforeReserved.subtract(reservation.getBigDecimal(x.quantity));
            }
        }

        // all items to cancel will also be in the notify list so start with that
        List<String> ordersToNotify = new LinkedList<>();
        for (Map.Entry<String, Map<String, Timestamp>> entry: ordersToUpdate.entrySet()) {
            String orderId = entry.getKey();
            Map<String, Timestamp> backOrderedItems = entry.getValue();
            Map<String, Timestamp> cancelItems = ordersToCancel.get(orderId);
            boolean cancelAll = false;
            Timestamp cancelAllTime = null;

            List<GenericValue> orderItemShipGroups = null;
            try {
                orderItemShipGroups = listOrderItemShipGroupValues(delegator, orderId);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Cannot_get_OrderItemShipGroups_from_orderId + orderId, MODULE);
            }

            for (GenericValue orderItemShipGroup: orderItemShipGroups) {
                List<GenericValue> orderItems = new LinkedList<>();
                List<GenericValue> orderItemShipGroupAssoc = null;
                try {
                    orderItemShipGroupAssoc = listOrderItemShipGroupAssocValues(delegator, orderId, orderItemShipGroup.getString(x.shipGroupSeqId));

                    for (GenericValue assoc: orderItemShipGroupAssoc) {
                        GenericValue orderItem = assoc.getRelatedOne(x.OrderItem, false);
                        if (orderItem != null) {
                            orderItems.add(orderItem);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Problem_fetching_OrderItemShipGroupAssoc, MODULE);
                }


                /* Check the split preference. */
                boolean maySplit = false;
                if (orderItemShipGroup.get(x.maySplit) != null) {
                    maySplit = orderItemShipGroup.getBoolean(x.maySplit);
                }

                /* Figure out if we must cancel all items. */
                if (!maySplit && cancelItems != null) {
                    cancelAll = true;
                    Set<String> cancelSet = cancelItems.keySet();
                    cancelAllTime = cancelItems.get(cancelSet.iterator().next());
                }

                // if there are none to cancel just create an empty map
                if (cancelItems == null) {
                    cancelItems = new HashMap<>();
                }

                List<GenericValue> toBeStored = new LinkedList<>();
                for (GenericValue orderItem: orderItems) {
                    String orderItemSeqId = orderItem.getString(x.orderItemSeqId);
                    Timestamp shipDate = backOrderedItems.get(orderItemSeqId);
                    Timestamp cancelDate = cancelItems.get(orderItemSeqId);
                    Timestamp currentCancelDate = orderItem.getTimestamp(x.autoCancelDate);

                    Debug.logInfo(x.OI_ef9e0842 + orderId + x.SEQID + orderItemSeqId + x.cancelAll + cancelAll + x.cancelDate + cancelDate, MODULE);
                    if (backOrderedItems.containsKey(orderItemSeqId)) {
                        orderItem.set(x.estimatedShipDate, shipDate);

                        if (currentCancelDate == null) {
                            if (cancelAll || cancelDate != null) {
                                if (orderItem.get(x.dontCancelSetUserLogin) == null && orderItem.get(x.dontCancelSetDate) == null) {
                                    if (cancelAllTime != null) {
                                        orderItem.set(x.autoCancelDate, cancelAllTime);
                                    } else {
                                        orderItem.set(x.autoCancelDate, cancelDate);
                                    }
                                }
                            }
                            // only notify orders which have not already sent the final notice
                            ordersToNotify.add(orderId);
                        }
                        toBeStored.add(orderItem);
                    }
                    if (!toBeStored.isEmpty()) {
                        try {
                            delegator.storeAll(toBeStored);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, x.Problem_storing_order_items, MODULE);
                        }
                    }
                }


            }
        }

        // send off a notification for each order
        for (String orderId: ordersToNotify) {
            try {
                dispatcher.runAsync(x.sendOrderBackorderNotification, UtilMisc.<String, Object>toMap(x.orderId, orderId, x.userLogin, userLogin));
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problems_sending_off_the_notification, MODULE);
                continue;
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Get Inventory Available for a Product based on the list of associated products.  The final ATP and QOH will
     * be the minimum of all the associated products' inventory divided by their ProductAssoc.quantity
     * */
    public static Map<String, Object> getProductInventoryAvailableFromAssocProducts(DispatchContext dctx, InventoryServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<GenericValue> productAssocList = UtilGenerics.cast(context.get(x.assocProducts));
        String facilityId = (String) context.get(x.facilityId);
        String statusId = (String) context.get(x.statusId);

        BigDecimal availableToPromiseTotal = BigDecimal.ZERO;
        BigDecimal quantityOnHandTotal = BigDecimal.ZERO;

        if (UtilValidate.isNotEmpty(productAssocList)) {
            // minimum QOH and ATP encountered
            BigDecimal minQuantityOnHandTotal = null;
            BigDecimal minAvailableToPromiseTotal = null;

            // loop through each associated product.
            for (GenericValue productAssoc : productAssocList) {
                String productIdTo = productAssoc.getString(x.productIdTo);
                BigDecimal assocQuantity = productAssoc.getBigDecimal(x.quantity);

                // if there is no quantity for the associated product in ProductAssoc entity, default it to 1.0
                if (assocQuantity == null) {
                    Debug.logWarning(x.ProductAssoc_from + productAssoc.getString(x.productId) + x.to_23757279 + productAssoc.getString(x.productIdTo)
                            + x.has_no_quantity_assuming_1_0, MODULE);
                    assocQuantity = BigDecimal.ONE;
                }

                // figure out the inventory available for this associated product
                Map<String, Object> resultOutput = null;
                try {
                    Map<String, String> inputMap = UtilMisc.toMap(x.productId, productIdTo, x.statusId, statusId);
                    if (facilityId != null) {
                        inputMap.put(x.facilityId, facilityId);
                        resultOutput = dispatcher.runSync(x.getInventoryAvailableByFacility, inputMap);
                    } else {
                        resultOutput = dispatcher.runSync(x.getProductInventoryAvailable, inputMap);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problems_getting_inventory_available_by_facility, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // Figure out what the QOH and ATP inventory would be with this associated product
                BigDecimal currentQuantityOnHandTotal = (BigDecimal) resultOutput.get(x.quantityOnHandTotal);
                BigDecimal currentAvailableToPromiseTotal = (BigDecimal) resultOutput.get(x.availableToPromiseTotal);
                BigDecimal tmpQuantityOnHandTotal = currentQuantityOnHandTotal.divideToIntegralValue(assocQuantity, GEN_ROUNDING);
                BigDecimal tmpAvailableToPromiseTotal = currentAvailableToPromiseTotal.divideToIntegralValue(assocQuantity, GEN_ROUNDING);

                // reset the minimum QOH and ATP quantities if those quantities for this product are less
                if (minQuantityOnHandTotal == null || tmpQuantityOnHandTotal.compareTo(minQuantityOnHandTotal) < 0) {
                    minQuantityOnHandTotal = tmpQuantityOnHandTotal;
                }
                if (minAvailableToPromiseTotal == null || tmpAvailableToPromiseTotal.compareTo(minAvailableToPromiseTotal) < 0) {
                    minAvailableToPromiseTotal = tmpAvailableToPromiseTotal;
                }

                if (Debug.verboseOn()) {
                    Debug.logVerbose(x.productIdTo_4888a104 + productIdTo + x.assocQuantity + assocQuantity + x.current_QOH
                            + currentQuantityOnHandTotal
                            + x.currentATP + currentAvailableToPromiseTotal + x.minQOH + minQuantityOnHandTotal + x.minATP
                            + minAvailableToPromiseTotal, MODULE);
                }
            }
            // the final QOH and ATP quantities are the minimum of all the products
            quantityOnHandTotal = minQuantityOnHandTotal;
            availableToPromiseTotal = minAvailableToPromiseTotal;
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.availableToPromiseTotal, availableToPromiseTotal);
        result.put(x.quantityOnHandTotal, quantityOnHandTotal);
        return result;
    }

    public static Map<String, Object> getProductInventorySummaryForItems(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<GenericValue> orderItems = UtilGenerics.cast(context.get(x.orderItems));
        String facilityId = (String) context.get(x.facilityId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, BigDecimal> atpMap = new HashMap<>();
        Map<String, BigDecimal> qohMap = new HashMap<>();
        Map<String, BigDecimal> mktgPkgAtpMap = new HashMap<>();
        Map<String, BigDecimal> mktgPkgQohMap = new HashMap<>();
        Map<String, Object> results = ServiceUtil.returnSuccess();

        // get a list of all available facilities for looping
        List<GenericValue> facilities = null;
        try {
            if (facilityId != null) {
                facilities = listFacilityValues(delegator, facilityId);
            } else {
                facilities = listFacilityValues(delegator, null);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductErrorFacilityIdNotFound,
                    UtilMisc.toMap(x.facilityId, facilityId), locale));
        }

        // loop through all the order items
        for (GenericValue orderItem: orderItems) {
            String productId = orderItem.getString(x.productId);

            if ((productId == null) || x.emptyString.equals(productId)) {
                continue;
            }

            GenericValue product = null;
            try {
                product = orderItem.getRelatedOne(x.Product, true);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Couldn_t_get_product, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductProductNotFound, locale) + productId);
            }

            BigDecimal atp = BigDecimal.ZERO;
            BigDecimal qoh = BigDecimal.ZERO;
            BigDecimal mktgPkgAtp = BigDecimal.ZERO;
            BigDecimal mktgPkgQoh = BigDecimal.ZERO;

            // loop through all the facilities
            for (GenericValue facility: facilities) {
                Map<String, Object> invResult = null;
                Map<String, Object> mktgPkgInvResult = null;

                // get both the real ATP/QOH available and the quantities available from marketing packages
                try {
                    if (EntityTypeUtil.hasParentType(delegator, x.ProductType, x.productTypeId, product.getString(x.productTypeId), x.parentTypeId,
                            x.MARKETING_PKG)) {
                        mktgPkgInvResult = dispatcher.runSync(x.getMktgPackagesAvailable, UtilMisc.toMap(x.productId, productId, x.facilityId,
                                facility.getString(x.facilityId)));
                    }
                    invResult = dispatcher.runSync(x.getInventoryAvailableByFacility, UtilMisc.toMap(x.productId, productId, x.facilityId,
                            facility.getString(x.facilityId)));
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Could_not_find_inventory_for_facility + facility.getString(x.facilityId), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductInventoryNotAvailableForFacility,
                            UtilMisc.toMap(x.facilityId, facility.getString(x.facilityId)), locale));
                }

                // add the results for this facility to the ATP/QOH counter for all facilities
                if (ServiceUtil.isSuccess(invResult)) {
                    BigDecimal fatp = (BigDecimal) invResult.get(x.availableToPromiseTotal);
                    BigDecimal fqoh = (BigDecimal) invResult.get(x.quantityOnHandTotal);
                    if (fatp != null) {
                        atp = atp.add(fatp);
                    }
                    if (fqoh != null) {
                        qoh = qoh.add(fqoh);
                    }
                }
                if (EntityTypeUtil.hasParentType(delegator, x.ProductType, x.productTypeId, product.getString(x.productTypeId), x.parentTypeId,
                        x.MARKETING_PKG) && ServiceUtil.isSuccess(mktgPkgInvResult)) {
                    BigDecimal fatp = (BigDecimal) mktgPkgInvResult.get(x.availableToPromiseTotal);
                    BigDecimal fqoh = (BigDecimal) mktgPkgInvResult.get(x.quantityOnHandTotal);
                    if (fatp != null) {
                        mktgPkgAtp = mktgPkgAtp.add(fatp);
                    }
                    if (fqoh != null) {
                        mktgPkgQoh = mktgPkgQoh.add(fqoh);
                    }
                }
            }

            atpMap.put(productId, atp);
            qohMap.put(productId, qoh);
            mktgPkgAtpMap.put(productId, mktgPkgAtp);
            mktgPkgQohMap.put(productId, mktgPkgQoh);
        }

        results.put(x.availableToPromiseMap, atpMap);
        results.put(x.quantityOnHandMap, qohMap);
        results.put(x.mktgPkgATPMap, mktgPkgAtpMap);
        results.put(x.mktgPkgQOHMap, mktgPkgQohMap);
        return results;
    }
    public static Map<String, Object> getProductInventoryAndFacilitySummary(DispatchContext dctx, InventoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp checkTime = (Timestamp) context.get(x.checkTime);
        String facilityId = (String) context.get(x.facilityId);
        String productId = (String) context.get(x.productId);
        BigDecimal minimumStock = (BigDecimal) context.get(x.minimumStock);
        String statusId = (String) context.get(x.statusId);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> resultOutput = new HashMap<>();

        Map<String, String> contextInput = UtilMisc.toMap(x.productId, productId, x.facilityId, facilityId, x.statusId, statusId);
        GenericValue product = null;
        try {
            product = getProductValue(delegator, productId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (product != null) {
            if (EntityTypeUtil.hasParentType(delegator, x.ProductType, x.productTypeId, product.getString(
                    x.productTypeId), x.parentTypeId, x.MARKETING_PKG)) {
                try {
                    resultOutput = dispatcher.runSync(x.getMktgPackagesAvailable, contextInput);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            } else {
                try {
                    resultOutput = dispatcher.runSync(x.getInventoryAvailableByFacility, contextInput);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }
            // filter for quantities
            minimumStock = minimumStock != null ? minimumStock : BigDecimal.ZERO;
            BigDecimal quantityOnHandTotal = BigDecimal.ZERO;
            if (resultOutput.get(x.quantityOnHandTotal) != null) {
                quantityOnHandTotal = (BigDecimal) resultOutput.get(x.quantityOnHandTotal);
            }
            BigDecimal offsetQOHQtyAvailable = quantityOnHandTotal.subtract(minimumStock);

            BigDecimal availableToPromiseTotal = BigDecimal.ZERO;
            if (resultOutput.get(x.availableToPromiseTotal) != null) {
                availableToPromiseTotal = (BigDecimal) resultOutput.get(x.availableToPromiseTotal);
            }
            BigDecimal offsetATPQtyAvailable = availableToPromiseTotal.subtract(minimumStock);

            BigDecimal quantityOnOrder = InventoryWorker.getOutstandingPurchasedQuantity(productId, delegator);
            result.put(x.totalQuantityOnHand, resultOutput.get(x.quantityOnHandTotal));
            result.put(x.totalAvailableToPromise, resultOutput.get(x.availableToPromiseTotal));
            result.put(x.quantityOnOrder, quantityOnOrder);
            result.put(x.quantityUomId, product.getString(x.quantityUomId));
            result.put(x.offsetQOHQtyAvailable, offsetQOHQtyAvailable);
            result.put(x.offsetATPQtyAvailable, offsetATPQtyAvailable);
        }
        List<GenericValue> productPrices = null;
        try {
            productPrices = listProductPriceValues(delegator, productId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        //change this for product price
        if (productPrices != null) {
            for (GenericValue onePrice: productPrices) {
                if (x.DEFAULT_PRICE.equals(onePrice.getString(x.productPriceTypeId))) { //defaultPrice
                    result.put(x.defaultPrice, onePrice.getBigDecimal(x.price));
                } else if (x.WHOLESALE_PRICE.equals(onePrice.getString(x.productPriceTypeId))) { //
                    result.put(x.wholeSalePrice, onePrice.getBigDecimal(x.price));
                } else if (x.LIST_PRICE.equals(onePrice.getString(x.productPriceTypeId))) { //listPrice
                    result.put(x.listPrice, onePrice.getBigDecimal(x.price));
                } else {
                    result.put(x.defaultPrice, onePrice.getBigDecimal(x.price));
                    result.put(x.listPrice, onePrice.getBigDecimal(x.price));
                    result.put(x.wholeSalePrice, onePrice.getBigDecimal(x.price));
                }
            }
        }

        DynamicViewEntity salesUsageViewEntity = new DynamicViewEntity();
        DynamicViewEntity productionUsageViewEntity = new DynamicViewEntity();
        if (!UtilValidate.isEmpty(checkTime)) {

            // Construct a dynamic view entity to search against for sales usage quantities
            salesUsageViewEntity.addMemberEntity(x.OI, x.OrderItem);
            salesUsageViewEntity.addMemberEntity(x.OH, x.OrderHeader);
            salesUsageViewEntity.addMemberEntity(x.ItIss, x.ItemIssuance);
            salesUsageViewEntity.addMemberEntity(x.InvIt, x.InventoryItem);
            salesUsageViewEntity.addViewLink(x.OI, x.OH, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.orderId));
            salesUsageViewEntity.addViewLink(x.OI, x.ItIss, Boolean.FALSE,
                    ModelKeyMap.makeKeyMapList(x.orderId, x.orderId, x.orderItemSeqId, x.orderItemSeqId));
            salesUsageViewEntity.addViewLink(x.ItIss, x.InvIt, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.inventoryItemId));
            salesUsageViewEntity.addAlias(x.OI, x.productId);
            salesUsageViewEntity.addAlias(x.OH, x.statusId);
            salesUsageViewEntity.addAlias(x.OH, x.orderTypeId);
            salesUsageViewEntity.addAlias(x.OH, x.orderDate);
            salesUsageViewEntity.addAlias(x.ItIss, x.inventoryItemId);
            salesUsageViewEntity.addAlias(x.ItIss, x.quantity);
            salesUsageViewEntity.addAlias(x.InvIt, x.facilityId);

            // Construct a dynamic view entity to search against for production usage quantities
            productionUsageViewEntity.addMemberEntity(x.WEIA, x.WorkEffortInventoryAssign);
            productionUsageViewEntity.addMemberEntity(x.WE, x.WorkEffort);
            productionUsageViewEntity.addMemberEntity(x.II, x.InventoryItem);
            productionUsageViewEntity.addViewLink(x.WEIA, x.WE, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.workEffortId));
            productionUsageViewEntity.addViewLink(x.WEIA, x.II, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.inventoryItemId));
            productionUsageViewEntity.addAlias(x.WEIA, x.quantity);
            productionUsageViewEntity.addAlias(x.WE, x.actualCompletionDate);
            productionUsageViewEntity.addAlias(x.WE, x.workEffortTypeId);
            productionUsageViewEntity.addAlias(x.II, x.facilityId);
            productionUsageViewEntity.addAlias(x.II, x.productId);

            // Make a query against the sales usage view entity
            EntityCondition cond = EntityCondition.makeCondition(
                    UtilMisc.toList(
                        EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition(x.statusId,
                                EntityOperator.IN, UtilMisc.toList(x.ORDER_COMPLETED, x.ORDER_APPROVED, x.ORDER_HELD)),
                        EntityCondition.makeCondition(x.orderTypeId, EntityOperator.EQUALS, x.SALES_ORDER),
                        EntityCondition.makeCondition(x.orderDate, EntityOperator.GREATER_THAN_EQUAL_TO, checkTime)),
                    EntityOperator.AND);

            try (EntityListIterator salesUsageIt = getInventoryItemDao(delegator).queryIterator(delegator, salesUsageViewEntity, cond)) {

                // Sum the sales usage quantities found
                BigDecimal salesUsageQuantity = BigDecimal.ZERO;
                GenericValue salesUsageItem = null;
                while ((salesUsageItem = salesUsageIt.next()) != null) {
                    if (salesUsageItem.get(x.quantity) != null) {
                        salesUsageQuantity = salesUsageQuantity.add(salesUsageItem.getBigDecimal(x.quantity));
                    }
                }
                // Make a query against the production usage view entity
                EntityCondition conditions = EntityCondition.makeCondition(
                        UtilMisc.toList(
                                EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId),
                                EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId),
                                EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.PROD_ORDER_TASK),
                                EntityCondition.makeCondition(x.actualCompletionDate, EntityOperator.GREATER_THAN_EQUAL_TO, checkTime)),
                        EntityOperator.AND);

                try (EntityListIterator productionUsageIt = getInventoryItemDao(delegator).queryIterator(delegator, productionUsageViewEntity, conditions)) {

                    // Sum the production usage quantities found
                    BigDecimal productionUsageQuantity = BigDecimal.ZERO;
                    GenericValue productionUsageItem = null;
                    while ((productionUsageItem = productionUsageIt.next()) != null) {
                        if (productionUsageItem.get(x.quantity) != null) {
                            productionUsageQuantity = productionUsageQuantity.add(productionUsageItem.getBigDecimal(x.quantity));
                        }
                    }
                    result.put(x.usageQuantity, salesUsageQuantity.add(productionUsageQuantity));
                } catch (GeneralException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    private static InventoryItemDao getInventoryItemDao(Delegator delegator) {
        return DaoRegistry.getDao(delegator, x.InventoryItem, InventoryItemDao.class);
    }

    private static GenericValue getInventoryItemValue(Delegator delegator, String inventoryItemId) throws GenericEntityException {
        try {
            InventoryItemEntity inventoryItemEntity = getInventoryItemDao(delegator).get(inventoryItemId).orElse(null);
            return inventoryItemEntity == null ? null : delegator.makeValue(x.InventoryItem, Beans.beanToMap(inventoryItemEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getInventoryTransferValue(Delegator delegator, String inventoryTransferId) throws GenericEntityException {
        InventoryTransferDao inventoryTransferDao = DaoRegistry.getDao(delegator, x.InventoryTransfer, InventoryTransferDao.class);
        try {
            InventoryTransferEntity inventoryTransferEntity = inventoryTransferDao.get(inventoryTransferId).orElse(null);
            return inventoryTransferEntity == null ? null : delegator.makeValue(x.InventoryTransfer, Beans.beanToMap(inventoryTransferEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static List<GenericValue> getInventoryItemsWithNegativeAtp(Delegator delegator) throws GenericEntityException {
        List<InventoryItemEntity> inventoryItemEntities;
        try {
            inventoryItemEntities = getInventoryItemDao(delegator).list(Filters.lt(x.availableToPromiseTotal, BigDecimal.ZERO));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (InventoryItemEntity inventoryItemEntity : inventoryItemEntities) {
            values.add(delegator.makeValue(x.InventoryItem, Beans.beanToMap(inventoryItemEntity)));
        }
        return values;
    }

    private static List<GenericValue> listShipmentAndItems(Delegator delegator, EntityCondition condition) throws GenericEntityException {
        return getInventoryItemDao(delegator).listShipmentAndItem(delegator, condition, UtilMisc.toList(x.estimatedArrivalDate));
    }

    private static List<GenericValue> listOrderItemShipGroupValues(Delegator delegator, String orderId) throws GenericEntityException {
        OrderItemShipGroupDao orderItemShipGroupDao = DaoRegistry.getDao(delegator, x.OrderItemShipGroup, OrderItemShipGroupDao.class);
        List<OrderItemShipGroupEntity> orderItemShipGroupEntities;
        try {
            orderItemShipGroupEntities = orderItemShipGroupDao.list(Filters.eq(x.orderId, orderId));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (OrderItemShipGroupEntity orderItemShipGroupEntity : orderItemShipGroupEntities) {
            values.add(delegator.makeValue(x.OrderItemShipGroup, Beans.beanToMap(orderItemShipGroupEntity)));
        }
        return values;
    }

    private static List<GenericValue> listOrderItemShipGroupAssocValues(Delegator delegator, String orderId, String shipGroupSeqId)
            throws GenericEntityException {
        OrderItemShipGroupAssocDao orderItemShipGroupAssocDao = DaoRegistry.getDao(delegator, x.OrderItemShipGroupAssoc,
                OrderItemShipGroupAssocDao.class);
        List<OrderItemShipGroupAssocEntity> orderItemShipGroupAssocEntities;
        try {
            orderItemShipGroupAssocEntities = orderItemShipGroupAssocDao.list(Filters.and(
                    Filters.eq(x.orderId, orderId),
                    Filters.eq(x.shipGroupSeqId, shipGroupSeqId)));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (OrderItemShipGroupAssocEntity orderItemShipGroupAssocEntity : orderItemShipGroupAssocEntities) {
            values.add(delegator.makeValue(x.OrderItemShipGroupAssoc, Beans.beanToMap(orderItemShipGroupAssocEntity)));
        }
        return values;
    }

    private static List<GenericValue> listFacilityValues(Delegator delegator, String facilityId) throws GenericEntityException {
        FacilityDao facilityDao = DaoRegistry.getDao(delegator, x.Facility, FacilityDao.class);
        List<FacilityEntity> facilityEntities;
        try {
            facilityEntities = UtilValidate.isNotEmpty(facilityId)
                    ? facilityDao.list(Filters.eq(x.facilityId, facilityId))
                    : facilityDao.list(Filters.alwaysTrue());
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (FacilityEntity facilityEntity : facilityEntities) {
            values.add(delegator.makeValue(x.Facility, Beans.beanToMap(facilityEntity)));
        }
        return values;
    }

    private static GenericValue getProductValue(Delegator delegator, String productId) throws GenericEntityException {
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        try {
            ProductEntity productEntity = productDao.get(productId).orElse(null);
            return productEntity == null ? null : delegator.makeValue(x.Product, Beans.beanToMap(productEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static List<GenericValue> listProductPriceValues(Delegator delegator, String productId) throws GenericEntityException {
        ProductPriceDao productPriceDao = DaoRegistry.getDao(delegator, x.ProductPrice, ProductPriceDao.class);
        List<ProductPriceEntity> productPriceEntities;
        try {
            productPriceEntities = productPriceDao.list(Filters.eq(x.productId, productId));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (ProductPriceEntity productPriceEntity : productPriceEntities) {
            values.add(delegator.makeValue(x.ProductPrice, Beans.beanToMap(productPriceEntity)));
        }
        return EntityUtil.orderBy(values, UtilMisc.toList(x.fromDate_f5440273));
    }
}

