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
package org.apache.ofbiz.shipment.shipment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.stream.Collectors;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.geo.GeoWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import org.apache.ofbiz.persistence.entity.x;
/**
 * ShipmentServices
 */
public class ShipmentServices {

    private static final String MODULE = ShipmentServices.class.getName();
    private static final String RESOURCE = "ProductUiLabels";
    private static final String RES_ERROR = "OrderErrorUiLabels";

    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("order.rounding");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    public static Map<String, Object> createShipmentEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        List<GenericValue> storeAll = new LinkedList<>();
        String productStoreShipMethId = (String) context.get(x.productStoreShipMethId);

        GenericValue productStoreShipMeth = null;
        try {
            productStoreShipMeth = EntityQuery.use(delegator).from("ProductStoreShipmentMeth").where("productStoreShipMethId",
                    productStoreShipMethId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductStoreShipmentMethodCannotRetrieve",
                    UtilMisc.toMap("productStoreShipMethId", productStoreShipMethId,
                            "errorString", e.toString()), locale));
        }

        // Create the basic entity.
        GenericValue estimate = delegator.makeValue("ShipmentCostEstimate");

        estimate.set(x.shipmentCostEstimateId, delegator.getNextSeqId("ShipmentCostEstimate"));
        estimate.set(x.productStoreShipMethId, productStoreShipMethId);
        estimate.set(x.shipmentMethodTypeId, productStoreShipMeth.getString(x.shipmentMethodTypeId));
        estimate.set(x.carrierPartyId, productStoreShipMeth.getString(x.partyId));
        estimate.set(x.carrierRoleTypeId, "CARRIER");
        estimate.set(x.productStoreId, productStoreShipMeth.getString(x.productStoreId));
        estimate.set(x.geoIdTo, context.get(x.toGeo));
        estimate.set(x.geoIdFrom, context.get(x.fromGeo));
        estimate.set(x.partyId, context.get(x.partyId));
        estimate.set(x.roleTypeId, context.get(x.roleTypeId));
        estimate.set(x.orderPricePercent, context.get(x.flatPercent));
        estimate.set(x.orderFlatPrice, context.get(x.flatPrice));
        estimate.set(x.orderItemFlatPrice, context.get(x.flatItemPrice));
        estimate.set(x.shippingPricePercent, context.get(x.shippingPricePercent));
        estimate.set(x.productFeatureGroupId, context.get(x.productFeatureGroupId));
        estimate.set(x.oversizeUnit, context.get(x.oversizeUnit));
        estimate.set(x.oversizePrice, context.get(x.oversizePrice));
        estimate.set(x.featurePercent, context.get(x.featurePercent));
        estimate.set(x.featurePrice, context.get(x.featurePrice));
        estimate.set(x.weightBreakId, context.get(x.weightBreakId));
        estimate.set(x.weightUnitPrice, context.get(x.wprice));
        estimate.set(x.weightUomId, context.get(x.wuom));
        estimate.set(x.quantityBreakId, context.get(x.quantityBreakId));
        estimate.set(x.quantityUnitPrice, context.get(x.qprice));
        estimate.set(x.quantityUomId, context.get(x.quom));
        estimate.set(x.priceBreakId, context.get(x.priceBreakId));
        estimate.set(x.priceUnitPrice, context.get(x.pprice));
        estimate.set(x.priceUomId, context.get(x.puom));
        storeAll.add(estimate);

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "weight")) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "quantity")) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "price")) {
            return result;
        }

        try {
            delegator.storeAll(storeAll);
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "Problem reading product features: " + e.toString());
            return result;
        }

        result.put("shipmentCostEstimateId", estimate.get(x.shipmentCostEstimateId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> removeShipmentEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentCostEstimateId = (String) context.get(x.shipmentCostEstimateId);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue estimate = null;

        try {
            estimate = EntityQuery.use(delegator).from("ShipmentCostEstimate").where("shipmentCostEstimateId", shipmentCostEstimateId).queryOne();
            estimate.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductShipmentCostEstimateRemoveError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static boolean applyQuantityBreak(Map<String, ? extends Object> context, Map<String, Object> result, List<GenericValue> storeAll,
            Delegator delegator, GenericValue estimate, String breakType) {
        String prefix = breakType.substring(0, 1);
        BigDecimal min = (BigDecimal) context.get(prefix + "min");
        BigDecimal max = (BigDecimal) context.get(prefix + "max");
        if (min != null || max != null) {
            if (min != null && max != null) {
                if (min.compareTo(max) <= 0 || max.compareTo(BigDecimal.ZERO) == 0) {
                    try {
                        String newSeqId = delegator.getNextSeqId("QuantityBreak");
                        GenericValue quantityBreak = delegator.makeValue("QuantityBreak",
                                "quantityBreakId", newSeqId,
                                "quantityBreakTypeId", "SHIP_" + breakType.toUpperCase(Locale.getDefault()),
                                "fromQuantity", min,
                                "thruQuantity", max);
                        estimate.set(breakType + "BreakId", newSeqId);
                        estimate.set(breakType + "UnitPrice", context.get(prefix + "price"));
                        if (context.containsKey(prefix + "uom")) {
                            estimate.set(breakType + "UomId", context.get(prefix + "uom"));
                        }
                        storeAll.add(0, quantityBreak);
                    } catch (Exception e) {
                        Debug.logError(e, MODULE);
                    }
                } else {
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    result.put(ModelService.ERROR_MESSAGE, "Max " + breakType
                            + " must not be less than Min " + breakType + ".");
                    return false;
                }
            } else {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, breakType + " Span Requires BOTH Fields.");
                return false;
            }
        }
        return true;
    }

    // ShippingEstimate Calc Service
    public static Map<String, Object> calcShipmentCostEstimate(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        // prepare the data
        String productStoreShipMethId = (String) context.get(x.productStoreShipMethId);
        String productStoreId = (String) context.get(x.productStoreId);
        String carrierRoleTypeId = (String) context.get(x.carrierRoleTypeId);
        String carrierPartyId = (String) context.get(x.carrierPartyId);
        String shipmentMethodTypeId = (String) context.get(x.shipmentMethodTypeId);
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        String shippingPostalCode = (String) context.get(x.shippingPostalCode);
        String shippingCountryCode = (String) context.get(x.shippingCountryCode);

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.cast(context.get(x.shippableItemInfo));
        final BigDecimal shippableTotal = UtilNumber.getBigDecimal(context, "shippableTotal", BigDecimal.ZERO);
        final BigDecimal shippableQuantity = UtilNumber.getBigDecimal(context, "shippableQuantity", BigDecimal.ZERO);
        final BigDecimal shippableWeight = UtilNumber.getBigDecimal(context, "shippableWeight", BigDecimal.ZERO);
        final BigDecimal initialEstimateAmt = UtilNumber.getBigDecimal(context, "initialEstimateAmt", BigDecimal.ZERO);

        // get the ShipmentCostEstimate(s)
        Map<String, String> estFields = UtilMisc.toMap("productStoreId", productStoreId,
                "shipmentMethodTypeId", shipmentMethodTypeId,
                "carrierPartyId", carrierPartyId,
                "carrierRoleTypeId", carrierRoleTypeId);

        if (UtilValidate.isNotEmpty(productStoreShipMethId)) {
            // if the productStoreShipMethId field is passed, then also get estimates that have the field set
            estFields.put("productStoreShipMethId", productStoreShipMethId);
        }

        List<GenericValue> estimates;
        try {
            estimates = EntityQuery.use(delegator).from("ShipmentCostEstimate")
                            .where(estFields)
                            .cache()
                            .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductShipmentCostEstimateCannotRetrieve", locale));
        }
        if (estimates.isEmpty()) {
            if (initialEstimateAmt.compareTo(BigDecimal.ZERO) == 0) {
                Debug.logWarning("No shipping estimates found; the shipping amount returned is 0! Condition used was: "
                        + estFields + "; Using the passed context: " + context, MODULE);
            }

            Map<String, Object> respNow = ServiceUtil.returnSuccess();
            respNow.put("shippingEstimateAmount", BigDecimal.ZERO);
            return respNow;
        }

        // Get the PostalAddress
        final GenericValue shipAddress;
        try {
            shipAddress = resolveShippingAddress(delegator, shippingContactMechId, shippingPostalCode, shippingCountryCode);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductShipmentCostEstimateCannotGetShippingAddress", locale));
        }
        // Get the possible estimates.
        List<GenericValue> estimateList = estimates.stream().filter(item ->
                matchGeoAndBreakQuantity(delegator, shippableTotal, shippableQuantity, shippableWeight, shipAddress, item))
                .collect(Collectors.toList());

        if (estimateList.isEmpty()) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    "ProductShipmentCostEstimateCannotFoundForCarrier",
                    UtilMisc.toMap("carrierPartyId", carrierPartyId,
                            "shipmentMethodTypeId", shipmentMethodTypeId), locale));
        }

        // make the shippable item size/feature objects
        List<BigDecimal> shippableItemSizes = new LinkedList<>();
        Map<String, BigDecimal> shippableFeatureMap = new HashMap<>();
        if (shippableItemInfo != null) {
            for (Map<String, Object> itemMap: shippableItemInfo) {
                // add the item sizes
                if (itemMap.containsKey("size")) {
                    BigDecimal itemSize = (BigDecimal) itemMap.get("size");
                    if (itemSize != null) {
                        shippableItemSizes.add(itemSize);
                    }
                }

                // add the feature quantities
                BigDecimal quantity = (BigDecimal) itemMap.get("quantity");
                if (itemMap.containsKey("featureSet")) {
                    Set<String> featureSet = UtilGenerics.cast(itemMap.get("featureSet"));
                    if (UtilValidate.isNotEmpty(featureSet)) {
                        for (String featureId: featureSet) {
                            shippableFeatureMap.put(featureId, UtilNumber.safeAdd(quantity, shippableFeatureMap.get(featureId)));
                        }
                    }
                }
            }
        }

        // Grab the estimate and work with it.
        GenericValue estimate;
        if (estimateList.size() > 1) {

            // Calculate priority based on available data.
            final Map<String, Integer> priorityByField = UtilMisc.toMap(
                    "partyId", 9,
                    "roleTypeId", 8,
                    "geoIdTo", 4,
                    "weightBreakId", 1,
                    "quantityBreakId", 1,
                    "priceBreakId", 1);
            TreeMap<Integer, GenericValue> estimatePriority = new TreeMap<>();
            for (GenericValue currentEstimate: estimateList) {
                estimatePriority.put(priorityByField.keySet()
                                                    .stream()
                                                    .filter(k -> UtilValidate.isNotEmpty(currentEstimate.get(k)))
                                                    .mapToInt(priorityByField::get)
                                                    .sum(), currentEstimate);
            }

            // locate the highest priority estimate; or the latest entered
            estimate = estimatePriority.descendingMap().pollFirstEntry().getValue();
        } else {
            estimate = estimateList.get(0);
        }

        // flat fees
        BigDecimal orderFlat = UtilNumber.getBigDecimal(estimate, "orderFlatPrice", BigDecimal.ZERO);
        BigDecimal orderItemFlat = UtilNumber.getBigDecimal(estimate, "orderItemFlatPrice", BigDecimal.ZERO);
        BigDecimal orderPercent = UtilNumber.getBigDecimal(estimate, "orderPricePercent", BigDecimal.ZERO);

        BigDecimal itemFlatAmount = shippableQuantity.multiply(orderItemFlat);
        BigDecimal orderPercentage = shippableTotal.multiply(orderPercent.movePointLeft(2));

        // flat total
        BigDecimal flatTotal = orderFlat.add(itemFlatAmount).add(orderPercentage);

        // spans
        BigDecimal weightUnit = UtilNumber.getBigDecimal(estimate, "weightUnitPrice", BigDecimal.ZERO);
        BigDecimal qtyUnit = UtilNumber.getBigDecimal(estimate, "quantityUnitPrice", BigDecimal.ZERO);
        BigDecimal priceUnit = UtilNumber.getBigDecimal(estimate, "priceUnitPrice", BigDecimal.ZERO);

        BigDecimal weightAmount = shippableWeight.multiply(weightUnit);
        BigDecimal quantityAmount = shippableQuantity.multiply(qtyUnit);
        BigDecimal priceAmount = shippableTotal.multiply(priceUnit);

        // span total
        BigDecimal spanTotal = weightAmount.add(quantityAmount).add(priceAmount);

        // feature surcharges
        BigDecimal featureSurcharge = BigDecimal.ZERO;
        BigDecimal featurePercent = UtilNumber.getBigDecimal(estimate, "featurePercent", BigDecimal.ZERO);
        BigDecimal featurePrice = UtilNumber.getBigDecimal(estimate, "featurePrice", BigDecimal.ZERO);

        String featureGroupId = estimate.getString(x.productFeatureGroupId);
        if (UtilValidate.isNotEmpty(featureGroupId)) {
            for (Map.Entry<String, BigDecimal> entry: shippableFeatureMap.entrySet()) {
                String featureId = entry.getKey();
                BigDecimal quantity = entry.getValue();
                GenericValue appl = null;
                Map<String, String> fields = UtilMisc.toMap("productFeatureGroupId", featureGroupId, "productFeatureId", featureId);
                try {
                    appl = EntityQuery.use(delegator).from("ProductFeatureGroupAppl")
                            .where("productFeatureGroupId", featureGroupId, "productFeatureId", featureId)
                            .cache()
                            .filterByDate()
                            .queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to lookup feature/group" + fields, MODULE);
                }
                if (appl != null) {
                    featureSurcharge = featureSurcharge.add(shippableTotal.multiply(featurePercent.movePointLeft(2)).multiply(quantity));
                    featureSurcharge = featureSurcharge.add(featurePrice.multiply(quantity));
                }
            }
        }

        // size surcharges
        BigDecimal sizeSurcharge = BigDecimal.ZERO;
        BigDecimal sizeUnit = estimate.getBigDecimal(x.oversizeUnit);
        BigDecimal sizePrice = estimate.getBigDecimal(x.oversizePrice);
        if (sizeUnit != null && sizeUnit.compareTo(BigDecimal.ZERO) > 0) {
            for (BigDecimal size : shippableItemSizes) {
                if (size != null && size.compareTo(sizeUnit) >= 0) {
                    sizeSurcharge = UtilNumber.safeAdd(sizeSurcharge, sizePrice);
                }
            }
        }

        // surcharges total
        BigDecimal surchargeTotal = featureSurcharge.add(sizeSurcharge);

        // shipping subtotal
        BigDecimal subTotal = spanTotal.add(flatTotal).add(surchargeTotal);

        // percent add-on
        BigDecimal shippingPricePercent = UtilNumber.getBigDecimal(estimate, "shippingPricePercent", BigDecimal.ZERO);

        // shipping total
        BigDecimal shippingTotal = subTotal.add((subTotal.add(initialEstimateAmt)).multiply(shippingPricePercent.movePointLeft(2)));

        // prepare the return result
        Map<String, Object> responseResult = ServiceUtil.returnSuccess();
        responseResult.put("shippingEstimateAmount", shippingTotal);
        return responseResult;
    }

    private static GenericValue resolveShippingAddress(Delegator delegator, String shippingContactMechId,
                                                       String shippingPostalCode, String shippingCountryCode)
            throws GenericEntityException {
        if (shippingContactMechId != null) {
            return EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
        } else if (shippingPostalCode != null) {
            String countryGeoId = null;
            GenericValue countryGeo = EntityQuery.use(delegator).from("Geo")
                    .where("geoTypeId", "COUNTRY", "geoCode", shippingCountryCode)
                    .cache()
                    .queryFirst();
            if (countryGeo != null) {
                countryGeoId = countryGeo.getString(x.geoId);
            }
            return delegator.makeValue("PostalAddress",
                    UtilMisc.toMap("countryGeoId", countryGeoId,
                            "postalCodeGeoId", shippingPostalCode));
        }
        return null;
    }

    private static boolean matchGeoAndBreakQuantity(Delegator delegator, BigDecimal shippableTotal,
                                                    BigDecimal shippableQuantity, BigDecimal shippableWeight,
                                                    GenericValue shipAddress, GenericValue thisEstimate) {
        try {
            String toGeo = thisEstimate.getString(x.geoIdTo);
            if (UtilValidate.isNotEmpty(toGeo) && shipAddress == null) {
                // This estimate requires shipping address details. We don't have it so we cannot use this estimate.
                return false;
            }

            List<GenericValue> toGeoList = GeoWorker.expandGeoGroup(toGeo, delegator);
            // Make sure we have a valid GEOID.
            if (UtilValidate.isEmpty(toGeoList)
                    || GeoWorker.containsGeo(toGeoList, shipAddress.getString(x.countryGeoId), delegator)
                    || GeoWorker.containsGeo(toGeoList, shipAddress.getString(x.stateProvinceGeoId), delegator)
                    || GeoWorker.containsGeo(toGeoList, shipAddress.getString(x.postalCodeGeoId), delegator)) {

                // now check if some break quantity are present and valid the matching value
                GenericValue wv = thisEstimate.getRelatedOne(x.WeightQuantityBreak, true);
                GenericValue qv = thisEstimate.getRelatedOne(x.QuantityQuantityBreak, true);
                GenericValue pv = thisEstimate.getRelatedOne(x.PriceQuantityBreak, true);
                return (wv == null && qv == null && pv == null) || (
                        isBreakQuantityValid(shippableWeight, wv)
                                && isBreakQuantityValid(shippableQuantity, qv)
                                && isBreakQuantityValid(shippableTotal, pv));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getLocalizedMessage(), MODULE);
        }
        return false;
    }

    private static boolean isBreakQuantityValid(BigDecimal qty, GenericValue breakQuantity) {
        if (breakQuantity == null) {
            return true;
        }
        BigDecimal min = breakQuantity.getBigDecimal(x.fromQuantity);
        BigDecimal max = breakQuantity.getBigDecimal(x.thruQuantity);
        return qty.compareTo(min) >= 0 && (max.compareTo(BigDecimal.ZERO) == 0 || qty.compareTo(max) <= 0);
    }

    public static Map<String, Object> fillShipmentStagingTables(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue shipment = null;
        if (shipmentId != null) {
            try {
                shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (shipment == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductShipmentNotFoundId", locale));
        }

        String shipmentStatusId = shipment.getString(x.statusId);
        if ("SHIPMENT_PACKED".equals(shipmentStatusId)) {
            GenericValue address = null;
            try {
                address = shipment.getRelatedOne(x.DestinationPostalAddress, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (address == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentNoAddressFound", locale));
            }

            List<GenericValue> packages = null;
            try {
                packages = shipment.getRelated(x.ShipmentPackage, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (UtilValidate.isEmpty(packages)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentNoPackagesAvailable", locale));
            }

            List<GenericValue> routeSegs = null;
            try {
                routeSegs = shipment.getRelated(x.ShipmentRouteSegment, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            GenericValue routeSeg = EntityUtil.getFirst(routeSegs);

            // to store list
            List<GenericValue> toStore = new LinkedList<>();

            // make the staging records
            GenericValue stageShip = delegator.makeValue("OdbcShipmentOut");
            stageShip.set(x.shipmentId, shipment.get(x.shipmentId));
            stageShip.set(x.partyId, shipment.get(x.partyIdTo));
            stageShip.set(x.carrierPartyId, routeSeg.get(x.carrierPartyId));
            stageShip.set(x.shipmentMethodTypeId, routeSeg.get(x.shipmentMethodTypeId));
            stageShip.set(x.toName, address.get(x.toName));
            stageShip.set(x.attnName, address.get(x.attnName));
            stageShip.set(x.address1, address.get(x.address1));
            stageShip.set(x.address2, address.get(x.address2));
            stageShip.set(x.directions, address.get(x.directions));
            stageShip.set(x.city, address.get(x.city));
            stageShip.set(x.postalCode, address.get(x.postalCode));
            stageShip.set(x.postalCodeExt, address.get(x.postalCodeExt));
            stageShip.set(x.countryGeoId, address.get(x.countryGeoId));
            stageShip.set(x.stateProvinceGeoId, address.get(x.stateProvinceGeoId));
            stageShip.set(x.numberOfPackages, (long) packages.size());
            stageShip.set(x.handlingInstructions, shipment.get(x.handlingInstructions));
            toStore.add(stageShip);


            for (GenericValue shipmentPkg: packages) {
                GenericValue stagePkg = delegator.makeValue("OdbcPackageOut");
                stagePkg.set(x.shipmentId, shipmentPkg.get(x.shipmentId));
                stagePkg.set(x.shipmentPackageSeqId, shipmentPkg.get(x.shipmentPackageSeqId));
                stagePkg.set(x.orderId, shipment.get(x.primaryOrderId));
                stagePkg.set(x.shipGroupSeqId, shipment.get(x.primaryShipGroupSeqId));
                stagePkg.set(x.shipmentBoxTypeId, shipmentPkg.get(x.shipmentBoxTypeId));
                stagePkg.set(x.weight, shipmentPkg.get(x.weight));
                toStore.add(stagePkg);
            }

            try {
                delegator.storeAll(toStore);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            Debug.logWarning("Shipment #" + shipmentId + " is not available for shipment; not setting in staging tables.", MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateShipmentsFromStaging(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, String> shipmentMap = new HashMap<>();

        EntityQuery eq = EntityQuery.use(delegator)
                .from("OdbcPackageIn")
                .orderBy("shipmentId", "shipmentPackageSeqId", "voidIndicator");

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue pkgInfo;
            while ((pkgInfo = eli.next()) != null) {
                String packageSeqId = pkgInfo.getString(x.shipmentPackageSeqId);
                String shipmentId = pkgInfo.getString(x.shipmentId);

                // locate the shipment package
                GenericValue shipmentPackage = EntityQuery.use(delegator).from("ShipmentPackage").where("shipmentId", shipmentId,
                        "shipmentPackageSeqId", packageSeqId).queryOne();
                if (shipmentPackage != null) {
                    if ("00001".equals(packageSeqId)) {
                        // only need to do this for the first package
                        GenericValue rtSeg = null;
                        rtSeg = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId, "shipmentRouteSegmentId",
                                "00001").queryOne();

                        if (rtSeg == null) {
                            rtSeg = delegator.makeValue("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId",
                                    "00001"));
                            try {
                                delegator.create(rtSeg);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }

                        rtSeg.set(x.actualStartDate, pkgInfo.get(x.shippedDate));
                        rtSeg.set(x.billingWeight, pkgInfo.get(x.billingWeight));
                        rtSeg.set(x.actualCost, pkgInfo.get(x.shippingTotal));
                        rtSeg.set(x.trackingIdNumber, pkgInfo.get(x.trackingNumber));
                        delegator.store(rtSeg);
                    }

                    Map<String, Object> pkgCtx = new HashMap<>();
                    pkgCtx.put("shipmentId", shipmentId);
                    pkgCtx.put("shipmentPackageSeqId", packageSeqId);

                    // first update the weight of the package
                    GenericValue pkg = null;
                    pkg = EntityQuery.use(delegator).from("ShipmentPackage").where(pkgCtx).queryOne();

                    if (pkg == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ProductShipmentPackageNotFound",
                                UtilMisc.toMap("shipmentPackageSeqId", packageSeqId,
                                        "shipmentId", shipmentId), locale));
                    }

                    pkg.set(x.weight, pkgInfo.get(x.packageWeight));
                    delegator.store(pkg);

                    // need if we are the first package (only) update the route seg info
                    pkgCtx.put("shipmentRouteSegmentId", "00001");
                    GenericValue pkgRtSeg = null;
                    pkgRtSeg = EntityQuery.use(delegator).from("ShipmentPackageRouteSeg").where(pkgCtx).queryOne();

                    if (pkgRtSeg == null) {
                        pkgRtSeg = delegator.makeValue("ShipmentPackageRouteSeg", pkgCtx);
                        try {
                            delegator.create(pkgRtSeg);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }

                    pkgRtSeg.set(x.trackingCode, pkgInfo.get(x.trackingNumber));
                    pkgRtSeg.set(x.boxNumber, pkgInfo.get(x.shipmentPackageSeqId));
                    pkgRtSeg.set(x.packageServiceCost, pkgInfo.get(x.packageTotal));
                    delegator.store(pkgRtSeg);
                    shipmentMap.put(shipmentId, pkgInfo.getString(x.voidIndicator));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the status of each shipment
        for (Map.Entry<String, String> entry: shipmentMap.entrySet()) {
            String shipmentId = entry.getKey();
            String voidInd = entry.getValue();
            Map<String, Object> shipCtx = new HashMap<>();
            shipCtx.put("shipmentId", shipmentId);
            if ("Y".equals(voidInd)) {
                shipCtx.put("statusId", "SHIPMENT_CANCELLED");
            } else {
                shipCtx.put("statusId", "SHIPMENT_SHIPPED");
            }
            shipCtx.put("userLogin", userLogin);
            Map<String, Object> shipResp = null;
            try {
                shipResp = dispatcher.runSync("updateShipment", shipCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(shipResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(shipResp));
            }

            // remove the shipment info
            Map<String, Object> clearResp = null;
            try {
                clearResp = dispatcher.runSync("clearShipmentStaging", UtilMisc.<String, Object>toMap("shipmentId", shipmentId,
                        "userLogin", userLogin));
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(clearResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(clearResp));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearShipmentStagingInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        try {
            delegator.removeByAnd("OdbcPackageIn", UtilMisc.toMap("shipmentId", shipmentId));
            delegator.removeByAnd("OdbcPackageOut", UtilMisc.toMap("shipmentId", shipmentId));
            delegator.removeByAnd("OdbcShipmentOut", UtilMisc.toMap("shipmentId", shipmentId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Whenever a ShipmentReceipt is generated, check the Shipment associated
     * with it to see if all items were received. If so, change its status to
     * PURCH_SHIP_RECEIVED. The check is accomplished by counting the
     * products shipped (from ShipmentAndItem) and matching them with the
     * products received (from ShipmentReceipt).
     */
    public static Map<String, Object> updatePurchaseShipmentFromReceipt(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get(x.shipmentId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        try {

            List<GenericValue> shipmentReceipts = EntityQuery.use(delegator).from("ShipmentReceipt").where("shipmentId", shipmentId).queryList();
            if (shipmentReceipts.isEmpty()) return ServiceUtil.returnSuccess();

            // If there are shipment receipts, the shipment must have been shipped, so set the shipment status to
            // PURCH_SHIP_SHIPPED if it's only PURCH_SHIP_CREATED
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if ((!UtilValidate.isEmpty(shipment)) && "PURCH_SHIP_CREATED".equals(shipment.getString(x.statusId))) {
                Map<String, Object> updateShipmentMap = dispatcher.runSync("updateShipment",
                        UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "statusId", "PURCH_SHIP_SHIPPED", "userLogin", userLogin));
                if (ServiceUtil.isError(updateShipmentMap)) {
                    return updateShipmentMap;
                }
            }

            List<GenericValue> shipmentAndItems = EntityQuery.use(delegator).from("ShipmentAndItem").where("shipmentId",
                    shipmentId, "statusId", "PURCH_SHIP_SHIPPED").queryList();
            if (shipmentAndItems.isEmpty()) {
                return ServiceUtil.returnSuccess();
            }

            // store the quantity of each product shipped in a hashmap keyed to productId
            Map<String, BigDecimal> shippedCountMap = new HashMap<>();
            for (GenericValue item: shipmentAndItems) {
                BigDecimal shippedQuantity = item.getBigDecimal(x.quantity);
                BigDecimal quantity = shippedCountMap.get(item.getString(x.productId));
                quantity = quantity == null ? shippedQuantity : shippedQuantity.add(quantity);
                shippedCountMap.put(item.getString(x.productId), quantity);
            }

            // store the quantity of each product received in a hashmap keyed to productId
            Map<String, BigDecimal> receivedCountMap = new HashMap<>();
            for (GenericValue item: shipmentReceipts) {
                BigDecimal receivedQuantity = item.getBigDecimal(x.quantityAccepted);
                BigDecimal quantity = receivedCountMap.get(item.getString(x.productId));
                quantity = quantity == null ? receivedQuantity : receivedQuantity.add(quantity);
                receivedCountMap.put(item.getString(x.productId), quantity);
            }

            // let Map.equals do all the hard comparison work
            if (!shippedCountMap.equals(receivedCountMap)) {
                return ServiceUtil.returnSuccess();
            }

            // now update the shipment
            Map<String, Object> serviceResult = dispatcher.runSync("updateShipment", UtilMisc.<String, Object>toMap("shipmentId",
                    shipmentId, "statusId", "PURCH_SHIP_RECEIVED", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> duplicateShipmentRouteSegment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            GenericValue shipmentRouteSeg = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSeg == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId,
                                "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            Map<String, Object> params = UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "carrierPartyId",
                    shipmentRouteSeg.getString(x.carrierPartyId), "shipmentMethodTypeId", shipmentRouteSeg.getString(x.shipmentMethodTypeId),
                    "originFacilityId", shipmentRouteSeg.getString(x.originFacilityId), "originContactMechId",
                    shipmentRouteSeg.getString(x.originContactMechId),
                    "originTelecomNumberId", shipmentRouteSeg.getString(x.originTelecomNumberId));
            params.put("destFacilityId", shipmentRouteSeg.getString(x.destFacilityId));
            params.put("destContactMechId", shipmentRouteSeg.getString(x.destContactMechId));
            params.put("destTelecomNumberId", shipmentRouteSeg.getString(x.destTelecomNumberId));
            params.put("billingWeight", shipmentRouteSeg.get(x.billingWeight));
            params.put("billingWeightUomId", shipmentRouteSeg.get(x.billingWeightUomId));
            params.put("userLogin", userLogin);

            Map<String, Object> tmpResult = dispatcher.runSync("createShipmentRouteSegment", params);
            if (ServiceUtil.isError(tmpResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
            } else {
                results.put("newShipmentRouteSegmentId", tmpResult.get("shipmentRouteSegmentId"));
                return results;
            }
        } catch (GenericEntityException | GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }
    /**
     * Service to call a ShipmentRouteSegment.carrierPartyId's confirm shipment method asynchronously
     */
    public static Map<String, Object> quickScheduleShipmentRouteSegment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        String carrierPartyId = null;

        // get the carrierPartyId
        try {
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment")
                    .where("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId)
                    .cache(true)
                    .queryOne();
            carrierPartyId = shipmentRouteSegment.getString(x.carrierPartyId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // get the shipment label.  This is carrier specific.
        // TODO: This may not need to be done asynchronously.  The reason it's done that way right now is that calling it synchronously means that
        // if we can't confirm a single shipment, then all shipment route segments in a multi-form are rolled back.
        try {
            Map<String, Object> input = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId,
                    "userLogin", userLogin);
            // for DHL, we just need to confirm the shipment to get the label.  Other carriers may have more elaborate requirements.
            if ("DHL".equals(carrierPartyId)) {
                dispatcher.runAsync("dhlShipmentConfirm", input);
            } else {
                Debug.logError(carrierPartyId + " is not supported at this time.  Sorry.", MODULE);
            }
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), MODULE);
        }

        // don't return an error
        return ServiceUtil.returnSuccess();
    }

    /**
     * Calculates the total value of a shipment package by totalling the results of the getOrderItemInvoicedAmountAndQuantity
     *  service for the orderItem related to each ShipmentPackageContent, prorated by the quantity of the orderItem issued to the
     *  ShipmentPackageContent. Value is converted according to the incoming currencyUomId.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> getShipmentPackageValueFromOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentPackageSeqId = (String) context.get(x.shipmentPackageSeqId);
        String currencyUomId = (String) context.get(x.currencyUomId);

        BigDecimal packageTotalValue = ZERO;

        GenericValue shipment = null;
        GenericValue shipmentPackage = null;
        try {

            shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(RESOURCE, "ProductShipmentNotFoundId", locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            shipmentPackage = EntityQuery.use(delegator).from("ShipmentPackage").where("shipmentId", shipmentId, "shipmentPackageSeqId",
                    shipmentPackageSeqId).queryOne();
            if (UtilValidate.isEmpty(shipmentPackage)) {
                String errorMessage = UtilProperties.getMessage(RESOURCE, "ProductShipmentPackageNotFound", context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            List<GenericValue> packageContents = EntityQuery.use(delegator).from("PackedQtyVsOrderItemQuantity").where("shipmentId", shipmentId,
                    "shipmentPackageSeqId", shipmentPackageSeqId).queryList();
            for (GenericValue packageContent: packageContents) {
                String orderId = packageContent.getString(x.orderId);
                String orderItemSeqId = packageContent.getString(x.orderItemSeqId);

                // Get the value of the orderItem by calling the getOrderItemInvoicedAmountAndQuantity service
                Map<String, Object> getOrderItemValueResult = dispatcher.runSync("getOrderItemInvoicedAmountAndQuantity",
                        UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "userLogin", userLogin, "locale", locale));
                if (ServiceUtil.isError(getOrderItemValueResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getOrderItemValueResult));
                }
                BigDecimal invoicedAmount = (BigDecimal) getOrderItemValueResult.get("invoicedAmount");
                BigDecimal invoicedQuantity = (BigDecimal) getOrderItemValueResult.get("invoicedQuantity");

                // How much of the invoiced quantity does the issued quantity represent?
                BigDecimal issuedQuantity = packageContent.getBigDecimal(x.issuedQuantity);
                BigDecimal proportionOfInvoicedQuantity = invoicedQuantity.signum() == 0 ? ZERO : issuedQuantity.divide(invoicedQuantity,
                        10, ROUNDING);

                // Prorate the orderItem's invoiced amount by that proportion
                BigDecimal packageContentValue = proportionOfInvoicedQuantity.multiply(invoicedAmount).setScale(DECIMALS, ROUNDING);

                // Convert the value to the shipment currency, if necessary
                GenericValue orderHeader = packageContent.getRelatedOne(x.OrderHeader, false);
                Map<String, Object> convertUomResult = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                        orderHeader.getString(x.currencyUom), "uomIdTo", currencyUomId, "originalValue", packageContentValue));
                if (ServiceUtil.isError(convertUomResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(convertUomResult));
                }
                if (convertUomResult.containsKey("convertedValue")) {
                    packageContentValue = ((BigDecimal) convertUomResult.get("convertedValue")).setScale(DECIMALS, ROUNDING);
                }

                // Add the value of the packed item to the package's total value
                packageTotalValue = packageTotalValue.add(packageContentValue);
            }

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("packageValue", packageTotalValue);
        return result;
    }

    public static Map<String, Object> sendShipmentCompleteNotification(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shipmentId = (String) context.get(x.shipmentId);
        String sendTo = (String) context.get(x.sendTo);
        String screenUri = (String) context.get(x.screenUri);
        Locale localePar = (Locale) context.get(x.locale);
        // prepare the shipment information
        Map<String, Object> sendMap = new HashMap<>();
        GenericValue shipment = null;
        GenericValue orderHeader = null;
        try {
            shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", shipment.getString(x.primaryOrderId)).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting info from database", MODULE);
        }
        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId",
                    orderHeader.get(x.productStoreId), "emailType", "PRDS_ODR_SHIP_COMPLT").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId =" + orderHeader.get(x.productStoreId)
                    + " and emailType = PRDS_ODR_SHIP_COMPLT", MODULE);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    "ProductProductStoreEmailSettingsNotValid",
                    UtilMisc.toMap("productStoreId", orderHeader.get(x.productStoreId),
                            "emailType", "PRDS_ODR_SHIP_COMPLT"), localePar));
        }
        // the override screenUri
        if (UtilValidate.isEmpty(screenUri)) {
            String bodyScreenLocation = productStoreEmail.getString(x.bodyScreenLocation);
            sendMap.put("bodyScreenUri", bodyScreenLocation);
        } else {
            sendMap.put("bodyScreenUri", screenUri);
        }

        String partyId = shipment.getString(x.partyIdTo);

        // get the email address
        String emailString = null;
        GenericValue email = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
        if (UtilValidate.isNotEmpty(email)) {
            emailString = email.getString(x.infoString);
        }
        if (UtilValidate.isEmpty(emailString)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductProductStoreEmailSettingsNoSendToFound", localePar));
        }

        Locale locale = PartyWorker.findPartyLastLocale(partyId, delegator);
        if (locale == null) {
            locale = Locale.getDefault();
        }

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("partyId", partyId, "shipmentId", shipmentId, "orderId",
                shipment.getString(x.primaryOrderId), "userLogin", userLogin, "locale", locale);
        sendMap.put("bodyParameters", bodyParameters);
        sendMap.put("userLogin", userLogin);

        sendMap.put("subject", productStoreEmail.getString(x.subject));
        sendMap.put("contentType", productStoreEmail.get(x.contentType));
        sendMap.put("sendFrom", productStoreEmail.get(x.fromAddress));
        sendMap.put("sendCc", productStoreEmail.get(x.ccAddress));
        sendMap.put("sendBcc", productStoreEmail.get(x.bccAddress));

        if ((sendTo != null) && UtilValidate.isEmail(sendTo)) {
            sendMap.put("sendTo", sendTo);
        } else {
            sendMap.put("sendTo", emailString);
        }
        // send the notification
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
        } catch (GenericServiceException gse) {
            Debug.logError(gse, "Problem sending mail", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderProblemSendingEmail", localePar));
        }
        // check for errors
        if (sendResp != null && ServiceUtil.isError(sendResp)) {
            sendResp.put("emailType", "PRDS_ODR_SHIP_COMPLT");
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderProblemSendingEmail", localePar),
                    null, null, sendResp);
        }
        return sendResp;
    }
    public static Map<String, Object> getShipmentGatewayConfigFromShipment(Delegator delegator, String shipmentId, Locale locale) {
        Map<String, Object> shipmentGatewayConfig = ServiceUtil.returnSuccess();
        try {
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentNotFoundId", locale) + shipmentId);
            }
            GenericValue primaryOrderHeader = shipment.getRelatedOne(x.PrimaryOrderHeader, false);
            if (primaryOrderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentPrimaryOrderHeaderNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId), locale));
            }
            String productStoreId = primaryOrderHeader.getString(x.productStoreId);
            if (UtilValidate.isEmpty(productStoreId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentPrimaryOrderHeaderProductStoreNotFound",
                        UtilMisc.toMap("productStoreId", productStoreId, "shipmentId", shipmentId), locale));
            }
            GenericValue primaryOrderItemShipGroup = shipment.getRelatedOne(x.PrimaryOrderItemShipGroup, false);
            if (primaryOrderItemShipGroup == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductShipmentPrimaryOrderHeaderItemShipGroupNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId), locale));
            }
            String shipmentMethodTypeId = primaryOrderItemShipGroup.getString(x.shipmentMethodTypeId);
            String carrierPartyId = primaryOrderItemShipGroup.getString(x.carrierPartyId);
            String carrierRoleTypeId = primaryOrderItemShipGroup.getString(x.carrierRoleTypeId);
            GenericValue productStoreShipmentMeth = EntityQuery.use(delegator).from("ProductStoreShipmentMeth")
                    .where("productStoreId", productStoreId, "shipmentMethodTypeId", shipmentMethodTypeId,
                             "partyId", carrierPartyId, "roleTypeId", carrierRoleTypeId)
                    .queryFirst();
            if (productStoreShipmentMeth != null) {
                shipmentGatewayConfig.put("shipmentGatewayConfigId", productStoreShipmentMeth.getString(x.shipmentGatewayConfigId));
                shipmentGatewayConfig.put("configProps", productStoreShipmentMeth.getString(x.configProps));
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductStoreShipmentMethodNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId), locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "FacilityShipmentGatewayConfigFromShipmentError",
                    UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        return shipmentGatewayConfig;
    }
}
