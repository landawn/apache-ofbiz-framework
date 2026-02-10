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
package org.apache.ofbiz.accounting.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
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
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.geo.GeoWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FacilityDao;
import org.apache.ofbiz.persistence.dao.PartyRelationshipDao;
import org.apache.ofbiz.persistence.dao.PartyTaxAuthInfoDao;
import org.apache.ofbiz.persistence.dao.PostalAddressDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryMemberDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.ProductPriceDao;
import org.apache.ofbiz.persistence.dao.ProductStoreDao;
import org.apache.ofbiz.persistence.dao.TaxAuthorityAssocDao;
import org.apache.ofbiz.persistence.dao.TaxAuthorityDao;
import org.apache.ofbiz.persistence.dao.TaxAuthorityGlAccountDao;
import org.apache.ofbiz.persistence.dao.TaxAuthorityRateProductDao;
import org.apache.ofbiz.persistence.entity.FacilityEntity;
import org.apache.ofbiz.persistence.entity.PartyRelationshipEntity;
import org.apache.ofbiz.persistence.entity.PartyTaxAuthInfoEntity;
import org.apache.ofbiz.persistence.entity.PostalAddressEntity;
import org.apache.ofbiz.persistence.entity.ProductCategoryMemberEntity;
import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.persistence.entity.ProductPriceEntity;
import org.apache.ofbiz.persistence.entity.ProductStoreEntity;
import org.apache.ofbiz.persistence.entity.TaxAuthorityAssocEntity;
import org.apache.ofbiz.persistence.entity.TaxAuthorityEntity;
import org.apache.ofbiz.persistence.entity.TaxAuthorityGlAccountEntity;
import org.apache.ofbiz.persistence.entity.TaxAuthorityRateProductEntity;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.TaxAuthorityServicesContext;
/**
 * Tax Authority tax calculation and other misc services
 */

public class TaxAuthorityServices {

