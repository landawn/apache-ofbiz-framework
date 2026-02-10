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
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.GeoDao;
import org.apache.ofbiz.persistence.dao.OrderHeaderDao;
import org.apache.ofbiz.persistence.dao.PostalAddressDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureGroupApplDao;
import org.apache.ofbiz.persistence.dao.ProductStoreEmailSettingDao;
import org.apache.ofbiz.persistence.dao.ProductStoreShipmentMethDao;
import org.apache.ofbiz.persistence.dao.ShipmentCostEstimateDao;
import org.apache.ofbiz.persistence.dao.ShipmentDao;
import org.apache.ofbiz.persistence.dao.ShipmentPackageDao;
import org.apache.ofbiz.persistence.dao.ShipmentPackageRouteSegDao;
import org.apache.ofbiz.persistence.dao.ShipmentReceiptDao;
import org.apache.ofbiz.persistence.dao.ShipmentRouteSegmentDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ShipmentServicesContext;
/**
 * ShipmentServices
 */
public class ShipmentServices {

    private static final String MODULE = ShipmentServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final String RES_ERROR = x.OrderErrorUiLabels;

    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.order_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.order_rounding);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    public static Map<String, Object> createShipmentEstimate(DispatchContext dctx, ShipmentServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        List<GenericValue> storeAll = new LinkedList<>();
        String productStoreShipMethId = (String) context.get(x.productStoreShipMethId);

        GenericValue productStoreShipMeth = null;
        try {
            ProductStoreShipmentMethDao productStoreShipmentMethDao =
                    DaoRegistry.getDao(delegator, x.ProductStoreShipmentMeth, ProductStoreShipmentMethDao.class);
            productStoreShipMeth = productStoreShipmentMethDao.findOneByWhere(delegator, x.ProductStoreShipmentMeth,
                    UtilMisc.toMap(x.productStoreShipMethId, productStoreShipMethId), null, null, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductStoreShipmentMethodCannotRetrieve,
                    UtilMisc.toMap(x.productStoreShipMethId, productStoreShipMethId,
                            x.errorString, e.toString()), locale));
        }

        // Create the basic entity.
        GenericValue estimate = delegator.makeValue(x.ShipmentCostEstimate);

        estimate.set(x.shipmentCostEstimateId, delegator.getNextSeqId(x.ShipmentCostEstimate));
        estimate.set(x.productStoreShipMethId, productStoreShipMethId);
        estimate.set(x.shipmentMethodTypeId, productStoreShipMeth.getString(x.shipmentMethodTypeId));
        estimate.set(x.carrierPartyId, productStoreShipMeth.getString(x.partyId));
        estimate.set(x.carrierRoleTypeId, x.CARRIER);
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

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, x.weight)) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, x.quantity)) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, x.price)) {
            return result;
        }

        try {
            delegator.storeAll(storeAll);
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, x.Problem_reading_product_features + e.toString());
            return result;
        }

        result.put(x.shipmentCostEstimateId, estimate.get(x.shipmentCostEstimateId));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> removeShipmentEstimate(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentCostEstimateId = (String) context.get(x.shipmentCostEstimateId);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue estimate = null;

        try {
            ShipmentCostEstimateDao shipmentCostEstimateDao =
                    DaoRegistry.getDao(delegator, x.ShipmentCostEstimate, ShipmentCostEstimateDao.class);
            estimate = shipmentCostEstimateDao.findOneByWhere(delegator, x.ShipmentCostEstimate,
                    UtilMisc.toMap(x.shipmentCostEstimateId, shipmentCostEstimateId), null, null, false);
            estimate.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductShipmentCostEstimateRemoveError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static boolean applyQuantityBreak(ShipmentServicesContext context, Map<String, Object> result, List<GenericValue> storeAll,
            Delegator delegator, GenericValue estimate, String breakType) {
        String prefix = breakType.substring(0, 1);
        BigDecimal min = (BigDecimal) context.get(prefix + x.min);
        BigDecimal max = (BigDecimal) context.get(prefix + x.max);
        if (min != null || max != null) {
            if (min != null && max != null) {
                if (min.compareTo(max) <= 0 || max.compareTo(BigDecimal.ZERO) == 0) {
                    try {
                        String newSeqId = delegator.getNextSeqId(x.QuantityBreak);
                        GenericValue quantityBreak = delegator.makeValue(x.QuantityBreak,
                                x.quantityBreakId, newSeqId,
                                x.quantityBreakTypeId, x.SHIP + breakType.toUpperCase(Locale.getDefault()),
                                x.fromQuantity, min,
                                x.thruQuantity, max);
                        estimate.set(breakType + x.BreakId, newSeqId);
                        estimate.set(breakType + x.UnitPrice, context.get(prefix + x.price));
                        if (context.containsKey(prefix + x.uom)) {
                            estimate.set(breakType + x.UomId, context.get(prefix + x.uom));
                        }
                        storeAll.add(0, quantityBreak);
                    } catch (Exception e) {
                        Debug.logError(e, MODULE);
                    }
                } else {
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    result.put(ModelService.ERROR_MESSAGE, x.Max + breakType
                            + x.must_not_be_less_than_Min + breakType + x.str_3a52ce78);
                    return false;
                }
            } else {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, breakType + x.Span_Requires_BOTH_Fields);
                return false;
            }
        }
        return true;
    }

    // ShippingEstimate Calc Service
    public static Map<String, Object> calcShipmentCostEstimate(DispatchContext dctx, ShipmentServicesContext context) {
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
        final BigDecimal shippableTotal = UtilNumber.getBigDecimal(context, x.shippableTotal, BigDecimal.ZERO);
        final BigDecimal shippableQuantity = UtilNumber.getBigDecimal(context, x.shippableQuantity, BigDecimal.ZERO);
        final BigDecimal shippableWeight = UtilNumber.getBigDecimal(context, x.shippableWeight, BigDecimal.ZERO);
        final BigDecimal initialEstimateAmt = UtilNumber.getBigDecimal(context, x.initialEstimateAmt, BigDecimal.ZERO);

        // get the ShipmentCostEstimate(s)
        Map<String, String> estFields = UtilMisc.toMap(x.productStoreId, productStoreId,
                x.shipmentMethodTypeId, shipmentMethodTypeId,
                x.carrierPartyId, carrierPartyId,
                x.carrierRoleTypeId, carrierRoleTypeId);

        if (UtilValidate.isNotEmpty(productStoreShipMethId)) {
            // if the productStoreShipMethId field is passed, then also get estimates that have the field set
            estFields.put(x.productStoreShipMethId, productStoreShipMethId);
        }

        List<GenericValue> estimates;
        try {
            ShipmentCostEstimateDao shipmentCostEstimateDao =
                    DaoRegistry.getDao(delegator, x.ShipmentCostEstimate, ShipmentCostEstimateDao.class);
            estimates = shipmentCostEstimateDao.findListByWhere(delegator, x.ShipmentCostEstimate, estFields, null, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductShipmentCostEstimateCannotRetrieve, locale));
        }
        if (estimates.isEmpty()) {
            if (initialEstimateAmt.compareTo(BigDecimal.ZERO) == 0) {
                Debug.logWarning(x.No_shipping_estimates_found_the_shipping_amount_returned_is_0_Condition_used_was
                        + estFields + x.Using_the_passed_context + context, MODULE);
            }

            Map<String, Object> respNow = ServiceUtil.returnSuccess();
            respNow.put(x.shippingEstimateAmount, BigDecimal.ZERO);
            return respNow;
        }

        // Get the PostalAddress
        final GenericValue shipAddress;
        try {
            shipAddress = resolveShippingAddress(delegator, shippingContactMechId, shippingPostalCode, shippingCountryCode);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductShipmentCostEstimateCannotGetShippingAddress, locale));
        }
        // Get the possible estimates.
        List<GenericValue> estimateList = estimates.stream().filter(item ->
                matchGeoAndBreakQuantity(delegator, shippableTotal, shippableQuantity, shippableWeight, shipAddress, item))
                .collect(Collectors.toList());

        if (estimateList.isEmpty()) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    x.ProductShipmentCostEstimateCannotFoundForCarrier,
                    UtilMisc.toMap(x.carrierPartyId, carrierPartyId,
                            x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
        }

        // make the shippable item size/feature objects
        List<BigDecimal> shippableItemSizes = new LinkedList<>();
        Map<String, BigDecimal> shippableFeatureMap = new HashMap<>();
        if (shippableItemInfo != null) {
            for (Map<String, Object> itemMap: shippableItemInfo) {
                // add the item sizes
                if (itemMap.containsKey(x.size)) {
                    BigDecimal itemSize = (BigDecimal) itemMap.get(x.size);
                    if (itemSize != null) {
                        shippableItemSizes.add(itemSize);
                    }
                }

                // add the feature quantities
                BigDecimal quantity = (BigDecimal) itemMap.get(x.quantity);
                if (itemMap.containsKey(x.featureSet)) {
                    Set<String> featureSet = UtilGenerics.cast(itemMap.get(x.featureSet));
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
                    x.partyId, 9,
                    x.roleTypeId, 8,
                    x.geoIdTo, 4,
                    x.weightBreakId, 1,
                    x.quantityBreakId, 1,
                    x.priceBreakId, 1);
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
        BigDecimal orderFlat = UtilNumber.getBigDecimal(estimate, x.orderFlatPrice, BigDecimal.ZERO);
        BigDecimal orderItemFlat = UtilNumber.getBigDecimal(estimate, x.orderItemFlatPrice, BigDecimal.ZERO);
        BigDecimal orderPercent = UtilNumber.getBigDecimal(estimate, x.orderPricePercent, BigDecimal.ZERO);

        BigDecimal itemFlatAmount = shippableQuantity.multiply(orderItemFlat);
        BigDecimal orderPercentage = shippableTotal.multiply(orderPercent.movePointLeft(2));

        // flat total
        BigDecimal flatTotal = orderFlat.add(itemFlatAmount).add(orderPercentage);

        // spans
        BigDecimal weightUnit = UtilNumber.getBigDecimal(estimate, x.weightUnitPrice, BigDecimal.ZERO);
        BigDecimal qtyUnit = UtilNumber.getBigDecimal(estimate, x.quantityUnitPrice, BigDecimal.ZERO);
        BigDecimal priceUnit = UtilNumber.getBigDecimal(estimate, x.priceUnitPrice, BigDecimal.ZERO);

        BigDecimal weightAmount = shippableWeight.multiply(weightUnit);
        BigDecimal quantityAmount = shippableQuantity.multiply(qtyUnit);
        BigDecimal priceAmount = shippableTotal.multiply(priceUnit);

        // span total
        BigDecimal spanTotal = weightAmount.add(quantityAmount).add(priceAmount);

        // feature surcharges
        BigDecimal featureSurcharge = BigDecimal.ZERO;
        BigDecimal featurePercent = UtilNumber.getBigDecimal(estimate, x.featurePercent, BigDecimal.ZERO);
        BigDecimal featurePrice = UtilNumber.getBigDecimal(estimate, x.featurePrice, BigDecimal.ZERO);

        String featureGroupId = estimate.getString(x.productFeatureGroupId);
        if (UtilValidate.isNotEmpty(featureGroupId)) {
            for (Map.Entry<String, BigDecimal> entry: shippableFeatureMap.entrySet()) {
                String featureId = entry.getKey();
                BigDecimal quantity = entry.getValue();
                GenericValue appl = null;
                Map<String, String> fields = UtilMisc.toMap(x.productFeatureGroupId, featureGroupId, x.productFeatureId, featureId);
                try {
                    ProductFeatureGroupApplDao productFeatureGroupApplDao =
                            DaoRegistry.getDao(delegator, x.ProductFeatureGroupAppl, ProductFeatureGroupApplDao.class);
                    EntityCondition applCond = EntityCondition.makeCondition(fields);
                    applCond = EntityCondition.makeCondition(applCond, EntityUtil.getFilterByDateExpr());
                    appl = productFeatureGroupApplDao.findFirstByCondition(delegator, x.ProductFeatureGroupAppl, applCond, null, null, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Unable_to_lookup_feature_group + fields, MODULE);
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
        BigDecimal shippingPricePercent = UtilNumber.getBigDecimal(estimate, x.shippingPricePercent, BigDecimal.ZERO);

        // shipping total
        BigDecimal shippingTotal = subTotal.add((subTotal.add(initialEstimateAmt)).multiply(shippingPricePercent.movePointLeft(2)));

        // prepare the return result
        Map<String, Object> responseResult = ServiceUtil.returnSuccess();
        responseResult.put(x.shippingEstimateAmount, shippingTotal);
        return responseResult;
    }

    private static GenericValue resolveShippingAddress(Delegator delegator, String shippingContactMechId,
                                                       String shippingPostalCode, String shippingCountryCode)
            throws GenericEntityException {
        if (shippingContactMechId != null) {
            PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
            return postalAddressDao.findOneByWhere(delegator, x.PostalAddress, UtilMisc.toMap(x.contactMechId, shippingContactMechId),
                    null, null, false);
        } else if (shippingPostalCode != null) {
            String countryGeoId = null;
            GeoDao geoDao = DaoRegistry.getDao(delegator, x.Geo, GeoDao.class);
            GenericValue countryGeo = geoDao.findFirstByWhere(delegator, x.Geo,
                    UtilMisc.toMap(x.geoTypeId, x.COUNTRY, x.geoCode, shippingCountryCode), null, null, true);
            if (countryGeo != null) {
                countryGeoId = countryGeo.getString(x.geoId);
            }
            return delegator.makeValue(x.PostalAddress,
                    UtilMisc.toMap(x.countryGeoId, countryGeoId,
                            x.postalCodeGeoId, shippingPostalCode));
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

    public static Map<String, Object> fillShipmentStagingTables(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue shipment = null;
        if (shipmentId != null) {
            try {
                ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
                shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (shipment == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductShipmentNotFoundId, locale));
        }

        String shipmentStatusId = shipment.getString(x.statusId);
        if (x.SHIPMENT_PACKED.equals(shipmentStatusId)) {
            GenericValue address = null;
            try {
                address = shipment.getRelatedOne(x.DestinationPostalAddress, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (address == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentNoAddressFound, locale));
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
                        x.ProductShipmentNoPackagesAvailable, locale));
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
            GenericValue stageShip = delegator.makeValue(x.OdbcShipmentOut);
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
                GenericValue stagePkg = delegator.makeValue(x.OdbcPackageOut);
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
            Debug.logWarning(x.Shipment_dda21353 + shipmentId + x.is_not_available_for_shipment_not_setting_in_staging_tables, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateShipmentsFromStaging(DispatchContext dctx, ShipmentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, String> shipmentMap = new HashMap<>();

        ShipmentPackageDao shipmentPackageDao = DaoRegistry.getDao(delegator, x.ShipmentPackage, ShipmentPackageDao.class);
        ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                ShipmentRouteSegmentDao.class);
        ShipmentPackageRouteSegDao shipmentPackageRouteSegDao = DaoRegistry.getDao(delegator, x.ShipmentPackageRouteSeg,
                ShipmentPackageRouteSegDao.class);
        try (EntityListIterator eli = shipmentPackageDao.findIteratorByWhere(delegator, x.OdbcPackageIn, null, null,
                UtilMisc.toList(x.shipmentId, x.shipmentPackageSeqId, x.voidIndicator), null)) {
            GenericValue pkgInfo;
            while ((pkgInfo = eli.next()) != null) {
                String packageSeqId = pkgInfo.getString(x.shipmentPackageSeqId);
                String shipmentId = pkgInfo.getString(x.shipmentId);

                // locate the shipment package
                GenericValue shipmentPackage = shipmentPackageDao.findOneByWhere(delegator, x.ShipmentPackage,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentPackageSeqId, packageSeqId), null, null, false);
                if (shipmentPackage != null) {
                    if (x._00001.equals(packageSeqId)) {
                        // only need to do this for the first package
                        GenericValue rtSeg = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                                UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, x._00001), null, null, false);

                        if (rtSeg == null) {
                            rtSeg = delegator.makeValue(x.ShipmentRouteSegment, UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId,
                                    x._00001));
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
                    pkgCtx.put(x.shipmentId, shipmentId);
                    pkgCtx.put(x.shipmentPackageSeqId, packageSeqId);

                    // first update the weight of the package
                    GenericValue pkg = shipmentPackageDao.findOneByWhere(delegator, x.ShipmentPackage, pkgCtx, null, null, false);

                    if (pkg == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductShipmentPackageNotFound,
                                UtilMisc.toMap(x.shipmentPackageSeqId, packageSeqId,
                                        x.shipmentId, shipmentId), locale));
                    }

                    pkg.set(x.weight, pkgInfo.get(x.packageWeight));
                    delegator.store(pkg);

                    // need if we are the first package (only) update the route seg info
                    pkgCtx.put(x.shipmentRouteSegmentId, x._00001);
                    GenericValue pkgRtSeg = shipmentPackageRouteSegDao.findOneByWhere(delegator, x.ShipmentPackageRouteSeg, pkgCtx, null, null,
                            false);

                    if (pkgRtSeg == null) {
                        pkgRtSeg = delegator.makeValue(x.ShipmentPackageRouteSeg, pkgCtx);
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
            shipCtx.put(x.shipmentId, shipmentId);
            if (x.Y.equals(voidInd)) {
                shipCtx.put(x.statusId, x.SHIPMENT_CANCELLED);
            } else {
                shipCtx.put(x.statusId, x.SHIPMENT_SHIPPED);
            }
            shipCtx.put(x.userLogin, userLogin);
            Map<String, Object> shipResp = null;
            try {
                shipResp = dispatcher.runSync(x.updateShipment, shipCtx);
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
                clearResp = dispatcher.runSync(x.clearShipmentStaging, UtilMisc.<String, Object>toMap(x.shipmentId, shipmentId,
                        x.userLogin, userLogin));
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

    public static Map<String, Object> clearShipmentStagingInfo(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        try {
            delegator.removeByAnd(x.OdbcPackageIn, UtilMisc.toMap(x.shipmentId, shipmentId));
            delegator.removeByAnd(x.OdbcPackageOut, UtilMisc.toMap(x.shipmentId, shipmentId));
            delegator.removeByAnd(x.OdbcShipmentOut, UtilMisc.toMap(x.shipmentId, shipmentId));
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
    public static Map<String, Object> updatePurchaseShipmentFromReceipt(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get(x.shipmentId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        try {

            ShipmentReceiptDao shipmentReceiptDao = DaoRegistry.getDao(delegator, x.ShipmentReceipt, ShipmentReceiptDao.class);
            ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
            List<GenericValue> shipmentReceipts = shipmentReceiptDao.findListByWhere(delegator, x.ShipmentReceipt,
                    UtilMisc.toMap(x.shipmentId, shipmentId), null, null, false);
            if (shipmentReceipts.isEmpty()) return ServiceUtil.returnSuccess();

            // If there are shipment receipts, the shipment must have been shipped, so set the shipment status to
            // PURCH_SHIP_SHIPPED if it's only PURCH_SHIP_CREATED
            GenericValue shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null,
                    false);
            if ((!UtilValidate.isEmpty(shipment)) && x.PURCH_SHIP_CREATED.equals(shipment.getString(x.statusId))) {
                Map<String, Object> updateShipmentMap = dispatcher.runSync(x.updateShipment,
                        UtilMisc.<String, Object>toMap(x.shipmentId, shipmentId, x.statusId, x.PURCH_SHIP_SHIPPED, x.userLogin, userLogin));
                if (ServiceUtil.isError(updateShipmentMap)) {
                    return updateShipmentMap;
                }
            }

            List<GenericValue> shipmentAndItems = shipmentDao.findListByWhere(delegator, x.ShipmentAndItem,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.statusId, x.PURCH_SHIP_SHIPPED), null, null, false);
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
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateShipment, UtilMisc.<String, Object>toMap(x.shipmentId,
                    shipmentId, x.statusId, x.PURCH_SHIP_RECEIVED, x.userLogin, userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> duplicateShipmentRouteSegment(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            GenericValue shipmentRouteSeg = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);
            if (shipmentRouteSeg == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId,
                                x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            Map<String, Object> params = UtilMisc.<String, Object>toMap(x.shipmentId, shipmentId, x.carrierPartyId,
                    shipmentRouteSeg.getString(x.carrierPartyId), x.shipmentMethodTypeId, shipmentRouteSeg.getString(x.shipmentMethodTypeId),
                    x.originFacilityId, shipmentRouteSeg.getString(x.originFacilityId), x.originContactMechId,
                    shipmentRouteSeg.getString(x.originContactMechId),
                    x.originTelecomNumberId, shipmentRouteSeg.getString(x.originTelecomNumberId));
            params.put(x.destFacilityId, shipmentRouteSeg.getString(x.destFacilityId));
            params.put(x.destContactMechId, shipmentRouteSeg.getString(x.destContactMechId));
            params.put(x.destTelecomNumberId, shipmentRouteSeg.getString(x.destTelecomNumberId));
            params.put(x.billingWeight, shipmentRouteSeg.get(x.billingWeight));
            params.put(x.billingWeightUomId, shipmentRouteSeg.get(x.billingWeightUomId));
            params.put(x.userLogin, userLogin);

            Map<String, Object> tmpResult = dispatcher.runSync(x.createShipmentRouteSegment, params);
            if (ServiceUtil.isError(tmpResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
            } else {
                results.put(x.newShipmentRouteSegmentId, tmpResult.get(x.shipmentRouteSegmentId));
                return results;
            }
        } catch (GenericEntityException | GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }
    /**
     * Service to call a ShipmentRouteSegment.carrierPartyId's confirm shipment method asynchronously
     */
    public static Map<String, Object> quickScheduleShipmentRouteSegment(DispatchContext dctx, ShipmentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        String carrierPartyId = null;

        // get the carrierPartyId
        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, true);
            carrierPartyId = shipmentRouteSegment.getString(x.carrierPartyId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // get the shipment label.  This is carrier specific.
        // TODO: This may not need to be done asynchronously.  The reason it's done that way right now is that calling it synchronously means that
        // if we can't confirm a single shipment, then all shipment route segments in a multi-form are rolled back.
        try {
            Map<String, Object> input = UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId,
                    x.userLogin, userLogin);
            // for DHL, we just need to confirm the shipment to get the label.  Other carriers may have more elaborate requirements.
            if (x.DHL.equals(carrierPartyId)) {
                dispatcher.runAsync(x.dhlShipmentConfirm, input);
            } else {
                Debug.logError(carrierPartyId + x.is_not_supported_at_this_time_Sorry, MODULE);
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
    public static Map<String, Object> getShipmentPackageValueFromOrders(DispatchContext dctx, ShipmentServicesContext context) {
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
            ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
            ShipmentPackageDao shipmentPackageDao = DaoRegistry.getDao(delegator, x.ShipmentPackage, ShipmentPackageDao.class);
            shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null, false);
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(RESOURCE, x.ProductShipmentNotFoundId, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            shipmentPackage = shipmentPackageDao.findOneByWhere(delegator, x.ShipmentPackage,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentPackageSeqId, shipmentPackageSeqId), null, null, false);
            if (UtilValidate.isEmpty(shipmentPackage)) {
                String errorMessage = UtilProperties.getMessage(RESOURCE, x.ProductShipmentPackageNotFound, context, locale);
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(errorMessage);
            }

            List<GenericValue> packageContents = shipmentPackageDao.findListByWhere(delegator, x.PackedQtyVsOrderItemQuantity,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentPackageSeqId, shipmentPackageSeqId), null, null, false);
            for (GenericValue packageContent: packageContents) {
                String orderId = packageContent.getString(x.orderId);
                String orderItemSeqId = packageContent.getString(x.orderItemSeqId);

                // Get the value of the orderItem by calling the getOrderItemInvoicedAmountAndQuantity service
                Map<String, Object> getOrderItemValueResult = dispatcher.runSync(x.getOrderItemInvoicedAmountAndQuantity,
                        UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId, x.userLogin, userLogin, x.locale, locale));
                if (ServiceUtil.isError(getOrderItemValueResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getOrderItemValueResult));
                }
                BigDecimal invoicedAmount = (BigDecimal) getOrderItemValueResult.get(x.invoicedAmount);
                BigDecimal invoicedQuantity = (BigDecimal) getOrderItemValueResult.get(x.invoicedQuantity);

                // How much of the invoiced quantity does the issued quantity represent?
                BigDecimal issuedQuantity = packageContent.getBigDecimal(x.issuedQuantity);
                BigDecimal proportionOfInvoicedQuantity = invoicedQuantity.signum() == 0 ? ZERO : issuedQuantity.divide(invoicedQuantity,
                        10, ROUNDING);

                // Prorate the orderItem's invoiced amount by that proportion
                BigDecimal packageContentValue = proportionOfInvoicedQuantity.multiply(invoicedAmount).setScale(DECIMALS, ROUNDING);

                // Convert the value to the shipment currency, if necessary
                GenericValue orderHeader = packageContent.getRelatedOne(x.OrderHeader, false);
                Map<String, Object> convertUomResult = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                        orderHeader.getString(x.currencyUom), x.uomIdTo, currencyUomId, x.originalValue, packageContentValue));
                if (ServiceUtil.isError(convertUomResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(convertUomResult));
                }
                if (convertUomResult.containsKey(x.convertedValue)) {
                    packageContentValue = ((BigDecimal) convertUomResult.get(x.convertedValue)).setScale(DECIMALS, ROUNDING);
                }

                // Add the value of the packed item to the package's total value
                packageTotalValue = packageTotalValue.add(packageContentValue);
            }

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.packageValue, packageTotalValue);
        return result;
    }

    public static Map<String, Object> sendShipmentCompleteNotification(DispatchContext dctx, ShipmentServicesContext context) {
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
        ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
        OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
        ProductStoreEmailSettingDao productStoreEmailSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreEmailSetting,
                ProductStoreEmailSettingDao.class);
        try {
            shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null, false);
            orderHeader = orderHeaderDao.findOneByWhere(delegator, x.OrderHeader,
                    UtilMisc.toMap(x.orderId, shipment.getString(x.primaryOrderId)), null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_getting_info_from_database, MODULE);
        }
        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = productStoreEmailSettingDao.findOneByWhere(delegator, x.ProductStoreEmailSetting,
                    UtilMisc.toMap(x.productStoreId, orderHeader.get(x.productStoreId), x.emailType, x.PRDS_ODR_SHIP_COMPLT), null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_getting_the_ProductStoreEmailSetting_for_productStoreId_b98a4507 + orderHeader.get(x.productStoreId)
                    + x.and_emailType_PRDS_ODR_SHIP_COMPLT, MODULE);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                    x.ProductProductStoreEmailSettingsNotValid,
                    UtilMisc.toMap(x.productStoreId, orderHeader.get(x.productStoreId),
                            x.emailType, x.PRDS_ODR_SHIP_COMPLT), localePar));
        }
        // the override screenUri
        if (UtilValidate.isEmpty(screenUri)) {
            String bodyScreenLocation = productStoreEmail.getString(x.bodyScreenLocation);
            sendMap.put(x.bodyScreenUri, bodyScreenLocation);
        } else {
            sendMap.put(x.bodyScreenUri, screenUri);
        }

        String partyId = shipment.getString(x.partyIdTo);

        // get the email address
        String emailString = null;
        GenericValue email = PartyWorker.findPartyLatestContactMech(partyId, x.EMAIL_ADDRESS, delegator);
        if (UtilValidate.isNotEmpty(email)) {
            emailString = email.getString(x.infoString);
        }
        if (UtilValidate.isEmpty(emailString)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductProductStoreEmailSettingsNoSendToFound, localePar));
        }

        Locale locale = PartyWorker.findPartyLastLocale(partyId, delegator);
        if (locale == null) {
            locale = Locale.getDefault();
        }

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap(x.partyId, partyId, x.shipmentId, shipmentId, x.orderId,
                shipment.getString(x.primaryOrderId), x.userLogin, userLogin, x.locale, locale);
        sendMap.put(x.bodyParameters, bodyParameters);
        sendMap.put(x.userLogin, userLogin);

        sendMap.put(x.subject, productStoreEmail.getString(x.subject));
        sendMap.put(x.contentType, productStoreEmail.get(x.contentType));
        sendMap.put(x.sendFrom, productStoreEmail.get(x.fromAddress));
        sendMap.put(x.sendCc, productStoreEmail.get(x.ccAddress));
        sendMap.put(x.sendBcc, productStoreEmail.get(x.bccAddress));

        if ((sendTo != null) && UtilValidate.isEmail(sendTo)) {
            sendMap.put(x.sendTo, sendTo);
        } else {
            sendMap.put(x.sendTo, emailString);
        }
        // send the notification
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync(x.sendMailFromScreen, sendMap);
        } catch (GenericServiceException gse) {
            Debug.logError(gse, x.Problem_sending_mail, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemSendingEmail, localePar));
        }
        // check for errors
        if (sendResp != null && ServiceUtil.isError(sendResp)) {
            sendResp.put(x.emailType, x.PRDS_ODR_SHIP_COMPLT);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemSendingEmail, localePar),
                    null, null, sendResp);
        }
        return sendResp;
    }
    public static Map<String, Object> getShipmentGatewayConfigFromShipment(Delegator delegator, String shipmentId, Locale locale) {
        Map<String, Object> shipmentGatewayConfig = ServiceUtil.returnSuccess();
        ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
        ProductStoreShipmentMethDao productStoreShipmentMethDao =
                DaoRegistry.getDao(delegator, x.ProductStoreShipmentMeth, ProductStoreShipmentMethDao.class);
        try {
            GenericValue shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null,
                    false);
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentNotFoundId, locale) + shipmentId);
            }
            GenericValue primaryOrderHeader = shipment.getRelatedOne(x.PrimaryOrderHeader, false);
            if (primaryOrderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentPrimaryOrderHeaderNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId), locale));
            }
            String productStoreId = primaryOrderHeader.getString(x.productStoreId);
            if (UtilValidate.isEmpty(productStoreId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentPrimaryOrderHeaderProductStoreNotFound,
                        UtilMisc.toMap(x.productStoreId, productStoreId, x.shipmentId, shipmentId), locale));
            }
            GenericValue primaryOrderItemShipGroup = shipment.getRelatedOne(x.PrimaryOrderItemShipGroup, false);
            if (primaryOrderItemShipGroup == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductShipmentPrimaryOrderHeaderItemShipGroupNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId), locale));
            }
            String shipmentMethodTypeId = primaryOrderItemShipGroup.getString(x.shipmentMethodTypeId);
            String carrierPartyId = primaryOrderItemShipGroup.getString(x.carrierPartyId);
            String carrierRoleTypeId = primaryOrderItemShipGroup.getString(x.carrierRoleTypeId);
            GenericValue productStoreShipmentMeth = productStoreShipmentMethDao.findFirstByWhere(delegator, x.ProductStoreShipmentMeth,
                    UtilMisc.toMap(x.productStoreId, productStoreId, x.shipmentMethodTypeId, shipmentMethodTypeId, x.partyId, carrierPartyId,
                            x.roleTypeId, carrierRoleTypeId), null, null, false);
            if (productStoreShipmentMeth != null) {
                shipmentGatewayConfig.put(x.shipmentGatewayConfigId, productStoreShipmentMeth.getString(x.shipmentGatewayConfigId));
                shipmentGatewayConfig.put(x.configProps, productStoreShipmentMeth.getString(x.configProps));
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductStoreShipmentMethodNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId), locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.FacilityShipmentGatewayConfigFromShipmentError,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        return shipmentGatewayConfig;
    }
}
