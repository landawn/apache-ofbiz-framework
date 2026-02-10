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
package org.apache.ofbiz.shipment.packing;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PackingServicesContext;
public class PackingServices {

    private static final String MODULE = PackingServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;

    public static Map<String, Object> addPackLine(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        String shipGroupSeqId = (String) context.get(x.shipGroupSeqId);
        String orderId = (String) context.get(x.orderId);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        BigDecimal weight = (BigDecimal) context.get(x.weight);
        Integer packageSeq = (Integer) context.get(x.packageSeq);

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get(x.handlingInstructions);
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get(x.pickerPartyId);
        session.setPickerPartyId(pickerPartyId);

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }

        Debug.logInfo(x.OrderId_8fce4311 + orderId + x.ship_group + shipGroupSeqId + x.Pack_input + productId + x.str_bf2a0367
                + quantity + x.packageSeq_f510c2a9 + packageSeq + x.weight_855d7da0 + weight + x.str_4ff447b8, MODULE);

        if (weight == null) {
            Debug.logWarning(x.OrderId_8fce4311 + orderId + x.ship_group + shipGroupSeqId + x.product_53809e6e + productId
                    + x.being_packed_without_a_weight_assuming_0, MODULE);
            weight = BigDecimal.ZERO;
        }

        try {
            session.addOrIncreaseLine(orderId, null, shipGroupSeqId, productId, quantity, packageSeq, weight, false);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * <p>Create or update package lines.
     *
     * Context parameters:
     * <ul>
     * <li>selInfo - selected rows</li>
     * <li>iteInfo - orderItemIds</li>
     * <li>prdInfo - productIds</li>
     * <li>pkgInfo - package numbers</li>
     * <li>wgtInfo - weights to pack</li>
     * <li>numPackagesInfo - number of packages to pack per line (&gt;= 1, default: 1)<br>
     * Packs the same items n times in consecutive packages, starting from the package number retrieved from pkgInfo.</li>
     * </ul>
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> packBulk(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        String orderId = (String) context.get(x.orderId);
        String shipGroupSeqId = (String) context.get(x.shipGroupSeqId);
        Boolean updateQuantity = (Boolean) context.get(x.updateQuantity);
        Locale locale = (Locale) context.get(x.locale);
        if (updateQuantity == null) {
            updateQuantity = Boolean.FALSE;
        }

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get(x.handlingInstructions);
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get(x.pickerPartyId);
        session.setPickerPartyId(pickerPartyId);

        Map<String, ?> selInfo = UtilGenerics.cast(context.get(x.selInfo));
        Map<String, String> iteInfo = UtilGenerics.cast(context.get(x.iteInfo));
        Map<String, String> prdInfo = UtilGenerics.cast(context.get(x.prdInfo));
        Map<String, String> qtyInfo = UtilGenerics.cast(context.get(x.qtyInfo));
        Map<String, String> pkgInfo = UtilGenerics.cast(context.get(x.pkgInfo));
        Map<String, String> wgtInfo = UtilGenerics.cast(context.get(x.wgtInfo));
        Map<String, String> numPackagesInfo = UtilGenerics.cast(context.get(x.numPackagesInfo));

        if (selInfo != null) {
            for (String rowKey: selInfo.keySet()) {
                String orderItemSeqId = iteInfo.get(rowKey);
                String prdStr = prdInfo.get(rowKey);
                if (UtilValidate.isEmpty(prdStr)) {
                    // set the productId to null if empty
                    prdStr = null;
                }

                // base package/quantity/weight strings
                String pkgStr = pkgInfo.get(rowKey);
                String qtyStr = qtyInfo.get(rowKey);
                String wgtStr = wgtInfo.get(rowKey);

                Debug.logInfo(x.Item_d878f95a + orderItemSeqId + x.Product_e60814cf + prdStr + x.Quantity + qtyStr + x.Package
                        + pkgStr + x.Weight + wgtStr, MODULE);

                // array place holders
                String[] quantities;
                String[] packages;
                String[] weights;

                // process the package array
                if (pkgStr.indexOf(',') != -1) {
                    // this is a multi-box update
                    packages = pkgStr.split(x.str_5c10b5b2);
                } else {
                    packages = new String[] {pkgStr };
                }

                // check to make sure there is at least one package
                if (packages.length == 0) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductPackBulkNoPackagesDefined, locale));
                }

                // process the quantity array
                if (qtyStr == null) {
                    quantities = new String[packages.length];
                    for (int p = 0; p < packages.length; p++) {
                        quantities[p] = qtyInfo.get(rowKey + x.str_05a79f06 + packages[p]);
                    }
                    if (quantities.length != packages.length) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ProductPackBulkPackagesAndQuantitiesDoNotMatch, locale));
                    }
                } else {
                    quantities = new String[] {qtyStr };
                }

                // process the weight array
                if (UtilValidate.isEmpty(wgtStr)) wgtStr = x._0;
                weights = new String[] {wgtStr };

                for (int p = 0; p < packages.length; p++) {
                    BigDecimal quantity;
                    int packageSeq;
                    BigDecimal weightSeq;
                    try {
                        quantity = new BigDecimal(quantities[p]);
                        packageSeq = Integer.parseInt(packages[p]);
                        weightSeq = new BigDecimal(weights[p]);
                    } catch (Exception e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    try {
                        String numPackagesStr = numPackagesInfo.get(rowKey);
                        int numPackages = 1;
                        if (numPackagesStr != null) {
                            try {
                                numPackages = Integer.parseInt(numPackagesStr);
                                if (numPackages < 1) {
                                    numPackages = 1;
                                }
                            } catch (NumberFormatException nex) {
                            }
                        }
                        for (int numPackage = 0; numPackage < numPackages; numPackage++) {
                            session.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, prdStr, quantity, packageSeq
                                    + numPackage, weightSeq, updateQuantity);
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> incrementPackageSeq(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        int nextSeq = session.nextPackageSeq();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.nextPackageSeq, nextSeq);
        return result;
    }

    public static Map<String, Object> clearLastPackage(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        int nextSeq = session.clearLastPackage();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.nextPackageSeq, nextSeq);
        return result;
    }

    public static Map<String, Object> clearPackLine(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        String orderId = (String) context.get(x.orderId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);
        String shipGroupSeqId = (String) context.get(x.shipGroupSeqId);
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        String productId = (String) context.get(x.productId);
        Integer packageSeqId = (Integer) context.get(x.packageSeqId);
        Locale locale = (Locale) context.get(x.locale);

        PackingSessionLine line = session.findLine(orderId, orderItemSeqId, shipGroupSeqId,
                productId, inventoryItemId, packageSeqId);

        // remove the line
        if (line != null) {
            session.clearLine(line);
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPackLineNotFound, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearPackAll(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        session.clearAllLines();

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> calcPackSessionAdditionalShippingCharge(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        Map<String, String> packageWeights = UtilGenerics.cast(context.get(x.packageWeights));
        String weightUomId = (String) context.get(x.weightUomId);
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        String shipmentMethodTypeId = (String) context.get(x.shipmentMethodTypeId);
        String carrierPartyId = (String) context.get(x.carrierPartyId);
        String carrierRoleTypeId = (String) context.get(x.carrierRoleTypeId);
        String productStoreId = (String) context.get(x.productStoreId);

        BigDecimal shippableWeight = setSessionPackageWeights(session, packageWeights);
        BigDecimal estimatedShipCost = session.getShipmentCostEstimate(shippingContactMechId, shipmentMethodTypeId, carrierPartyId,
                carrierRoleTypeId, productStoreId, null, null, shippableWeight, null);
        session.setAdditionalShippingCharge(estimatedShipCost);
        session.setWeightUomId(weightUomId);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.additionalShippingCharge, estimatedShipCost);
        return result;
    }


    public static Map<String, Object> completePack(DispatchContext dctx, PackingServicesContext context) {
        PackingSession session = (PackingSession) context.get(x.packingSession);
        Locale locale = (Locale) context.get(x.locale);
        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get(x.handlingInstructions);
        String pickerPartyId = (String) context.get(x.pickerPartyId);
        BigDecimal additionalShippingCharge = (BigDecimal) context.get(x.additionalShippingCharge);
        Map<String, String> packageWeights = UtilGenerics.cast(context.get(x.packageWeights));
        Map<String, String> boxTypes = UtilGenerics.cast(context.get(x.boxTypes));
        String weightUomId = (String) context.get(x.weightUomId);
        session.setHandlingInstructions(instructions);
        session.setPickerPartyId(pickerPartyId);
        session.setAdditionalShippingCharge(additionalShippingCharge);
        session.setWeightUomId(weightUomId);
        setSessionPackageWeights(session, packageWeights);
        setSessionShipmentBoxTypes(session, boxTypes);

        Boolean force = (Boolean) context.get(x.forceComplete);
        if (force == null) {
            force = Boolean.FALSE;
        }

        String shipmentId = null;
        try {
            shipmentId = session.complete(force);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }

        Map<String, Object> resp;
        if (x.EMPTY.equals(shipmentId)) {
            resp = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPackCompleteNoItems, locale));
        } else {
            resp = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                    x.ProductPackComplete, UtilMisc.toMap(x.shipmentId, shipmentId), locale));
        }

        resp.put(x.shipmentId, shipmentId);
        return resp;
    }

    public static BigDecimal setSessionPackageWeights(PackingSession session, Map<String, String> packageWeights) {
        BigDecimal shippableWeight = BigDecimal.ZERO;
        if (!UtilValidate.isEmpty(packageWeights)) {
            for (Map.Entry<String, String> entry: packageWeights.entrySet()) {
                String packageSeqId = entry.getKey();
                String packageWeightStr = entry.getValue();
                if (UtilValidate.isNotEmpty(packageWeightStr)) {
                    BigDecimal packageWeight = new BigDecimal(packageWeights.get(packageSeqId));
                    session.setPackageWeight(Integer.parseInt(packageSeqId), packageWeight);
                    shippableWeight = shippableWeight.add(packageWeight);
                } else {
                    session.setPackageWeight(Integer.parseInt(packageSeqId), null);
                }
            }
        }
        return shippableWeight;
    }

    public static void setSessionShipmentBoxTypes(PackingSession session, Map<String, String> boxTypes) {
        if (UtilValidate.isNotEmpty(boxTypes)) {
            for (Map.Entry<String, String> entry: boxTypes.entrySet()) {
                String packageSeqId = entry.getKey();
                String boxTypeStr = entry.getValue();
                if (UtilValidate.isNotEmpty(boxTypeStr)) {
                    session.setShipmentBoxType(Integer.parseInt(packageSeqId), boxTypeStr);
                } else {
                    session.setShipmentBoxType(Integer.parseInt(packageSeqId), null);
                }
            }
        }
    }
}