    private static final String MODULE = TaxAuthorityServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    private static final BigDecimal ZERO_BASE = BigDecimal.ZERO;
    private static final BigDecimal ONE_BASE = BigDecimal.ONE;
    private static final BigDecimal PERCENT_SCALE = new BigDecimal(x._100_000);
    private static final int TAX_FINAL_SCALE = UtilNumber.getBigDecimalScale(x.salestax_final_decimals);
    private static final int TAX_SCALE = UtilNumber.getBigDecimalScale(x.salestax_calc_decimals);
    private static final RoundingMode TAX_ROUNDING = UtilNumber.getRoundingMode(x.salestax_rounding);

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static <T> T withSqlException(String operation, SqlSupplier<T> supplier) throws GenericEntityException {
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new GenericEntityException(x.Failed_to + operation, e);
        }
    }

    public static Map<String, Object> rateProductTaxCalcForDisplay(DispatchContext dctx, TaxAuthorityServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get(x.productStoreId);
        String billToPartyId = (String) context.get(x.billToPartyId);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        BigDecimal basePrice = (BigDecimal) context.get(x.basePrice);
        BigDecimal shippingPrice = (BigDecimal) context.get(x.shippingPrice);
        Locale locale = (Locale) context.get(x.locale);

        if (quantity == null) {
            quantity = ONE_BASE;
        }
        BigDecimal amount = basePrice.multiply(quantity);

        BigDecimal taxTotal = ZERO_BASE;
        BigDecimal taxPercentage = ZERO_BASE;
        BigDecimal priceWithTax = basePrice;
        if (shippingPrice != null) {
            priceWithTax = priceWithTax.add(shippingPrice);
        }

        try {
            GenericValue product = getProductValue(delegator, productId);
            GenericValue productStore = getProductStoreValue(delegator, productStoreId);
            if (productStore == null) {
                throw new IllegalArgumentException(x.Could_not_find_ProductStore_with_ID + productStoreId + x.for_tax_calculation);
            }

            if (x.Y.equals(productStore.getString(x.showPricesWithVatTax))) {
                Set<GenericValue> taxAuthoritySet = new HashSet<>();
                if (productStore.get(x.vatTaxAuthPartyId) == null) {
                    List<GenericValue> taxAuthorityRawList = listTaxAuthorityValuesByGeoId(delegator, (String) productStore.get(x.vatTaxAuthGeoId));
                    taxAuthoritySet.addAll(taxAuthorityRawList);
                } else {
                    GenericValue taxAuthority = getTaxAuthorityValue(delegator, (String) productStore.get(x.vatTaxAuthGeoId), (String) productStore.get(x.vatTaxAuthPartyId));
                    taxAuthoritySet.add(taxAuthority);
                }

                if (taxAuthoritySet.isEmpty()) {
                    throw new IllegalArgumentException(x.Could_not_find_any_Tax_Authories_for_store_with_ID
                            + productStoreId + x.for_tax_calculation_the_store_settings_may_need_to_be_corrected);
                }

                List<GenericValue> taxAdustmentList = getTaxAdjustments(delegator, product, productStore, null,
                        billToPartyId, taxAuthoritySet, basePrice, quantity, amount, shippingPrice, ZERO_BASE);
                if (taxAdustmentList.isEmpty()) {
                    // this is something that happens every so often for different products and
                    // such, so don't blow up on it...
                    Debug.logWarning(x.Could_not_find_any_Tax_Authories_Rate_Rules_for_store_with_ID + productStoreId
                            + x.productId_9af4179d + productId + x.basePrice_fb2b6e97 + basePrice + x.amount_0d54197c + amount
                            + x.for_tax_calculation_the_store_settings_may_need_to_be_corrected_94b22aa1, MODULE);
                }

                // add up amounts from adjustments (amount OR exemptAmount, sourcePercentage)
                for (GenericValue taxAdjustment : taxAdustmentList) {
                    if (x.SALES_TAX.equals(taxAdjustment.getString(x.orderAdjustmentTypeId))) {
                        taxPercentage = taxPercentage.add(taxAdjustment.getBigDecimal(x.sourcePercentage));
                        BigDecimal adjAmount = taxAdjustment.getBigDecimal(x.amount);
                        taxTotal = taxTotal.add(adjAmount);
                        priceWithTax = priceWithTax.add(adjAmount.divide(quantity, TAX_SCALE,
                                TAX_ROUNDING));
                        Debug.logInfo(x.For_productId + productId + x.added + adjAmount.divide(quantity,
                                TAX_SCALE, TAX_ROUNDING) + x.of_tax_to_price_for_geoId
                                + taxAdjustment.getString(x.taxAuthGeoId) + x.new_price_is + priceWithTax + x.str_4ff447b8,
                                MODULE);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Data_error_getting_tax_settings + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingTaxSettingError, UtilMisc
                    .toMap(x.errorString, e.toString()), locale));
        }

        // round to 2 decimal places for display/etc
        taxTotal = taxTotal.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);
        priceWithTax = priceWithTax.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.taxTotal, taxTotal);
        result.put(x.taxPercentage, taxPercentage);
        result.put(x.priceWithTax, priceWithTax);
        return result;
    }

    public static Map<String, Object> rateProductTaxCalc(DispatchContext dctx, TaxAuthorityServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get(x.productStoreId);
        String facilityId = (String) context.get(x.facilityId);
        String payToPartyId = (String) context.get(x.payToPartyId);
        String billToPartyId = (String) context.get(x.billToPartyId);
        List<GenericValue> itemProductList = UtilGenerics.cast(context.get(x.itemProductList));
        List<BigDecimal> itemAmountList = UtilGenerics.cast(context.get(x.itemAmountList));
        List<BigDecimal> itemPriceList = UtilGenerics.cast(context.get(x.itemPriceList));
        List<BigDecimal> itemQuantityList = UtilGenerics.cast(context.get(x.itemQuantityList));
        List<BigDecimal> itemShippingList = UtilGenerics.cast(context.get(x.itemShippingList));
        BigDecimal orderShippingAmount = (BigDecimal) context.get(x.orderShippingAmount);
        BigDecimal orderPromotionsAmount = (BigDecimal) context.get(x.orderPromotionsAmount);
        GenericValue shippingAddress = (GenericValue) context.get(x.shippingAddress);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue productStore = null;
        GenericValue facility = null;
        try {
            if (productStoreId != null) {
                productStore = getProductStoreValue(delegator, productStoreId);
            }
            if (facilityId != null) {
                facility = getFacilityValue(delegator, facilityId);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Data_error_getting_tax_settings + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingTaxSettingError, UtilMisc
                    .toMap(x.errorString, e.toString()), locale));
        }

        if (productStore == null && payToPartyId == null) {
            throw new IllegalArgumentException(x.Could_not_find_payToPartyId_or_ProductStore_for_tax_calculation);
        }

        if (shippingAddress == null && facility != null) {
            // if there is no shippingAddress and there is a facility it means it is a
            // face-to-face sale so get facility's address
            try {
                GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator,
                        facilityId, UtilMisc.toList(x.SHIP_ORIG_LOCATION, x.PRIMARY_LOCATION));
                if (facilityContactMech != null) {
                    shippingAddress = getPostalAddressValue(delegator, (String) facilityContactMech.get(x.contactMechId));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Data_error_getting_tax_settings + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingTaxSettingError, UtilMisc
                        .toMap(x.errorString, e.toString()), locale));
            }
        }
        if (shippingAddress == null || (shippingAddress.get(x.countryGeoId) == null && shippingAddress.get(
                x.stateProvinceGeoId) == null && shippingAddress.get(x.postalCodeGeoId) == null)) {
            String errMsg = UtilProperties.getMessage(RESOURCE, x.AccountingTaxNoAddressSpecified, locale);
            if (shippingAddress != null) {
                errMsg += UtilProperties.getMessage(RESOURCE, x.AccountingTaxNoAddressSpecifiedDetails, UtilMisc.toMap(
                        x.contactMechId, shippingAddress.getString(x.contactMechId), x.address1, shippingAddress.get(
                                x.address1), x.postalCodeGeoId, shippingAddress.get(x.postalCodeGeoId),
                        x.stateProvinceGeoId, shippingAddress.get(x.stateProvinceGeoId), x.countryGeoId, shippingAddress
                                .get(x.countryGeoId)), locale);
                Debug.logError(errMsg, MODULE);
            }
            return ServiceUtil.returnError(errMsg);
        }

        // without knowing the TaxAuthority parties, just find all TaxAuthories for the
        // set of IDs...
        Set<GenericValue> taxAuthoritySet = new HashSet<>();
        try {
            getTaxAuthorities(delegator, shippingAddress, taxAuthoritySet);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Data_error_getting_tax_settings + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingTaxSettingError, UtilMisc
                    .toMap(x.errorString, e.toString()), locale));
        }

        // Setup the return lists.
        List<GenericValue> orderAdjustments = new LinkedList<>();
        List<List<GenericValue>> itemAdjustments = new LinkedList<>();

        BigDecimal totalPrice = ZERO_BASE;
        Map<GenericValue, BigDecimal> productWeight = new HashMap<>();
        // Loop through the products; get the taxCategory; and lookup each in the cache.
        for (int i = 0; i < itemProductList.size(); i++) {
            GenericValue product = itemProductList.get(i);
            BigDecimal itemAmount = itemAmountList.get(i);
            BigDecimal itemPrice = itemPriceList.get(i);
            BigDecimal itemQuantity = itemQuantityList != null ? itemQuantityList.get(i) : null;
            BigDecimal shippingAmount = itemShippingList != null ? itemShippingList.get(i) : null;

            totalPrice = totalPrice.add(itemAmount);

            List<GenericValue> taxList = getTaxAdjustments(delegator, product, productStore, payToPartyId,
                    billToPartyId, taxAuthoritySet, itemPrice, itemQuantity, itemAmount, shippingAmount, ZERO_BASE);

            // this is an add and not an addAll because we want a List of Lists of
            // GenericValues, one List of Adjustments per item
            itemAdjustments.add(taxList);

            //Calculates the TotalPrices for each Product in the Order
            BigDecimal currentTotalPrice = productWeight.getOrDefault(product, BigDecimal.ZERO);
            currentTotalPrice = currentTotalPrice.add(itemAmount);
            productWeight.put(product, currentTotalPrice);
        }
        // converts the totals of the products into percent weights
        for (GenericValue prod : productWeight.keySet()) {
            BigDecimal value = productWeight.get(prod);
            if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal weight = value.divide(totalPrice, 100, TAX_ROUNDING);
                productWeight.put(prod, weight);
            }
        }

        if (orderShippingAmount != null && orderShippingAmount.compareTo(BigDecimal.ZERO) > 0) {
            for (GenericValue prod : productWeight.keySet()) {
                List<GenericValue> taxList = getTaxAdjustments(delegator, prod, productStore, payToPartyId, billToPartyId,
                        taxAuthoritySet, ZERO_BASE, ZERO_BASE, ZERO_BASE, orderShippingAmount, null, productWeight.get(prod));
                orderAdjustments.addAll(taxList);
            }
        }
        if (orderPromotionsAmount != null && orderPromotionsAmount.compareTo(BigDecimal.ZERO) != 0) {
            List<GenericValue> taxList = getTaxAdjustments(delegator, null, productStore, payToPartyId, billToPartyId,
                    taxAuthoritySet, ZERO_BASE, ZERO_BASE, ZERO_BASE, null, orderPromotionsAmount);
            orderAdjustments.addAll(taxList);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.orderAdjustments, orderAdjustments);
        result.put(x.itemAdjustments, itemAdjustments);

        return result;
    }

    private static void getTaxAuthorities(Delegator delegator, GenericValue shippingAddress,
                                          Set<GenericValue> taxAuthoritySet) throws GenericEntityException {
        Map<String, String> geoIdByTypeMap = new HashMap<>();
        if (shippingAddress != null) {
            if (UtilValidate.isNotEmpty(shippingAddress.getString(x.countryGeoId))) {
                geoIdByTypeMap.put(x.COUNTRY, shippingAddress.getString(x.countryGeoId));
            }
            if (UtilValidate.isNotEmpty(shippingAddress.getString(x.stateProvinceGeoId))) {
                geoIdByTypeMap.put(x.STATE, shippingAddress.getString(x.stateProvinceGeoId));
            }
            if (UtilValidate.isNotEmpty(shippingAddress.getString(x.countyGeoId))) {
                geoIdByTypeMap.put(x.COUNTY, shippingAddress.getString(x.countyGeoId));
            }
            String postalCodeGeoId = ContactMechWorker.getPostalAddressPostalCodeGeoId(shippingAddress, delegator);
            if (UtilValidate.isNotEmpty(postalCodeGeoId)) {
                geoIdByTypeMap.put(x.POSTAL_CODE, postalCodeGeoId);
            }
        } else {
            Debug.logWarning(x.shippingAddress_was_null_adding_nothing_to_taxAuthoritySet, MODULE);
        }

        // get the most granular, or all available, geoIds and then find parents by
        // GeoAssoc with geoAssocTypeId="REGIONS" and geoIdTo=<granular geoId> and find
        // the GeoAssoc.geoId
        geoIdByTypeMap = GeoWorker.expandGeoRegionDeep(geoIdByTypeMap, delegator);

        List<GenericValue> taxAuthorityRawList = listTaxAuthorityValuesByGeoIds(delegator, geoIdByTypeMap.values());
        taxAuthoritySet.addAll(taxAuthorityRawList);
    }

    private static List<GenericValue> getTaxAdjustments(Delegator delegator, GenericValue product,
                                                        GenericValue productStore,
                                                        String payToPartyId, String billToPartyId, Set<GenericValue> taxAuthoritySet,
                                                        BigDecimal itemPrice, BigDecimal itemQuantity, BigDecimal itemAmount,
                                                        BigDecimal shippingAmount, BigDecimal orderPromotionsAmount) {
        return getTaxAdjustments(delegator, product, productStore, payToPartyId, billToPartyId,
                taxAuthoritySet, itemPrice, itemQuantity, itemAmount, shippingAmount,
                orderPromotionsAmount, null);
    }

    private static List<GenericValue> getTaxAdjustments(Delegator delegator, GenericValue product,
            GenericValue productStore,
            String payToPartyId, String billToPartyId, Set<GenericValue> taxAuthoritySet,
            BigDecimal itemPrice, BigDecimal itemQuantity, BigDecimal itemAmount,
            BigDecimal shippingAmount, BigDecimal orderPromotionsAmount, BigDecimal weight) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List<GenericValue> adjustments = new LinkedList<>();
        if (weight == null) {
            weight = BigDecimal.ONE;
        }

        if (payToPartyId == null) {
            if (productStore != null) {
                payToPartyId = productStore.getString(x.payToPartyId);
            }
        }

        // store expr
        EntityCondition storeCond = null;
        if (productStore != null) {
            storeCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(x.productStoreId, EntityOperator.EQUALS, productStore.get(
                            x.productStoreId)),
                    EntityOperator.OR,
                    EntityCondition.makeCondition(x.productStoreId, EntityOperator.EQUALS, null));
        } else {
            storeCond = EntityCondition.makeCondition(x.productStoreId, EntityOperator.EQUALS, null);
        }

        // build the TaxAuthority expressions (taxAuthGeoId, taxAuthPartyId)
        List<EntityCondition> taxAuthCondOrList = new LinkedList<>();
        // start with the _NA_ TaxAuthority...
        taxAuthCondOrList.add(EntityCondition.makeCondition(
                EntityCondition.makeCondition(x.taxAuthPartyId, EntityOperator.EQUALS, x.NA),
                EntityOperator.AND,
                EntityCondition.makeCondition(x.taxAuthGeoId, EntityOperator.EQUALS, x.NA)));

        for (GenericValue taxAuthority : taxAuthoritySet) {
            EntityCondition taxAuthCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(x.taxAuthPartyId, EntityOperator.EQUALS, taxAuthority.getString(
                            x.taxAuthPartyId)),
                    EntityOperator.AND,
                    EntityCondition.makeCondition(x.taxAuthGeoId, EntityOperator.EQUALS, taxAuthority.getString(
                            x.taxAuthGeoId)));
            taxAuthCondOrList.add(taxAuthCond);
        }
        EntityCondition taxAuthoritiesCond = EntityCondition.makeCondition(taxAuthCondOrList, EntityOperator.OR);

        try {
            EntityCondition productCategoryCond;
            productCategoryCond = setProductCategoryCond(delegator, product);

            if (product == null && shippingAmount != null) {
                EntityCondition taxShippingCond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition(x.taxShipping, EntityOperator.EQUALS, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(x.taxShipping, EntityOperator.EQUALS, x.Y));

                productCategoryCond = EntityCondition.makeCondition(productCategoryCond, EntityOperator.OR,
                        taxShippingCond);
            }

            if (product == null && orderPromotionsAmount != null) {
                EntityCondition taxOrderPromotionsCond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition(x.taxPromotions, EntityOperator.EQUALS, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(x.taxPromotions, EntityOperator.EQUALS, x.Y));

                productCategoryCond = EntityCondition.makeCondition(productCategoryCond, EntityOperator.OR,
                        taxOrderPromotionsCond);
            }

            // build the main condition clause
            List<EntityCondition> mainExprs = UtilMisc.toList(storeCond, taxAuthoritiesCond, productCategoryCond);
            mainExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.minItemPrice,
                    EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.minItemPrice,
                            EntityOperator.LESS_THAN_EQUAL_TO, itemPrice)));
            mainExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.minPurchase,
                    EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.minPurchase,
                            EntityOperator.LESS_THAN_EQUAL_TO, itemAmount)));
            EntityCondition mainCondition = EntityCondition.makeCondition(mainExprs, EntityOperator.AND);

            // finally ready... do the rate query
            List<GenericValue> lookupList = listCurrentTaxAuthorityRateProductValues(delegator, mainCondition);

            if (lookupList.isEmpty()) {
                Debug.logWarning(x.In_TaxAuthority_Product_Rate_no_records_were_found_for_condition + mainCondition.toString(), MODULE);
                return adjustments;
            }

            // find the right entry(s) based on purchase amount
            for (GenericValue taxAuthorityRateProduct : lookupList) {
                BigDecimal taxRate = taxAuthorityRateProduct.get(x.taxPercentage) != null ? taxAuthorityRateProduct
                        .getBigDecimal(x.taxPercentage) : ZERO_BASE;
                taxRate = taxRate.multiply(weight);
                BigDecimal taxable = ZERO_BASE;

                if (product != null && (product.get(x.taxable) == null || (product.get(x.taxable) != null && product
                        .getBoolean(x.taxable)))) {
                    taxable = taxable.add(itemAmount);
                }
                if (shippingAmount != null && (taxAuthorityRateProduct.get(x.taxShipping) == null
                        || (taxAuthorityRateProduct.get(x.taxShipping) != null && taxAuthorityRateProduct.getBoolean(
                        x.taxShipping)))) {
                    taxable = taxable.add(shippingAmount);
                }
                if (orderPromotionsAmount != null && (taxAuthorityRateProduct.get(x.taxPromotions) == null
                        || (taxAuthorityRateProduct.get(x.taxPromotions) != null && taxAuthorityRateProduct.getBoolean(
                        x.taxPromotions)))) {
                    taxable = taxable.add(orderPromotionsAmount);
                }

                if (taxable.compareTo(BigDecimal.ZERO) == 0) {
                    // this should make it less confusing if the taxable flag on the product is not
                    // Y/true, and there is no shipping and such
                    continue;
                }

                // taxRate is in percentage, so needs to be divided by 100
                BigDecimal taxAmount = (taxable.multiply(taxRate)).divide(PERCENT_SCALE, TAX_SCALE,
                        TAX_ROUNDING);

                String taxAuthGeoId = taxAuthorityRateProduct.getString(x.taxAuthGeoId);
                String taxAuthPartyId = taxAuthorityRateProduct.getString(x.taxAuthPartyId);

                // get glAccountId from TaxAuthorityGlAccount entity using the payToPartyId as
                // the organizationPartyId
                GenericValue taxAuthorityGlAccount = getTaxAuthorityGlAccountValue(delegator, taxAuthPartyId, taxAuthGeoId, payToPartyId);
                String taxAuthGlAccountId = null;
                if (taxAuthorityGlAccount != null) {
                    taxAuthGlAccountId = taxAuthorityGlAccount.getString(x.glAccountId);
                } else {
                    // TODO: what to do if no TaxAuthorityGlAccount found? Use some default, or is that done elsewhere later on?
                    Debug.logVerbose(x.what_to_do_if_no_TaxAuthorityGlAccount_found, MODULE);
                }

                GenericValue productPrice = null;
                if (product != null && taxAuthPartyId != null && taxAuthGeoId != null) {
                    // find a ProductPrice for the productId and taxAuth* values, and see if it has
                    // a priceWithTax value
                    productPrice = getProductPrice(delegator, product, productStore, taxAuthGeoId, taxAuthPartyId);
                    if (productPrice == null) {
                        GenericValue virtualProduct = ProductWorker.getParentProduct(product.getString(x.productId), delegator);
                        if (virtualProduct != null) {
                            productPrice = getProductPrice(delegator, virtualProduct, productStore, taxAuthGeoId, taxAuthPartyId);
                        }
                    }
                }
                GenericValue taxAdjValue = delegator.makeValue(x.OrderAdjustment);

                BigDecimal discountedSalesTax = BigDecimal.ZERO;
                taxAdjValue.set(x.orderAdjustmentTypeId, x.SALES_TAX);
                if (productPrice != null && x.Y.equals(productPrice.getString(x.taxInPrice))
                        && itemQuantity != BigDecimal.ZERO) {
                    // For example product price is 43 with 20% VAT(means product actual price is
                    // 35.83).
                    // itemPrice = 43;
                    // itemQuantity = 3;
                    // taxAmountIncludedInFullPrice = (43-(43/(1+(20/100))))*3 = 21.51
                    taxAdjValue.set(x.orderAdjustmentTypeId, x.VAT_TAX);
                    BigDecimal taxAmountIncludedInFullPrice = itemPrice.subtract(itemPrice.divide(BigDecimal.ONE.add(
                            taxRate.divide(PERCENT_SCALE, 4, RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP)).multiply(
                                    itemQuantity);
                    // If 1 quantity has 50% discount then itemAmount = 107.5 otherwise 129 (In case
                    // of no discount)
                    // Net price for each item
                    // netItemPrice = itemAmount / quantity = 107.5 / 3 = 35.833333333
                    BigDecimal netItemPrice = itemAmount.divide(itemQuantity, RoundingMode.HALF_UP);
                    // Calculate tax on the discounted price, be sure to round to 2 decimal places
                    // before multiplying by quantity
                    // netTax = (netItemPrice - netItemPrice / (1 + (taxRate/100))) * quantity
                    // netTax = (35.833333333-(35.833333333/(1+(20/100))))*3 = 17.92
                    BigDecimal netTax = netItemPrice.subtract(netItemPrice.divide(BigDecimal.ONE.add(taxRate.divide(
                            PERCENT_SCALE, 4, RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP)).multiply(itemQuantity);
                    // Subtract net tax from base tax (taxAmountIncludedFullPrice) to get the
                    // negative promotion tax adjustment amount
                    // discountedSalesTax = 17.92 - 21.51 = −3.59 (If no discounted item quantity
                    // then discountedSalesTax will be ZERO)
                    discountedSalesTax = netTax.subtract(taxAmountIncludedInFullPrice);
                    taxAdjValue.set(x.amountAlreadyIncluded, taxAmountIncludedInFullPrice);
                    taxAdjValue.set(x.amount, BigDecimal.ZERO);
                } else {
                    taxAdjValue.set(x.amount, taxAmount);
                }

                taxAdjValue.set(x.sourcePercentage, taxRate);
                taxAdjValue.set(x.taxAuthorityRateSeqId, taxAuthorityRateProduct.getString(x.taxAuthorityRateSeqId));
                // the primary Geo should be the main jurisdiction that the tax is for, and the
                // secondary would just be to define a parent or wrapping jurisdiction of the
                // primary
                taxAdjValue.set(x.primaryGeoId, taxAuthGeoId);
                taxAdjValue.set(x.comments, taxAuthorityRateProduct.getString(x.description));
                if (taxAuthPartyId != null) {
                    taxAdjValue.set(x.taxAuthPartyId, taxAuthPartyId);
                }
                if (taxAuthGlAccountId != null) {
                    taxAdjValue.set(x.overrideGlAccountId, taxAuthGlAccountId);
                }
                if (taxAuthGeoId != null) {
                    taxAdjValue.set(x.taxAuthGeoId, taxAuthGeoId);
                }

                // check to see if this party has a tax ID for this, and if the party is tax
                // exempt in the primary (most-local) jurisdiction
                if (UtilValidate.isNotEmpty(billToPartyId) && UtilValidate.isNotEmpty(taxAuthGeoId)) {
                    // see if partyId is a member of any groups, if so honor their tax exemptions
                    // look for PartyRelationship with partyRelationshipTypeId=GROUP_ROLLUP, the
                    // partyIdTo is the group member, so the partyIdFrom is the groupPartyId
                    Set<String> billToPartyIdSet = new HashSet<>();
                    billToPartyIdSet.add(billToPartyId);
                    List<GenericValue> partyRelationshipList = listCurrentPartyRelationshipValues(delegator, billToPartyId);

                    for (GenericValue partyRelationship : partyRelationshipList) {
                        billToPartyIdSet.add(partyRelationship.getString(x.partyIdFrom));
                    }
                    handlePartyTaxExempt(taxAdjValue, billToPartyIdSet, taxAuthGeoId, taxAuthPartyId, taxAmount,
                            nowTimestamp, delegator);
                } else {
                    Debug.logInfo(x.NOTE_A_tax_calculation_was_done_without_a_billToPartyId_or_taxAuthGeoId_so_no_tax_exemptions_or_tax_IDs
                            + x.considered_billToPartyId + billToPartyId + x.taxAuthGeoId_43a94674 + taxAuthGeoId + x.str_4ff447b8, MODULE);
                }
                if (discountedSalesTax.compareTo(BigDecimal.ZERO) < 0) {
                    GenericValue taxAdjValueNegative = delegator.makeValue(x.OrderAdjustment);
                    taxAdjValueNegative.setFields(taxAdjValue);
                    taxAdjValueNegative.set(x.amountAlreadyIncluded, discountedSalesTax);
                    adjustments.add(taxAdjValueNegative);
                }
                adjustments.add(taxAdjValue);

                if (productPrice != null && itemQuantity != null
                        && productPrice.getBigDecimal(x.priceWithTax) != null
                        && !x.Y.equals(productPrice.getString(x.taxInPrice))) {
                    BigDecimal priceWithTax = productPrice.getBigDecimal(x.priceWithTax);
                    BigDecimal price = productPrice.getBigDecimal(x.price);
                    BigDecimal baseSubtotal = price.multiply(itemQuantity);
                    BigDecimal baseTaxAmount = (baseSubtotal.multiply(taxRate)).divide(PERCENT_SCALE,
                            TAX_SCALE, TAX_ROUNDING);

                    // tax is not already in price so we want to add it in, but this is a VAT
                    // situation so adjust to make it as accurate as possible

                    // for VAT taxes if the calculated total item price plus calculated taxes is
                    // different from what would be
                    // expected based on the original entered price with taxes (if the price was
                    // entered this way), then create
                    // an adjustment that corrects for the difference, and this correction will be
                    // effectively subtracted from the
                    // price and not from the tax (the tax is meant to be calculated based on Tax
                    // Authority rules and so should
                    // not be shorted)

                    // TODO (don't think this is needed, but just to keep it in mind): get this to
                    // work with multiple VAT tax authorities instead of just one (right now will
                    // get incorrect totals if there are multiple taxes included in the price)
                    // TODO add constraint to ProductPrice lookup by any productStoreGroupId
                    // associated with the current productStore

                    BigDecimal enteredTotalPriceWithTax = priceWithTax.multiply(itemQuantity);
                    BigDecimal calcedTotalPriceWithTax = (baseSubtotal).add(baseTaxAmount);
                    if (!enteredTotalPriceWithTax.equals(calcedTotalPriceWithTax)) {
                        // if the calculated amount is higher than the entered amount we want the value
                        // to be negative
                        // to get it down to match the entered amount
                        // so, subtract the calculated amount from the entered amount (ie: correction =
                        // entered - calculated)
                        BigDecimal correctionAmount = enteredTotalPriceWithTax.subtract(calcedTotalPriceWithTax);

                        GenericValue correctionAdjValue = delegator.makeValue(x.OrderAdjustment);
                        correctionAdjValue.set(x.taxAuthorityRateSeqId, taxAuthorityRateProduct.getString(
                                x.taxAuthorityRateSeqId));
                        correctionAdjValue.set(x.amount, correctionAmount);
                        // don't set this, causes a doubling of the tax rate because calling code adds
                        // up all tax rates: correctionAdjValue.set("sourcePercentage", taxRate);
                        correctionAdjValue.set(x.orderAdjustmentTypeId, x.VAT_PRICE_CORRECT);
                        // the primary Geo should be the main jurisdiction that the tax is for, and the
                        // secondary would just be to define a parent or wrapping jurisdiction of the
                        // primary
                        correctionAdjValue.set(x.primaryGeoId, taxAuthGeoId);
                        correctionAdjValue.set(x.comments, taxAuthorityRateProduct.getString(x.description));
                        if (taxAuthPartyId != null) {
                            correctionAdjValue.set(x.taxAuthPartyId, taxAuthPartyId);
                        }
                        if (taxAuthGlAccountId != null) {
                            correctionAdjValue.set(x.overrideGlAccountId, taxAuthGlAccountId);
                        }
                        if (taxAuthGeoId != null) {
                            correctionAdjValue.set(x.taxAuthGeoId, taxAuthGeoId);
                        }
                        adjustments.add(correctionAdjValue);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problems_looking_up_tax_rates, MODULE);
            return new LinkedList<>();
        }

        return adjustments;
    }

    /**
     * @param delegator
     * @param product
     * @param productStore
     * @param taxAuthGeoId
     * @param taxAuthPartyId
     * @return productPrice
     * @throws GenericEntityException
     */
    private static GenericValue getProductPrice(Delegator delegator, GenericValue product, GenericValue productStore, String taxAuthGeoId,
            String taxAuthPartyId) throws GenericEntityException {
        if (productStore != null && UtilValidate.isNotEmpty(productStore.getString(x.primaryStoreGroupId))) {
            return getCurrentProductPriceValue(delegator, (String) product.get(x.productId), taxAuthPartyId, taxAuthGeoId, (String) productStore.get(x.primaryStoreGroupId));
        } else {
            // Purchase order case
            return getCurrentProductPriceValue(delegator, (String) product.get(x.productId), taxAuthPartyId, taxAuthGeoId, null);
        }
    }

    /**
     * Private helper method which determines, based on the state of the product, how the ProdCondition should be set for the main condition.
     * @param delegator
     * @param product which may be null
     * @return non-null Condition
     * @throws GenericEntityException
     */
    private static EntityCondition setProductCategoryCond(Delegator delegator, GenericValue product)
            throws GenericEntityException {

        if (product == null) {
            return EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS, null);
        }

        // find the tax categories associated with the product and filter by
        // those, with an IN clause or some such
        // if this product is variant, find the virtual product id and consider
        // also the categories of the virtual
        // question: get all categories, or just a special type? for now let's
        // do all categories...
        String virtualProductId = null;
        if (x.Y.equals(product.getString(x.isVariant))) {
            virtualProductId = ProductWorker.getVariantVirtualId(product);
        }
        Set<String> productCategoryIdSet = new HashSet<>();
        EntityCondition productIdCond = null;
        if (virtualProductId != null) {
            productIdCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, product.getString(x.productId)),
                    EntityOperator.OR,
                    EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, virtualProductId));

        } else {
            productIdCond = EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS,
                    product.getString(x.productId));
        }
        List<GenericValue> pcmList = listCurrentProductCategoryMemberValues(delegator, productIdCond);
        for (GenericValue pcm : pcmList) {
            productCategoryIdSet.add(pcm.getString(x.productCategoryId));
        }

        if (productCategoryIdSet.isEmpty()) {
            return EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS, null);
        }
        return EntityCondition.makeCondition(
                EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS, null), EntityOperator.OR,
                EntityCondition.makeCondition(x.productCategoryId, EntityOperator.IN, productCategoryIdSet));

    }

    private static GenericValue getProductValue(Delegator delegator, String productId) throws GenericEntityException {
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        ProductEntity productEntity = withSqlException(x.load_Product_for_productId + productId + x.str_4ff447b8,
                () -> productDao.get(productId).orElse(null));
        return productEntity == null ? null : delegator.makeValue(x.Product, Beans.beanToMap(productEntity));
    }

    private static GenericValue getProductStoreValue(Delegator delegator, String productStoreId) throws GenericEntityException {
        ProductStoreDao productStoreDao = DaoRegistry.getDao(delegator, x.ProductStore, ProductStoreDao.class);
        ProductStoreEntity productStoreEntity = withSqlException(x.load_ProductStore_for_productStoreId + productStoreId + x.str_4ff447b8,
                () -> productStoreDao.get(productStoreId).orElse(null));
        return productStoreEntity == null ? null : delegator.makeValue(x.ProductStore, Beans.beanToMap(productStoreEntity));
    }

    private static List<GenericValue> listTaxAuthorityValuesByGeoId(Delegator delegator, String taxAuthGeoId) throws GenericEntityException {
        TaxAuthorityDao taxAuthorityDao = DaoRegistry.getDao(delegator, x.TaxAuthority, TaxAuthorityDao.class);
        List<TaxAuthorityEntity> taxAuthorityEntities = withSqlException(x.list_TaxAuthority_by_taxAuthGeoId + taxAuthGeoId + x.str_4ff447b8,
                () -> taxAuthorityDao.list(Filters.eq(x.taxAuthGeoId, taxAuthGeoId)));
        List<GenericValue> values = new LinkedList<>();
        for (TaxAuthorityEntity taxAuthorityEntity : taxAuthorityEntities) {
            values.add(delegator.makeValue(x.TaxAuthority, Beans.beanToMap(taxAuthorityEntity)));
        }
        return values;
    }

    private static GenericValue getTaxAuthorityValue(Delegator delegator, String taxAuthGeoId, String taxAuthPartyId)
            throws GenericEntityException {
        TaxAuthorityDao taxAuthorityDao = DaoRegistry.getDao(delegator, x.TaxAuthority, TaxAuthorityDao.class);
        TaxAuthorityEntity taxAuthorityEntity = withSqlException(x.load_TaxAuthority_for_taxAuthGeoId + taxAuthGeoId + x.and_taxAuthPartyId
                + taxAuthPartyId + x.str_4ff447b8, () -> taxAuthorityDao.list(Filters.and(
                Filters.eq(x.taxAuthGeoId, taxAuthGeoId),
                Filters.eq(x.taxAuthPartyId, taxAuthPartyId))).stream().findFirst().orElse(null));
        return taxAuthorityEntity == null ? null : delegator.makeValue(x.TaxAuthority, Beans.beanToMap(taxAuthorityEntity));
    }

    private static GenericValue getFacilityValue(Delegator delegator, String facilityId) throws GenericEntityException {
        FacilityDao facilityDao = DaoRegistry.getDao(delegator, x.Facility, FacilityDao.class);
        FacilityEntity facilityEntity = withSqlException(x.load_Facility_for_facilityId + facilityId + x.str_4ff447b8,
                () -> facilityDao.get(facilityId).orElse(null));
        return facilityEntity == null ? null : delegator.makeValue(x.Facility, Beans.beanToMap(facilityEntity));
    }

    private static GenericValue getPostalAddressValue(Delegator delegator, String contactMechId) throws GenericEntityException {
        PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
        PostalAddressEntity postalAddressEntity = withSqlException(x.load_PostalAddress_for_contactMechId + contactMechId + x.str_4ff447b8,
                () -> postalAddressDao.get(contactMechId).orElse(null));
        return postalAddressEntity == null ? null : delegator.makeValue(x.PostalAddress, Beans.beanToMap(postalAddressEntity));
    }

    private static List<GenericValue> listTaxAuthorityValuesByGeoIds(Delegator delegator, java.util.Collection<?> geoIds)
            throws GenericEntityException {
        TaxAuthorityDao taxAuthorityDao = DaoRegistry.getDao(delegator, x.TaxAuthority, TaxAuthorityDao.class);
        List<TaxAuthorityEntity> taxAuthorityEntities = withSqlException(x.list_TaxAuthority_by_taxAuthGeoIds,
                () -> taxAuthorityDao.list(Filters.in(x.taxAuthGeoId, geoIds)));
        List<GenericValue> values = new LinkedList<>();
        for (TaxAuthorityEntity taxAuthorityEntity : taxAuthorityEntities) {
            values.add(delegator.makeValue(x.TaxAuthority, Beans.beanToMap(taxAuthorityEntity)));
        }
        return values;
    }

    private static List<GenericValue> listCurrentTaxAuthorityRateProductValues(Delegator delegator, EntityCondition condition)
            throws GenericEntityException {
        TaxAuthorityRateProductDao taxAuthorityRateProductDao = DaoRegistry.getDao(delegator, x.TaxAuthorityRateProduct,
                TaxAuthorityRateProductDao.class);
        return taxAuthorityRateProductDao.listByCondition(delegator, condition, UtilMisc.toList(x.minItemPrice, x.minPurchase, x.fromDate), true);
    }

    private static GenericValue getTaxAuthorityGlAccountValue(Delegator delegator, String taxAuthPartyId, String taxAuthGeoId,
            String organizationPartyId) throws GenericEntityException {
        TaxAuthorityGlAccountDao taxAuthorityGlAccountDao = DaoRegistry.getDao(delegator, x.TaxAuthorityGlAccount, TaxAuthorityGlAccountDao.class);
        TaxAuthorityGlAccountEntity taxAuthorityGlAccountEntity = withSqlException(x.load_TaxAuthorityGlAccount, () ->
                taxAuthorityGlAccountDao.list(Filters.and(
                        Filters.eq(x.taxAuthPartyId, taxAuthPartyId),
                        Filters.eq(x.taxAuthGeoId, taxAuthGeoId),
                        Filters.eq(x.organizationPartyId, organizationPartyId))).stream().findFirst().orElse(null));
        return taxAuthorityGlAccountEntity == null ? null : delegator.makeValue(x.TaxAuthorityGlAccount, Beans.beanToMap(taxAuthorityGlAccountEntity));
    }

    private static List<GenericValue> listCurrentPartyRelationshipValues(Delegator delegator, String partyIdTo)
            throws GenericEntityException {
        PartyRelationshipDao partyRelationshipDao = DaoRegistry.getDao(delegator, x.PartyRelationship, PartyRelationshipDao.class);
        List<PartyRelationshipEntity> partyRelationshipEntities = withSqlException(
                x.list_current_PartyRelationship_for_partyIdTo + partyIdTo + x.str_4ff447b8, () -> partyRelationshipDao.list(Filters.and(
                        Filters.eq(x.partyIdTo, partyIdTo),
                        Filters.eq(x.partyRelationshipTypeId, x.GROUP_ROLLUP))));
        List<GenericValue> values = new LinkedList<>();
        for (PartyRelationshipEntity partyRelationshipEntity : partyRelationshipEntities) {
            values.add(delegator.makeValue(x.PartyRelationship, Beans.beanToMap(partyRelationshipEntity)));
        }
        return EntityUtil.filterByDate(values);
    }

    private static GenericValue getCurrentProductPriceValue(Delegator delegator, String productId, String taxAuthPartyId, String taxAuthGeoId,
            String productStoreGroupId) throws GenericEntityException {
        ProductPriceDao productPriceDao = DaoRegistry.getDao(delegator, x.ProductPrice, ProductPriceDao.class);
        List<com.landawn.abacus.query.condition.Condition> conditions = new LinkedList<>();
        conditions.add(Filters.eq(x.productId, productId));
        conditions.add(Filters.eq(x.taxAuthPartyId, taxAuthPartyId));
        conditions.add(Filters.eq(x.taxAuthGeoId, taxAuthGeoId));
        conditions.add(Filters.eq(x.productPricePurposeId, x.PURCHASE));
        if (UtilValidate.isNotEmpty(productStoreGroupId)) {
            conditions.add(Filters.eq(x.productStoreGroupId, productStoreGroupId));
        }
        List<ProductPriceEntity> productPriceEntities = withSqlException(x.list_ProductPrice_for_productId + productId + x.str_4ff447b8,
                () -> productPriceDao.list(Filters.and(conditions)));
        List<GenericValue> values = new LinkedList<>();
        for (ProductPriceEntity productPriceEntity : productPriceEntities) {
            values.add(delegator.makeValue(x.ProductPrice, Beans.beanToMap(productPriceEntity)));
        }
        values = EntityUtil.filterByDate(values);
        values = EntityUtil.orderBy(values, UtilMisc.toList(x.fromDate_f5440273));
        return EntityUtil.getFirst(values);
    }

    private static List<GenericValue> listCurrentProductCategoryMemberValues(Delegator delegator, EntityCondition productIdCond)
            throws GenericEntityException {
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember, ProductCategoryMemberDao.class);
        List<ProductCategoryMemberEntity> productCategoryMemberEntities = withSqlException(x.list_ProductCategoryMember_values,
                () -> productCategoryMemberDao.list(Filters.in(x.productId, getProductIdsFromCondition(productIdCond))));
        List<GenericValue> values = new LinkedList<>();
        for (ProductCategoryMemberEntity productCategoryMemberEntity : productCategoryMemberEntities) {
            GenericValue value = delegator.makeValue(x.ProductCategoryMember, Beans.beanToMap(productCategoryMemberEntity));
            value.set(x.productCategoryId, productCategoryMemberEntity.getProductCategoryId());
            values.add(value);
        }
        return EntityUtil.filterByDate(values);
    }

    private static Set<String> getProductIdsFromCondition(EntityCondition productIdCond) {
        Set<String> productIds = new HashSet<>();
        String condString = String.valueOf(productIdCond);
        for (String token : condString.split(x.str_bb589d06)) {
            if (token.contains(x.str_b858cb28) || token.contains(x.str_21606782) || token.contains(x.str_1e5c2f36) || token.contains(x.str_4ff447b8)) {
                continue;
            }
            if (token.startsWith(x.WG) || token.startsWith(x.DEM) || token.startsWith(x.PROD) || token.startsWith(x.TEST) || token.startsWith(x.str_53a0acfa)) {
                productIds.add(token);
            }
        }
        return productIds;
    }

    private static GenericValue getLatestPartyTaxInfoValue(Delegator delegator, EntityCondition ptiCondition) throws GenericEntityException {
        PartyTaxAuthInfoDao partyTaxAuthInfoDao = DaoRegistry.getDao(delegator, x.PartyTaxAuthInfo, PartyTaxAuthInfoDao.class);
        return partyTaxAuthInfoDao.queryFirstByCondition(delegator, ptiCondition, UtilMisc.toList(x.fromDate_f5440273));
    }

    private static GenericValue getCurrentTaxAuthorityAssocValue(Delegator delegator, String toTaxAuthGeoId, String toTaxAuthPartyId)
            throws GenericEntityException {
        TaxAuthorityAssocDao taxAuthorityAssocDao = DaoRegistry.getDao(delegator, x.TaxAuthorityAssoc, TaxAuthorityAssocDao.class);
        List<TaxAuthorityAssocEntity> taxAuthorityAssocEntities = withSqlException(
                x.list_TaxAuthorityAssoc_for_toTaxAuthGeoId + toTaxAuthGeoId + x.and_toTaxAuthPartyId + toTaxAuthPartyId + x.str_4ff447b8,
                () -> taxAuthorityAssocDao.list(Filters.and(
                        Filters.eq(x.toTaxAuthGeoId, toTaxAuthGeoId),
                        Filters.eq(x.toTaxAuthPartyId, toTaxAuthPartyId),
                        Filters.eq(x.taxAuthorityAssocTypeId, x.EXEMPT_INHER))));
        List<GenericValue> values = new LinkedList<>();
        for (TaxAuthorityAssocEntity taxAuthorityAssocEntity : taxAuthorityAssocEntities) {
            values.add(delegator.makeValue(x.TaxAuthorityAssoc, Beans.beanToMap(taxAuthorityAssocEntity)));
        }
        values = EntityUtil.filterByDate(values);
        values = EntityUtil.orderBy(values, UtilMisc.toList(x.fromDate_f5440273));
        return EntityUtil.getFirst(values);
    }

    private static void handlePartyTaxExempt(GenericValue adjValue, Set<String> billToPartyIdSet, String taxAuthGeoId,
            String taxAuthPartyId, BigDecimal taxAmount, Timestamp nowTimestamp, Delegator delegator)
            throws GenericEntityException {
        Debug.logInfo(x.Checking_for_tax_exemption + taxAuthGeoId + x.str_0d0c4ddd + taxAuthPartyId, MODULE);
        List<EntityCondition> ptiConditionList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(x.partyId, EntityOperator.IN, billToPartyIdSet),
                EntityCondition.makeCondition(x.taxAuthGeoId, EntityOperator.EQUALS, taxAuthGeoId),
                EntityCondition.makeCondition(x.taxAuthPartyId, EntityOperator.EQUALS, taxAuthPartyId));
        ptiConditionList.add(EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO,
                nowTimestamp));
        ptiConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate,
                EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.thruDate,
                        EntityOperator.GREATER_THAN, nowTimestamp)));
        EntityCondition ptiCondition = EntityCondition.makeCondition(ptiConditionList, EntityOperator.AND);
        // sort by -fromDate to get the newest (largest) first, just in case there is
        // more than one, we only want the most recent valid one, should only be one per
        // jurisdiction...
        GenericValue partyTaxInfo = getLatestPartyTaxInfoValue(delegator, ptiCondition);

        boolean foundExemption = false;
        if (partyTaxInfo != null) {
            adjValue.set(x.customerReferenceId, partyTaxInfo.get(x.partyTaxId));
            if (x.Y.equals(partyTaxInfo.getString(x.isExempt))) {
                adjValue.set(x.amount, BigDecimal.ZERO);
                adjValue.set(x.exemptAmount, taxAmount);
                foundExemption = true;
            }
        }

        // if no exceptions were found for the current; try the parent
        if (!foundExemption) {
            // try the "parent" TaxAuthority
            GenericValue taxAuthorityAssoc = getCurrentTaxAuthorityAssocValue(delegator, taxAuthGeoId, taxAuthPartyId);
            if (taxAuthorityAssoc != null) {
                handlePartyTaxExempt(adjValue, billToPartyIdSet, taxAuthorityAssoc.getString(x.taxAuthGeoId),
                        taxAuthorityAssoc.getString(x.taxAuthPartyId), taxAmount, nowTimestamp, delegator);
            }
        }
    }
}

