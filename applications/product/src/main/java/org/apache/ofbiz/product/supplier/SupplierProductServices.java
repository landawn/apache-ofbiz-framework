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

package org.apache.ofbiz.product.supplier;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.SupplierProductServicesContext;
/**
 * Services for suppliers of products
 */
public class SupplierProductServices {

    private static final String MODULE = SupplierProductServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;

    /*
     * Parameters: productId, partyId, currencyUomId, quantity
     * Result: a List of SupplierProduct entities for productId,
     *         filtered by date and optionally by partyId, ordered with lowest price first
     */
    public static Map<String, Object> getSuppliersForProduct(DispatchContext dctx, SupplierProductServicesContext context) {
        Map<String, Object> results;
        Delegator delegator = dctx.getDelegator();

        GenericValue product = null;
        String productId = (String) context.get(x.productId);
        String partyId = (String) context.get(x.partyId);
        String currencyUomId = (String) context.get(x.currencyUomId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String canDropShip = (String) context.get(x.canDropShip);
        String agreementId = (String) context.get(x.agreementId);

        try {
            ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
            ProductEntity productEntity = productDao.get(productId).orElse(null);
            if (productEntity != null) {
                product = delegator.makeValue(x.Product, Beans.beanToMap(productEntity));
            }
            if (product == null) {
                results = ServiceUtil.returnSuccess();
                results.put(x.supplierProducts, null);
                return results;
            }
            List<GenericValue> supplierProducts = product.getRelated(x.SupplierProduct, null, null, true);

            // if there were no related SupplierProduct entities and the item is a variant, then get the SupplierProducts of the
            // virtual parent product
            if (supplierProducts.isEmpty() && product.getString(x.isVariant) != null && x.Y.equals(product.getString(x.isVariant))) {
                String virtualProductId = ProductWorker.getVariantVirtualId(product);
                ProductEntity virtualProductEntity = productDao.get(virtualProductId).orElse(null);
                GenericValue virtualProduct = virtualProductEntity == null ? null
                        : delegator.makeValue(x.Product, Beans.beanToMap(virtualProductEntity));
                if (virtualProduct != null) {
                    supplierProducts = virtualProduct.getRelated(x.SupplierProduct, null, null, true);
                }
            }
            if (agreementId != null) {
                supplierProducts = EntityUtil.filterByAnd(supplierProducts, UtilMisc.toMap(x.agreementId, agreementId));

            }
            // filter the list by date
            supplierProducts = EntityUtil.filterByDate(supplierProducts, UtilDateTime.nowTimestamp(), x.availableFromDate, x.availableThruDate, true);

            // filter the list down by the partyId if one is provided
            if (partyId != null) {
                supplierProducts = EntityUtil.filterByAnd(supplierProducts, UtilMisc.toMap(x.partyId, partyId));
            }

            // filter the list down by the currencyUomId if one is provided
            if (currencyUomId != null) {
                supplierProducts = EntityUtil.filterByAnd(supplierProducts, UtilMisc.toMap(x.currencyUomId, currencyUomId));
            }

            // filter the list down by the minimumOrderQuantity if one is provided
            if (quantity != null) {
                //minimumOrderQuantity
                supplierProducts = EntityUtil.filterByCondition(supplierProducts, EntityCondition.makeCondition(x.minimumOrderQuantity,
                        EntityOperator.LESS_THAN_EQUAL_TO, quantity));
            }

            // filter the list down by the canDropShip if one is provided
            if (canDropShip != null) {
                supplierProducts = EntityUtil.filterByAnd(supplierProducts, UtilMisc.toMap(x.canDropShip, canDropShip));
            }

            //sort resulting list of SupplierProduct entities by price in ASCENDING order
            supplierProducts = EntityUtil.orderBy(supplierProducts, UtilMisc.toList(x.lastPrice_ASC));

            results = ServiceUtil.returnSuccess();
            results.put(x.supplierProducts, supplierProducts);
        } catch (Exception ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }

    /*
     * Parameters: partyId of a supplier and productFeatures, a Collection (usually List) of product features
     * Service will convert each feature in the Collection, changing their idCode and description based on the
     * SupplierProduct entity for that supplier party and feature, and return it as convertedProductFeatures
     */
    public static Map<String, Object> convertFeaturesForSupplier(DispatchContext dctx, SupplierProductServicesContext context) {
        Map<String, Object> results;
        String partyId = (String) context.get(x.partyId);
        Collection<GenericValue> features = UtilGenerics.cast(context.get(x.productFeatures));

        try {
            if (partyId != null && UtilValidate.isNotEmpty(features)) {
                // loop through all the features, find the related SupplierProductFeature for the given partyId, and
                // substitue description and idCode
                for (GenericValue nextFeature: features) {
                    List<GenericValue> supplierFeatures = EntityUtil.filterByAnd(nextFeature.getRelated(x.SupplierProductFeature, null, null, false),
                                                                   UtilMisc.toMap(x.partyId, partyId));
                    GenericValue supplierFeature = null;

                    if ((supplierFeatures != null) && (!supplierFeatures.isEmpty())) {
                        supplierFeature = supplierFeatures.get(0);
                        if (supplierFeature.get(x.description) != null) {
                            nextFeature.put(x.description, supplierFeature.get(x.description));
                        }
                        if (supplierFeature.get(x.idCode) != null) {
                            nextFeature.put(x.idCode, supplierFeature.get(x.idCode));
                        }
                        // TODO: later, do some kind of uom/quantity conoversion with the UomConversion entity
                    }
                }
            }
            results = ServiceUtil.returnSuccess();
            results.put(x.convertedProductFeatures, features);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }
}
