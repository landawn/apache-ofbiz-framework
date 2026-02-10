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
package org.apache.ofbiz.product.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import com.landawn.abacus.jdbc.dao.Dao;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.model.CalculateProductPriceContext;
import org.apache.ofbiz.model.CalculatePurchasePriceContext;
import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;
import org.apache.ofbiz.persistence.entity.x;
/**
 * PriceServices - Workers and Services class for product price related functionality
 */
public class PriceServices {

    private static final String MODULE = PriceServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final BigDecimal ONE_BASE = BigDecimal.ONE;
    private static final BigDecimal PERCENT_SCALE = new BigDecimal(x._100_000);

    private static final int TAX_SCALE = UtilNumber.getBigDecimalScale(x.salestax_calc_decimals);
    private static final int TAX_FINAL_SCALE = UtilNumber.getBigDecimalScale(x.salestax_final_decimals);
    private static final RoundingMode TAX_ROUNDING = UtilNumber.getRoundingMode(x.salestax_rounding);

    /**
     * <p>Calculates the price of a product from pricing rules given the following input, and of course access to the database:</p>
     * <ul>
     *   <li>productId
     *   <li>productCategoryId
     *   <li>partyId
     *   <li>partyClassificationGroupId
     *   <li>prodCatalogId
     *   <li>webSiteId
     *   <li>productStoreId
     *   <li>productStoreGroupId
     *   <li>agreementId
     *   <li>quantity
     *   <li>currencyUomId
     *   <li>checkIncludeVat
     * </ul>
     */
    public static Map<String, Object> calculateProductPrice(DispatchContext dctx, CalculateProductPriceContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<>();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        ProductEntity product = context.getProduct();
        GenericValue productGenericValue = null;
        GenericValue userLoginGenericValue = null;
        String productId = product.getProductId();
        String prodCatalogId = context.getProdCatalogId();
        String webSiteId = context.getWebSiteId();
        String checkIncludeVat = context.getCheckIncludeVat();
        String surveyResponseId = context.getSurveyResponseId();
        Map<String, Object> customAttributes = context.getCustomAttributes();

        String findAllQuantityPricesStr = context.getFindAllQuantityPrices();
        boolean findAllQuantityPrices = x.Y.equals(findAllQuantityPricesStr);
        boolean optimizeForLargeRuleSet = x.Y.equals(context.getOptimizeForLargeRuleSet());

        String agreementId = context.getAgreementId();

        String productStoreId = context.getProductStoreId();
        String productStoreGroupId = context.getProductStoreGroupId();
        Locale locale = context.getLocale();

        GenericValue productStore = null;
        try {
            // we have a productStoreId, if the corresponding ProductStore.primaryStoreGroupId is not empty, use that
            productStore = DaoQuery.use(delegator).from(x.ProductStore).where(x.productStoreId, productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_getting_product_store_info_from_the_database_while_calculating_price + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPriceCannotRetrieveProductStore, UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        if (UtilValidate.isEmpty(productStoreGroupId)) {
            if (productStore != null) {
                try {
                    if (UtilValidate.isNotEmpty(productStore.getString(x.primaryStoreGroupId))) {
                        productStoreGroupId = productStore.getString(x.primaryStoreGroupId);
                    } else {
                        // no ProductStore.primaryStoreGroupId, try ProductStoreGroupMember
                        List<GenericValue> productStoreGroupMemberList = DaoQuery.use(delegator).from(x.ProductStoreGroupMember)
                                .where(x.productStoreId, productStoreId).orderBy(x.sequenceNum, x.fromDate_f5440273).cache(true).queryList();
                        productStoreGroupMemberList = EntityUtil.filterByDate(productStoreGroupMemberList, true);
                        if (!productStoreGroupMemberList.isEmpty()) {
                            GenericValue productStoreGroupMember = EntityUtil.getFirst(productStoreGroupMemberList);
                            productStoreGroupId = productStoreGroupMember.getString(x.productStoreGroupId);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Error_getting_product_store_info_from_the_database_while_calculating_price + e.toString(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductPriceCannotRetrieveProductStore, UtilMisc.toMap(x.errorString, e.toString()), locale));
                }
            }

            // still empty, default to _NA_
            if (UtilValidate.isEmpty(productStoreGroupId)) {
                productStoreGroupId = x.NA;
            }
        }

        // if currencyUomId is null get from properties file, if nothing there assume USD (USD: American Dollar) for now
        String currencyDefaultUomId = context.getCurrencyUomId();
        String currencyUomIdTo = context.getCurrencyUomIdTo();
        if (UtilValidate.isEmpty(currencyDefaultUomId)) {
            if (productStore != null && UtilValidate.isNotEmpty(productStore.getString(x.defaultCurrencyUomId))) {
                currencyDefaultUomId = productStore.getString(x.defaultCurrencyUomId);
            } else {
                currencyDefaultUomId = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
            }
        }

        // productPricePurposeId is null assume "PURCHASE", which is equivalent to what prices were before the purpose concept
        String productPricePurposeId = context.getProductPricePurposeId();
        if (UtilValidate.isEmpty(productPricePurposeId)) {
            productPricePurposeId = x.PURCHASE;
        }

        // termUomId, for things like recurring prices specifies the term (time/frequency measure for example) of the recurrence
        // if this is empty it will simply not be used to constrain the selection
        String termUomId = context.getTermUomId();

        // if this product is variant, find the virtual product and apply checks to it as well
        String virtualProductId = null;
        if (x.Y.equals(product.getIsVariant())) {
            if (productGenericValue == null) {
                try {
                    productGenericValue = DaoQuery.use(delegator).from(x.Product)
                            .where(x.productId, productId).cache().queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Error_getting_product_from_the_database_while_calculating_price + e.toString(), MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            try {
                virtualProductId = ProductWorker.getVariantVirtualId(productGenericValue);
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Error_getting_virtual_product_id_from_the_database_while_calculating_price + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotRetrieveVirtualProductId, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        // get prices for virtual product if one is found; get all ProductPrice entities for this productId and currencyUomId
        List<GenericValue> virtualProductPrices = null;
        if (virtualProductId != null) {
            try {
                virtualProductPrices = DaoQuery.use(delegator).from(x.ProductPrice).where(x.productId, virtualProductId, x.currencyUomId,
                        currencyDefaultUomId, x.productStoreGroupId, productStoreGroupId).orderBy(x.fromDate_f5440273).cache(true).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, x.An_error_occurred_while_getting_the_product_prices, MODULE);
            }
            virtualProductPrices = EntityUtil.filterByDate(virtualProductPrices, true);
        }

        // NOTE: partyId CAN be null
        String partyId = context.getPartyId();
        if (UtilValidate.isEmpty(partyId) && context.getUserLogin() != null) {
            UserLoginEntity userLogin = context.getUserLogin();
            partyId = userLogin.getPartyId();
        }

        // check for auto-userlogin for price rules
        if (UtilValidate.isEmpty(partyId) && context.getAutoUserLogin() != null) {
            UserLoginEntity userLogin = context.getAutoUserLogin();
            partyId = userLogin.getPartyId();
        }

        BigDecimal quantity = context.getQuantity();
        if (quantity == null) quantity = BigDecimal.ONE;

        BigDecimal amount = context.getAmount();

        Map<String, Object> productPriceWhere = new HashMap<>();
        productPriceWhere.put(x.productId, productId);
        productPriceWhere.put(x.currencyUomId, currencyDefaultUomId);
        productPriceWhere.put(x.productStoreGroupId, productStoreGroupId);
        if (UtilValidate.isNotEmpty(termUomId)) {
            productPriceWhere.put(x.termUomId, termUomId);
        }

        // for prices, get all ProductPrice entities for this productId and currencyUomId
        List<GenericValue> productPrices = null;
        try {
            productPrices = DaoQuery.use(delegator).from(x.ProductPrice).where(productPriceWhere).orderBy(x.fromDate_f5440273).cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.An_error_occurred_while_getting_the_product_prices, MODULE);
        }
        productPrices = EntityUtil.filterByDate(productPrices, true);
        // Backward compatibility: keep rows with null purpose for PURCHASE prices.
        if (UtilValidate.isNotEmpty(productPrices)) {
            if (x.PURCHASE.equals(productPricePurposeId)) {
                List<GenericValue> priceListForPurpose = new LinkedList<>();
                for (GenericValue productPrice : productPrices) {
                    String purpose = productPrice.getString(x.productPricePurposeId);
                    if (purpose == null || productPricePurposeId.equals(purpose)) {
                        priceListForPurpose.add(productPrice);
                    }
                }
                productPrices = priceListForPurpose;
            } else {
                productPrices = EntityUtil.filterByAnd(productPrices, UtilMisc.toMap(x.productPricePurposeId, productPricePurposeId));
            }
        }

        // ===== get the prices we need: list, default, average cost, promo, min, max =====
        // if any of these prices is missing and this product is a variant, default to the corresponding price on the virtual product
        GenericValue listPriceValue = getPriceValueForType(x.LIST_PRICE, productPrices, virtualProductPrices);
        GenericValue defaultPriceValue = getPriceValueForType(x.DEFAULT_PRICE, productPrices, virtualProductPrices);

        // If there is an agreement between the company and the client, and there is
        // a price for the product in it, it will override the default price of the
        // ProductPrice entity.
        if (UtilValidate.isNotEmpty(agreementId)) {
            try {
                GenericValue agreementPriceValue = DaoQuery.use(delegator).from(x.AgreementItemAndProductAppl).where(x.agreementId, agreementId,
                        x.productId, productId, x.currencyUomId, currencyDefaultUomId).queryFirst();
                if (agreementPriceValue != null && agreementPriceValue.get(x.price) != null) {
                    defaultPriceValue = agreementPriceValue;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Error_getting_agreement_info_from_the_database_while_calculating_price + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotRetrieveAgreementInfo, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        GenericValue competitivePriceValue = getPriceValueForType(x.COMPETITIVE_PRICE, productPrices, virtualProductPrices);
        GenericValue averageCostValue = getPriceValueForType(x.AVERAGE_COST, productPrices, virtualProductPrices);
        GenericValue promoPriceValue = getPriceValueForType(x.PROMO_PRICE, productPrices, virtualProductPrices);
        GenericValue minimumPriceValue = getPriceValueForType(x.MINIMUM_PRICE, productPrices, virtualProductPrices);
        GenericValue maximumPriceValue = getPriceValueForType(x.MAXIMUM_PRICE, productPrices, virtualProductPrices);
        GenericValue wholesalePriceValue = getPriceValueForType(x.WHOLESALE_PRICE, productPrices, virtualProductPrices);
        GenericValue specialPromoPriceValue = getPriceValueForType(x.SPECIAL_PROMO_PRICE, productPrices, virtualProductPrices);

        // now if this is a virtual product check each price type, if doesn't exist get from variant with lowest DEFAULT_PRICE
        if (x.Y.equals(product.getIsVirtual())) {
            // only do this if there is no default price, consider the others optional for performance reasons
            if (defaultPriceValue == null) {
                //use the cache to find the variant with the lowest default price
                try {
                    List<GenericValue> variantAssocList = DaoQuery.use(delegator).from(x.ProductAssoc).where(x.productId, productId,
                            x.productAssocTypeId, x.PRODUCT_VARIANT).orderBy(x.fromDate_f5440273).cache(true).filterByDate().queryList();
                    BigDecimal minDefaultPrice = null;
                    List<GenericValue> variantProductPrices = null;
                    for (GenericValue variantAssoc: variantAssocList) {
                        String curVariantProductId = variantAssoc.getString(x.productIdTo);
                        List<GenericValue> curVariantPriceList = DaoQuery.use(delegator).from(x.ProductPrice)
                                .where(x.productId, curVariantProductId).orderBy(x.fromDate_f5440273).cache(true).filterByDate(nowTimestamp).queryList();
                        List<GenericValue> tempDefaultPriceList = EntityUtil.filterByAnd(curVariantPriceList, UtilMisc.toMap(x.productPriceTypeId,
                                x.DEFAULT_PRICE));
                        GenericValue curDefaultPriceValue = EntityUtil.getFirst(tempDefaultPriceList);
                        if (curDefaultPriceValue != null) {
                            BigDecimal curDefaultPrice = curDefaultPriceValue.getBigDecimal(x.price);
                            if (minDefaultPrice == null || curDefaultPrice.compareTo(minDefaultPrice) < 0) {
                                // check to see if the product is discontinued for sale before considering it the lowest price
                                GenericValue curVariantProduct = DaoQuery.use(delegator).from(x.Product).where(x.productId, curVariantProductId)
                                        .cache().queryOne();
                                if (curVariantProduct != null) {
                                    Timestamp salesDiscontinuationDate = curVariantProduct.getTimestamp(x.salesDiscontinuationDate);
                                    if (salesDiscontinuationDate == null || salesDiscontinuationDate.after(nowTimestamp)) {
                                        minDefaultPrice = curDefaultPrice;
                                        variantProductPrices = curVariantPriceList;
                                    }
                                }
                            }
                        }
                    }

                    if (variantProductPrices != null) {
                        // we have some other options, give 'em a go...
                        if (listPriceValue == null) {
                            listPriceValue = getPriceValueForType(x.LIST_PRICE, variantProductPrices, null);
                        }
                        if (competitivePriceValue == null) {
                            competitivePriceValue = getPriceValueForType(x.COMPETITIVE_PRICE, variantProductPrices, null);
                        }
                        if (averageCostValue == null) {
                            averageCostValue = getPriceValueForType(x.AVERAGE_COST, variantProductPrices, null);
                        }
                        if (promoPriceValue == null) {
                            promoPriceValue = getPriceValueForType(x.PROMO_PRICE, variantProductPrices, null);
                        }
                        if (minimumPriceValue == null) {
                            minimumPriceValue = getPriceValueForType(x.MINIMUM_PRICE, variantProductPrices, null);
                        }
                        if (maximumPriceValue == null) {
                            maximumPriceValue = getPriceValueForType(x.MAXIMUM_PRICE, variantProductPrices, null);
                        }
                        if (wholesalePriceValue == null) {
                            wholesalePriceValue = getPriceValueForType(x.WHOLESALE_PRICE, variantProductPrices, null);
                        }
                        if (specialPromoPriceValue == null) {
                            specialPromoPriceValue = getPriceValueForType(x.SPECIAL_PROMO_PRICE, variantProductPrices, null);
                        }
                        defaultPriceValue = getPriceValueForType(x.DEFAULT_PRICE, variantProductPrices, null);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.An_error_occurred_while_getting_the_product_prices, MODULE);
                }
            }
        }

        BigDecimal promoPrice = BigDecimal.ZERO;
        if (promoPriceValue != null && promoPriceValue.get(x.price) != null) {
            promoPrice = promoPriceValue.getBigDecimal(x.price);
        }

        BigDecimal wholesalePrice = BigDecimal.ZERO;
        if (wholesalePriceValue != null && wholesalePriceValue.get(x.price) != null) {
            wholesalePrice = wholesalePriceValue.getBigDecimal(x.price);
        }

        boolean validPriceFound = false;
        BigDecimal defaultPrice = BigDecimal.ZERO;
        BigDecimal listPrice = null;
        BigDecimal discountRate = null;
        List<GenericValue> orderItemPriceInfos = new LinkedList<>();
        if (defaultPriceValue != null) {
            // If a price calc formula (service) is specified, then use it to get the unit price
            if (x.ProductPrice.equals(defaultPriceValue.getEntityName()) && UtilValidate.isNotEmpty(defaultPriceValue
                    .getString(x.customPriceCalcService))) {
                GenericValue customMethod = null;
                try {
                    customMethod = defaultPriceValue.getRelatedOne(x.CustomMethod, false);
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, x.An_error_occurred_while_getting_the_customPriceCalcService, MODULE);
                }
                if (customMethod != null && UtilValidate.isNotEmpty(customMethod.getString(x.customMethodName))) {
                    if (productGenericValue == null) {
                        try {
                            productGenericValue = DaoQuery.use(delegator).from(x.Product)
                                    .where(x.productId, productId).cache().queryOne();
                        } catch (GenericEntityException gee) {
                            Debug.logError(gee, x.An_error_occurred_while_getting_product_for_customPriceCalcService, MODULE);
                        }
                    }
                    if (userLoginGenericValue == null && context.getUserLogin() != null
                            && UtilValidate.isNotEmpty(context.getUserLogin().getUserLoginId())) {
                        try {
                            userLoginGenericValue = DaoQuery.use(delegator).from(x.UserLogin)
                                    .where(x.userLoginId, context.getUserLogin().getUserLoginId()).cache().queryOne();
                        } catch (GenericEntityException gee) {
                            Debug.logError(gee, x.An_error_occurred_while_getting_userLogin_for_customPriceCalcService, MODULE);
                        }
                    }

                    Map<String, Object> inMap = UtilMisc.toMap(x.userLogin, userLoginGenericValue, x.product, productGenericValue);
                    inMap.put(x.initialPrice, defaultPriceValue.getBigDecimal(x.price));
                    inMap.put(x.currencyUomId, currencyDefaultUomId);
                    inMap.put(x.quantity, quantity);
                    inMap.put(x.amount, amount);
                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                        inMap.put(x.surveyResponseId, surveyResponseId);
                    }
                    if (UtilValidate.isNotEmpty(customAttributes)) {
                        inMap.put(x.customAttributes, customAttributes);
                    }
                    inMap.put(x.productStoreGroupId, productStoreGroupId);
                    inMap.put(x.partyId, partyId);
                    try {
                        Map<String, Object> outMap = dispatcher.runSync(customMethod.getString(x.customMethodName), inMap);
                        if (ServiceUtil.isSuccess(outMap)) {
                            BigDecimal calculatedDefaultPrice = (BigDecimal) outMap.get(x.price);
                            BigDecimal calculatedListPrice = (BigDecimal) outMap.get(x.listPrice);
                            BigDecimal calculatedDiscountRate = (BigDecimal) outMap.get(x.discountRate);
                            orderItemPriceInfos = UtilGenerics.cast(outMap.get(x.orderItemPriceInfos));
                            if (UtilValidate.isNotEmpty(calculatedDefaultPrice)) {
                                defaultPrice = calculatedDefaultPrice;
                                listPrice = calculatedListPrice;
                                discountRate = calculatedDiscountRate;
                                validPriceFound = true;
                            }
                        }
                    } catch (GenericServiceException gse) {
                        Debug.logError(gse, x.An_error_occurred_while_running_the_customPriceCalcService
                                + customMethod.getString(x.customMethodName) + x.str_4ff447b8, MODULE);
                    }
                }
            }
            if (!validPriceFound && defaultPriceValue.get(x.price) != null) {
                defaultPrice = defaultPriceValue.getBigDecimal(x.price);
                validPriceFound = true;
            }
        }

        boolean skipPriceRules = true;
        if (listPrice == null && listPriceValue != null) {
            listPrice = listPriceValue.getBigDecimal(x.price);
            skipPriceRules = listPrice == null;
        }

        if (skipPriceRules) {
            // no list price, use defaultPrice for the final price

            // ========= ensure calculated price is not below minSalePrice or above maxSalePrice =========
            BigDecimal maxSellPrice = maximumPriceValue != null ? maximumPriceValue.getBigDecimal(x.price) : null;
            if (maxSellPrice != null && defaultPrice.compareTo(maxSellPrice) > 0) {
                defaultPrice = maxSellPrice;
            }
            // min price second to override max price, safety net
            BigDecimal minSellPrice = minimumPriceValue != null ? minimumPriceValue.getBigDecimal(x.price) : null;
            if (minSellPrice != null && defaultPrice.compareTo(minSellPrice) < 0) {
                defaultPrice = minSellPrice;
                // since we have found a minimum price that has overriden a the defaultPrice, even if no valid one was found,
                // we will consider it as if one had been...
                validPriceFound = true;
            }

            result.put(x.listPrice, listPrice);
            result.put(x.discountRate, discountRate);
            result.put(x.basePrice, defaultPrice);
            result.put(x.price, defaultPrice);
            result.put(x.defaultPrice, defaultPrice);
            result.put(x.competitivePrice, competitivePriceValue != null ? competitivePriceValue.getBigDecimal(x.price) : null);
            result.put(x.averageCost, averageCostValue != null ? averageCostValue.getBigDecimal(x.price) : null);
            result.put(x.promoPrice, promoPriceValue != null ? promoPriceValue.getBigDecimal(x.price) : null);
            result.put(x.specialPromoPrice, specialPromoPriceValue != null ? specialPromoPriceValue.getBigDecimal(x.price) : null);
            result.put(x.validPriceFound, validPriceFound);
            result.put(x.isSale, Boolean.FALSE);
            result.put(x.orderItemPriceInfos, orderItemPriceInfos);

            Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                    checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
            if (errorResult != null) return errorResult;
        } else {
            try {
                List<GenericValue> allProductPriceRules = makeProducePriceRuleList(delegator, optimizeForLargeRuleSet, productId, virtualProductId,
                        prodCatalogId, productStoreGroupId, webSiteId, partyId, currencyDefaultUomId);
                allProductPriceRules = EntityUtil.filterByDate(allProductPriceRules, true);

                List<GenericValue> quantityProductPriceRules = null;
                List<GenericValue> nonQuantityProductPriceRules = null;
                if (findAllQuantityPrices) {
                    // split into list with quantity conditions and list without, then iterate through each quantity cond one
                    quantityProductPriceRules = new LinkedList<>();
                    nonQuantityProductPriceRules = new LinkedList<>();
                    for (GenericValue productPriceRule: allProductPriceRules) {
                        List<GenericValue> productPriceCondList = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.productPriceRuleId,
                                productPriceRule.get(x.productPriceRuleId)).cache(true).queryList();

                        boolean foundQuantityInputParam = false;
                        // only consider a rule if all conditions except the quantity condition are true
                        boolean allExceptQuantTrue = true;
                        for (GenericValue productPriceCond: productPriceCondList) {
                            if (x.PRIP_QUANTITY.equals(productPriceCond.getString(x.inputParamEnumId))) {
                                foundQuantityInputParam = true;
                            } else {
                                if (!checkPriceCondition(productPriceCond, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                                        webSiteId, partyId, quantity, listPrice, currencyDefaultUomId, delegator, nowTimestamp)) {
                                    allExceptQuantTrue = false;
                                }
                            }
                        }

                        if (foundQuantityInputParam && allExceptQuantTrue) {
                            quantityProductPriceRules.add(productPriceRule);
                        } else {
                            nonQuantityProductPriceRules.add(productPriceRule);
                        }
                    }
                }

                if (findAllQuantityPrices) {
                    List<Map<String, Object>> allQuantityPrices = new LinkedList<>();

                    // if findAllQuantityPrices then iterate through quantityProductPriceRules
                    // foreach create an entry in the out list and eval that rule and all nonQuantityProductPriceRules rather than a single rule
                    for (GenericValue quantityProductPriceRule: quantityProductPriceRules) {
                        List<GenericValue> ruleListToUse = new LinkedList<>();
                        ruleListToUse.add(quantityProductPriceRule);
                        ruleListToUse.addAll(nonQuantityProductPriceRules);

                        Map<String, Object> quantCalcResults = calcPriceResultFromRules(ruleListToUse, listPrice, defaultPrice, promoPrice,
                                wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                                averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                                webSiteId, partyId, null, currencyDefaultUomId, delegator, nowTimestamp, locale);
                        Map<String, Object> quantErrorResult = addGeneralResults(quantCalcResults, competitivePriceValue, specialPromoPriceValue,
                                productStore,
                                checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                        if (quantErrorResult != null) return quantErrorResult;

                        // also add the quantityProductPriceRule to the Map so it can be used for quantity break information
                        quantCalcResults.put(x.quantityProductPriceRule, quantityProductPriceRule);

                        allQuantityPrices.add(quantCalcResults);
                    }
                    result.put(x.allQuantityPrices, allQuantityPrices);

                    // use a quantity 1 to get the main price, then fill in the quantity break prices
                    Map<String, Object> calcResults = calcPriceResultFromRules(allProductPriceRules, listPrice, defaultPrice, promoPrice,
                            wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                            averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                            webSiteId, partyId, BigDecimal.ONE, currencyDefaultUomId, delegator, nowTimestamp, locale);
                    result.putAll(calcResults);
                    // The orderItemPriceInfos out parameter requires a special treatment:
                    // the list of OrderItemPriceInfos generated by the price rule is appended to
                    // the existing orderItemPriceInfos list and the aggregated list is returned.
                    List<GenericValue> orderItemPriceInfosFromRule = UtilGenerics.cast(calcResults.get(x.orderItemPriceInfos));
                    if (UtilValidate.isNotEmpty(orderItemPriceInfosFromRule)) {
                        orderItemPriceInfos.addAll(orderItemPriceInfosFromRule);
                    }
                    result.put(x.orderItemPriceInfos, orderItemPriceInfos);

                    Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                            checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                    if (errorResult != null) return errorResult;
                } else {
                    Map<String, Object> calcResults = calcPriceResultFromRules(allProductPriceRules, listPrice, defaultPrice, promoPrice,
                            wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                            averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                            webSiteId, partyId, quantity, currencyDefaultUomId, delegator, nowTimestamp, locale);
                    result.putAll(calcResults);
                    // The orderItemPriceInfos out parameter requires a special treatment:
                    // the list of OrderItemPriceInfos generated by the price rule is appended to
                    // the existing orderItemPriceInfos list and the aggregated list is returned.
                    List<GenericValue> orderItemPriceInfosFromRule = UtilGenerics.cast(calcResults.get(x.orderItemPriceInfos));
                    if (UtilValidate.isNotEmpty(orderItemPriceInfosFromRule)) {
                        orderItemPriceInfos.addAll(orderItemPriceInfosFromRule);
                    }
                    result.put(x.orderItemPriceInfos, orderItemPriceInfos);

                    Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                            checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                    if (errorResult != null) return errorResult;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Error_getting_rules_from_the_database_while_calculating_price, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotRetrievePriceRules, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        // Convert the value to the price currency, if required
        if (x._true.equals(EntityUtilProperties.getPropertyValue(x.catalog, x.convertProductPriceCurrency, delegator))) {
            if (UtilValidate.isNotEmpty(currencyDefaultUomId) && UtilValidate.isNotEmpty(currencyUomIdTo)
                    && !currencyDefaultUomId.equals(currencyUomIdTo)) {
                if (UtilValidate.isNotEmpty(result)) {
                    Map<String, Object> convertPriceMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : result.entrySet()) {
                        BigDecimal tempPrice;
                        switch (entry.getKey()) {
                        case x.basePrice:
                        case x.price:
                        case x.defaultPrice:
                        case x.competitivePrice:
                        case x.averageCost:
                        case x.promoPrice:
                        case x.specialPromoPrice:
                        case x.listPrice:
                            tempPrice = (BigDecimal) entry.getValue();
                            break;
                        default:
                            tempPrice = BigDecimal.ZERO;
                        }

                        if (tempPrice != null && tempPrice != BigDecimal.ZERO) {
                            Map<String, Object> priceResults = new HashMap<>();
                            try {
                                priceResults = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId, currencyDefaultUomId,
                                        x.uomIdTo, currencyUomIdTo,
                                        x.originalValue, tempPrice, x.defaultDecimalScale, 2L, x.defaultRoundingMode, x.HalfUp));
                                if (ServiceUtil.isError(priceResults) || (priceResults.get(x.convertedValue) == null)) {
                                    Debug.logWarning(x.Unable_to_convert + entry.getKey() + x.for_product + productId, MODULE);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, MODULE);
                            }
                            convertPriceMap.put(entry.getKey(), priceResults.get(x.convertedValue));
                        } else {
                            convertPriceMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    if (UtilValidate.isNotEmpty(convertPriceMap)) {
                        convertPriceMap.put(x.currencyUsed, currencyUomIdTo);
                        result = convertPriceMap;
                    }
                }
            }
        }
        return result;
    }

    private static GenericValue getPriceValueForType(String productPriceTypeId, List<GenericValue> productPriceList,
                                                     List<GenericValue> secondaryPriceList) {
        List<GenericValue> filteredPrices = EntityUtil.filterByAnd(productPriceList, UtilMisc.toMap(x.productPriceTypeId, productPriceTypeId));
        GenericValue priceValue = EntityUtil.getFirst(filteredPrices);
        if (filteredPrices != null && filteredPrices.size() > 1) {
            if (Debug.infoOn()) {
                Debug.logInfo(x.There_is_more_than_one + productPriceTypeId + x.with_the_currencyUomId + priceValue.getString(x.currencyUomId)
                        + x.and_productId + priceValue.getString(x.productId) + x.using_the_latest_found_with_price
                        + priceValue.getBigDecimal(x.price), MODULE);
            }
        }
        if (priceValue == null && secondaryPriceList != null) {
            return getPriceValueForType(productPriceTypeId, secondaryPriceList, null);
        }
        return priceValue;
    }

    public static Map<String, Object> addGeneralResults(Map<String, Object> result, GenericValue competitivePriceValue,
                                                        GenericValue specialPromoPriceValue, GenericValue productStore, String checkIncludeVat,
                                                        String currencyUomId, String productId,
                                                        BigDecimal quantity, String partyId, LocalDispatcher dispatcher, Locale locale) {
        result.put(x.competitivePrice, competitivePriceValue != null ? competitivePriceValue.getBigDecimal(x.price) : null);
        result.put(x.specialPromoPrice, specialPromoPriceValue != null ? specialPromoPriceValue.getBigDecimal(x.price) : null);
        result.put(x.currencyUsed, currencyUomId);

        // okay, now we have the calculated price, see if we should add in tax and if so do it
        if (x.Y.equals(checkIncludeVat) && productStore != null && x.Y.equals(productStore.getString(x.showPricesWithVatTax))) {
            Map<String, Object> calcTaxForDisplayContext = UtilMisc.toMap(x.productStoreId, productStore.get(x.productStoreId),
                    x.productId, productId, x.quantity, quantity,
                    x.basePrice, (BigDecimal) result.get(x.price));
            if (UtilValidate.isNotEmpty(partyId)) {
                calcTaxForDisplayContext.put(x.billToPartyId, partyId);
            }

            try {
                Map<String, Object> calcTaxForDisplayResult = dispatcher.runSync(x.calcTaxForDisplay, calcTaxForDisplayContext);
                if (ServiceUtil.isError(calcTaxForDisplayResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.ProductPriceCannotCalculateVatTax, locale), null, null, calcTaxForDisplayResult);
                }
                // taxTotal, taxPercentage, priceWithTax
                result.put(x.price, calcTaxForDisplayResult.get(x.priceWithTax));

                // based on the taxPercentage calculate the other amounts, including: listPrice, defaultPrice, averageCost,
                // promoPrice, competitivePrice
                BigDecimal taxPercentage = (BigDecimal) calcTaxForDisplayResult.get(x.taxPercentage);
                BigDecimal taxMultiplier = ONE_BASE.add(taxPercentage.divide(PERCENT_SCALE, TAX_SCALE, TAX_ROUNDING));
                if (result.get(x.listPrice) != null) {
                    result.put(x.listPrice, ((BigDecimal) result.get(x.listPrice)).multiply(taxMultiplier).setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get(x.defaultPrice) != null) {
                    result.put(x.defaultPrice, ((BigDecimal) result.get(x.defaultPrice)).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get(x.averageCost) != null) {
                    result.put(x.averageCost, ((BigDecimal) result.get(x.averageCost)).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get(x.promoPrice) != null) {
                    result.put(x.promoPrice, ((BigDecimal) result.get(x.promoPrice)).multiply(taxMultiplier).setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get(x.competitivePrice) != null) {
                    result.put(x.competitivePrice, ((BigDecimal) result.get(x.competitivePrice)).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Error_calculating_VAT_tax_with_calcTaxForDisplay_service + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductPriceCannotCalculateVatTax, locale));
            }
        }

        return null;
    }

    public static List<GenericValue> makeProducePriceRuleList(Delegator delegator, boolean optimizeForLargeRuleSet, String productId,
            String virtualProductId, String prodCatalogId, String productStoreGroupId, String webSiteId, String partyId, String currencyUomId)
            throws GenericEntityException {
        List<GenericValue> productPriceRules = null;

        // At this point we have two options: optimize for large ruleset, or optimize for small ruleset
        // NOTE: This only effects the way that the rules to be evaluated are selected.
        // For large rule sets we can do a cached pre-filter to limit the rules that need to be evaled for a specific product.
        // Genercally I don't think that rule sets will get that big though, so the default is optimize for smaller rule set.
        if (optimizeForLargeRuleSet) {
            // ========= find all rules that must be run for each input type; this is kind of like a pre-filter to slim down the rules to run =======
            TreeSet<String> productPriceRuleIds = new TreeSet<>();

            // ------- These are all of the conditions that DON'T depend on the current inputs -------

            // by productCategoryId
            // for we will always include any rules that go by category, shouldn't be too many to iterate through each time and will save on cache
            // entries note that we always want to put the category, quantity, etc ones that find all rules with these conditions in separate cache
            // lists so that they can be easily cleared
            Collection<GenericValue> productCategoryIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_PROD_CAT_ID).cache(true).queryList();
            if (UtilValidate.isNotEmpty(productCategoryIdConds)) {
                for (GenericValue productCategoryIdCond: productCategoryIdConds) {
                    productPriceRuleIds.add(productCategoryIdCond.getString(x.productPriceRuleId));
                }
            }

            // by productFeatureId
            Collection<GenericValue> productFeatureIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_PROD_FEAT_ID).cache(true).queryList();
            if (UtilValidate.isNotEmpty(productFeatureIdConds)) {
                for (GenericValue productFeatureIdCond: productFeatureIdConds) {
                    productPriceRuleIds.add(productFeatureIdCond.getString(x.productPriceRuleId));
                }
            }

            // by quantity -- should we really do this one, ie is it necessary?
            // we could say that all rules with quantity on them must have one of these other values
            // but, no we'll do it the other way, any that have a quantity will always get compared
            Collection<GenericValue> quantityConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_QUANTITY).cache(true).queryList();
            if (UtilValidate.isNotEmpty(quantityConds)) {
                for (GenericValue quantityCond: quantityConds) {
                    productPriceRuleIds.add(quantityCond.getString(x.productPriceRuleId));
                }
            }

            // by roleTypeId
            Collection<GenericValue> roleTypeIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_ROLE_TYPE).cache(true).queryList();
            if (UtilValidate.isNotEmpty(roleTypeIdConds)) {
                for (GenericValue roleTypeIdCond: roleTypeIdConds) {
                    productPriceRuleIds.add(roleTypeIdCond.getString(x.productPriceRuleId));
                }
            }

            // by partyClassificationGroupId
            Collection<GenericValue> partyClassificationGroupIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_PARTY_CLASS).cache(true).queryList();
            if (UtilValidate.isNotEmpty(partyClassificationGroupIdConds)) {
                for (GenericValue partyClassificationGroupIdCond: partyClassificationGroupIdConds) {
                    productPriceRuleIds.add(partyClassificationGroupIdCond.getString(x.productPriceRuleId));
                }
            }

            // TODO, not supported yet: by groupPartyId
            // later: (by partyClassificationTypeId)

            // by listPrice
            Collection<GenericValue> listPriceConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_LIST_PRICE).cache(true).queryList();
            if (UtilValidate.isNotEmpty(listPriceConds)) {
                for (GenericValue listPriceCond: listPriceConds) {
                    productPriceRuleIds.add(listPriceCond.getString(x.productPriceRuleId));
                }
            }

            // ------- These are all of them that DO depend on the current inputs -------

            // by productId
            Collection<GenericValue> productIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_PRODUCT_ID, x.condValue, productId).cache(true).queryList();
            if (UtilValidate.isNotEmpty(productIdConds)) {
                for (GenericValue productIdCond: productIdConds) {
                    productPriceRuleIds.add(productIdCond.getString(x.productPriceRuleId));
                }
            }

            // by virtualProductId, if not null
            if (virtualProductId != null) {
                Collection<GenericValue> virtualProductIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                        x.PRIP_PRODUCT_ID, x.condValue, virtualProductId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(virtualProductIdConds)) {
                    for (GenericValue virtualProductIdCond: virtualProductIdConds) {
                        productPriceRuleIds.add(virtualProductIdCond.getString(x.productPriceRuleId));
                    }
                }
            }

            // by prodCatalogId - which is optional in certain cases
            if (UtilValidate.isNotEmpty(prodCatalogId)) {
                Collection<GenericValue> prodCatalogIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                        x.PRIP_PROD_CLG_ID, x.condValue, prodCatalogId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(prodCatalogIdConds)) {
                    for (GenericValue prodCatalogIdCond: prodCatalogIdConds) {
                        productPriceRuleIds.add(prodCatalogIdCond.getString(x.productPriceRuleId));
                    }
                }
            }

            // by productStoreGroupId
            if (UtilValidate.isNotEmpty(productStoreGroupId)) {
                Collection<GenericValue> storeGroupConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                        x.PRIP_PROD_SGRP_ID, x.condValue, productStoreGroupId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(storeGroupConds)) {
                    for (GenericValue storeGroupCond: storeGroupConds) {
                        productPriceRuleIds.add(storeGroupCond.getString(x.productPriceRuleId));
                    }
                }
            }

            // by webSiteId
            if (UtilValidate.isNotEmpty(webSiteId)) {
                Collection<GenericValue> webSiteIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                        x.PRIP_WEBSITE_ID, x.condValue, webSiteId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(webSiteIdConds)) {
                    for (GenericValue webSiteIdCond: webSiteIdConds) {
                        productPriceRuleIds.add(webSiteIdCond.getString(x.productPriceRuleId));
                    }
                }
            }

            // by partyId
            if (UtilValidate.isNotEmpty(partyId)) {
                Collection<GenericValue> partyIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                        x.PRIP_PARTY_ID, x.condValue, partyId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(partyIdConds)) {
                    for (GenericValue partyIdCond: partyIdConds) {
                        productPriceRuleIds.add(partyIdCond.getString(x.productPriceRuleId));
                    }
                }
            }

            // by currencyUomId
            Collection<GenericValue> currencyUomIdConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.inputParamEnumId,
                    x.PRIP_CURRENCY_UOMID, x.condValue, currencyUomId).cache(true).queryList();
            if (UtilValidate.isNotEmpty(currencyUomIdConds)) {
                for (GenericValue currencyUomIdCond: currencyUomIdConds) {
                    productPriceRuleIds.add(currencyUomIdCond.getString(x.productPriceRuleId));
                }
            }

            productPriceRules = new LinkedList<>();
            for (String productPriceRuleId: productPriceRuleIds) {
                GenericValue productPriceRule = DaoQuery.use(delegator).from(x.ProductPriceRule).where(x.productPriceRuleId, productPriceRuleId)
                        .cache().queryOne();
                if (productPriceRule == null) continue;
                productPriceRules.add(productPriceRule);
            }
        } else {
            productPriceRules = DaoQuery.use(delegator).from(x.ProductPriceRule).cache(true).queryList();
            if (productPriceRules == null) productPriceRules = new LinkedList<>();
        }

        return productPriceRules;
    }

    public static Map<String, Object> calcPriceResultFromRules(List<GenericValue> productPriceRules, BigDecimal listPrice, BigDecimal defaultPrice,
                                                               BigDecimal promoPrice,
                                                               BigDecimal wholesalePrice, GenericValue maximumPriceValue,
                                                               GenericValue minimumPriceValue, boolean validPriceFound,
                                                               GenericValue averageCostValue, String productId, String virtualProductId,
                                                               String prodCatalogId, String productStoreGroupId,
                                                               String webSiteId, String partyId, BigDecimal quantity, String currencyUomId,
                                                               Delegator delegator, Timestamp nowTimestamp,
                                                               Locale locale) throws GenericEntityException {

        Map<String, Object> calcResults = new HashMap<>();

        List<GenericValue> orderItemPriceInfos = new LinkedList<>();
        boolean isSale = false;

        // ========= go through each price rule by id and eval all conditions =========
        int totalConds = 0;
        int totalActions = 0;
        int totalRules = 0;

        // get some of the base values to calculate with
        BigDecimal averageCost = (averageCostValue != null && averageCostValue.get(x.price) != null) ? averageCostValue.getBigDecimal(x.price)
                : listPrice;
        BigDecimal margin = listPrice.subtract(averageCost);

        // calculate running sum based on listPrice and rules found
        BigDecimal price = listPrice;

        for (GenericValue productPriceRule: productPriceRules) {
            String productPriceRuleId = productPriceRule.getString(x.productPriceRuleId);

            // check from/thru dates
            java.sql.Timestamp fromDate = productPriceRule.getTimestamp(x.fromDate);
            java.sql.Timestamp thruDate = productPriceRule.getTimestamp(x.thruDate);

            if (fromDate != null && fromDate.after(nowTimestamp)) {
                // hasn't started yet
                continue;
            }
            if (thruDate != null && thruDate.before(nowTimestamp)) {
                // already expired
                continue;
            }

            // check all conditions
            boolean allTrue = true;
            StringBuilder condsDescription = new StringBuilder();
            List<GenericValue> productPriceConds = DaoQuery.use(delegator).from(x.ProductPriceCond).where(x.productPriceRuleId,
                    productPriceRuleId).cache(true).queryList();
            for (GenericValue productPriceCond: productPriceConds) {

                totalConds++;

                if (!checkPriceCondition(productPriceCond, productId, virtualProductId, prodCatalogId, productStoreGroupId, webSiteId, partyId,
                        quantity, listPrice, currencyUomId, delegator, nowTimestamp)) {
                    allTrue = false;
                    break;
                }

                // add condsDescription string entry
                condsDescription.append(x.str_1e5c2f36);
                GenericValue inputParamEnum = productPriceCond.getRelatedOne(x.InputParamEnumeration, true);

                condsDescription.append(inputParamEnum.getString(x.enumCode));
                // condsDescription.append(":");
                GenericValue operatorEnum = productPriceCond.getRelatedOne(x.OperatorEnumeration, true);

                condsDescription.append(operatorEnum.getString(x.description));
                // condsDescription.append(":");
                condsDescription.append(productPriceCond.getString(x.condValue));
                condsDescription.append(x.str_01af9139);
            }

            // add some info about the prices we are calculating from
            condsDescription.append(x.list);
            condsDescription.append(listPrice);
            condsDescription.append(x.avgCost);
            condsDescription.append(averageCost);
            condsDescription.append(x.margin);
            condsDescription.append(margin);
            condsDescription.append(x.str_01af9139);

            boolean foundFlatOverride = false;

            // if all true, perform all actions
            if (allTrue) {
                // check isSale
                if (x.Y.equals(productPriceRule.getString(x.isSale))) {
                    isSale = true;
                }

                List<GenericValue> productPriceActions = DaoQuery.use(delegator).from(x.ProductPriceAction).where(x.productPriceRuleId,
                        productPriceRuleId).cache(true).queryList();
                for (GenericValue productPriceAction: productPriceActions) {

                    totalActions++;

                    // yeah, finally here, perform the action, ie, modify the price
                    BigDecimal modifyAmount = BigDecimal.ZERO;

                    if (x.PRICE_POD.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null) {
                            modifyAmount = defaultPrice.multiply(productPriceAction.getBigDecimal(x.amount).movePointLeft(2));
                            price = defaultPrice;
                        }
                    } else if (x.PRICE_POL.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null) {
                            modifyAmount = listPrice.multiply(productPriceAction.getBigDecimal(x.amount).movePointLeft(2));
                        }
                    } else if (x.PRICE_POAC.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null) {
                            modifyAmount = averageCost.multiply(productPriceAction.getBigDecimal(x.amount).movePointLeft(2));
                        }
                    } else if (x.PRICE_POM.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null) {
                            modifyAmount = margin.multiply(productPriceAction.getBigDecimal(x.amount).movePointLeft(2));
                        }
                    } else if (x.PRICE_POWHS.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null && wholesalePrice != null) {
                            modifyAmount = wholesalePrice.multiply(productPriceAction.getBigDecimal(x.amount).movePointLeft(2));
                        }
                    } else if (x.PRICE_FOL.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        if (productPriceAction.get(x.amount) != null) {
                            modifyAmount = productPriceAction.getBigDecimal(x.amount);
                        }
                    } else if (x.PRICE_FLAT.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        // this one is a bit different, break out of the loop because we now have our final price
                        foundFlatOverride = true;
                        if (productPriceAction.get(x.amount) != null) {
                            price = productPriceAction.getBigDecimal(x.amount);
                        } else {
                            Debug.logInfo(x.ProductPriceAction_had_null_amount_using_default_price + defaultPrice + x.for_product_with_id
                                    + productId, MODULE);
                            price = defaultPrice;
                            isSale = false;                // reverse isSale flag, as this sale rule was actually not applied
                        }
                    } else if (x.PRICE_PFLAT.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        // this one is a bit different too, break out of the loop because we now have our final price
                        foundFlatOverride = true;
                        price = promoPrice;
                        if (productPriceAction.get(x.amount) != null) {
                            price = price.add(productPriceAction.getBigDecimal(x.amount));
                        }
                        if (price.compareTo(BigDecimal.ZERO) == 0) {
                            if (defaultPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo(x.PromoPrice_and_ProductPriceAction_had_null_amount_using_default_price + defaultPrice
                                        + x.for_product_with_id + productId, MODULE);
                                price = defaultPrice;
                            } else if (listPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo(x.PromoPrice_and_ProductPriceAction_had_null_amount_and_no_default_price_was_available
                                        + x.using_list_price + listPrice + x.for_product_with_id + productId, MODULE);
                                price = listPrice;
                            } else {
                                Debug.logError(x.PromoPrice_and_ProductPriceAction_had_null_amount_and_no_default_or_list_price_was_available
                                        + x.so_price_is_set_to_zero_for_product_with_id + productId, MODULE);
                                price = BigDecimal.ZERO;
                            }
                            isSale = false;                // reverse isSale flag, as this sale rule was actually not applied
                        }
                    } else if (x.PRICE_WFLAT.equals(productPriceAction.getString(x.productPriceActionTypeId))) {
                        // same as promo price but using the wholesale price instead
                        foundFlatOverride = true;
                        price = wholesalePrice;
                        if (productPriceAction.get(x.amount) != null) {
                            price = price.add(productPriceAction.getBigDecimal(x.amount));
                        }
                        if (price.compareTo(BigDecimal.ZERO) == 0) {
                            if (defaultPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo(x.WholesalePrice_and_ProductPriceAction_had_null_amount_using_default_price + defaultPrice
                                        + x.for_product_with_id + productId, MODULE);
                                price = defaultPrice;
                            } else if (listPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo(x.WholesalePrice_and_ProductPriceAction_had_null_amount_and_no_default_price_was_available
                                        + x.using_list_price + listPrice + x.for_product_with_id + productId, MODULE);
                                price = listPrice;
                            } else {
                                Debug.logError(x.WholesalePrice_and_ProductPriceAction_had_null_amount_and_no_default_or_list_price_was_available
                                        + x.so_price_is_set_to_zero_for_product_with_id_ae0d57a2 + productId, MODULE);
                                price = BigDecimal.ZERO;
                            }
                            isSale = false; // reverse isSale flag, as this sale rule was actually not applied
                        }
                    }

                    // add a orderItemPriceInfo element too, without orderId or orderItemId
                    StringBuilder priceInfoDescription = new StringBuilder();


                    priceInfoDescription.append(condsDescription.toString());
                    priceInfoDescription.append(x.str_1e5c2f36);
                    priceInfoDescription.append(UtilProperties.getMessage(RESOURCE, x.ProductPriceConditionType, locale));
                    priceInfoDescription.append(productPriceAction.getString(x.productPriceActionTypeId));
                    priceInfoDescription.append(x.str_4ff447b8);

                    GenericValue orderItemPriceInfo = delegator.makeValue(x.OrderItemPriceInfo);

                    orderItemPriceInfo.set(x.productPriceRuleId, productPriceAction.get(x.productPriceRuleId));
                    orderItemPriceInfo.set(x.productPriceActionSeqId, productPriceAction.get(x.productPriceActionSeqId));
                    orderItemPriceInfo.set(x.modifyAmount, modifyAmount);
                    orderItemPriceInfo.set(x.rateCode, productPriceAction.get(x.rateCode));
                    // make sure description is <= than 250 chars
                    String priceInfoDescriptionString = priceInfoDescription.toString();

                    if (priceInfoDescriptionString.length() > 250) {
                        priceInfoDescriptionString = priceInfoDescriptionString.substring(0, 250);
                    }
                    orderItemPriceInfo.set(x.description, priceInfoDescriptionString);
                    orderItemPriceInfos.add(orderItemPriceInfo);

                    if (foundFlatOverride) {
                        break;
                    } else {
                        price = price.add(modifyAmount);
                    }
                }
            }

            totalRules++;

            if (foundFlatOverride) {
                break;
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Unchecked_Calculated_price + price, MODULE);
            Debug.logVerbose(x.PriceInfo, MODULE);
            for (GenericValue orderItemPriceInfo: orderItemPriceInfos) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(x.str_b5e406cc + orderItemPriceInfo, MODULE);
                }
            }
        }

        // if no actions were run on the list price, then use the default price
        if (totalActions == 0) {
            price = defaultPrice;
            // here we will leave validPriceFound as it was originally set for the defaultPrice since that is what we are setting the price to...
        } else {
            // at least one price rule action was found, so we will consider it valid
            validPriceFound = true;
        }

        // ========= ensure calculated price is not below minSalePrice or above maxSalePrice =========
        BigDecimal maxSellPrice = maximumPriceValue != null ? maximumPriceValue.getBigDecimal(x.price) : null;
        if (maxSellPrice != null && price.compareTo(maxSellPrice) > 0) {
            price = maxSellPrice;
        }
        // min price second to override max price, safety net
        BigDecimal minSellPrice = minimumPriceValue != null ? minimumPriceValue.getBigDecimal(x.price) : null;
        if (minSellPrice != null && price.compareTo(minSellPrice) < 0) {
            price = minSellPrice;
            // since we have found a minimum price that has overriden a the defaultPrice, even if no valid one was found,
            // we will consider it as if one had been...
            validPriceFound = true;
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Final_Calculated_price + price + x.rules + totalRules + x.conds + totalConds
                    + x.actions + totalActions, MODULE);
        }

        calcResults.put(x.basePrice, price);
        calcResults.put(x.price, price);
        calcResults.put(x.listPrice, listPrice);
        calcResults.put(x.defaultPrice, defaultPrice);
        calcResults.put(x.averageCost, averageCost);
        calcResults.put(x.orderItemPriceInfos, orderItemPriceInfos);
        calcResults.put(x.isSale, isSale);
        calcResults.put(x.validPriceFound, validPriceFound);

        return calcResults;
    }

    public static boolean checkPriceCondition(GenericValue productPriceCond, String productId, String virtualProductId, String prodCatalogId,
            String productStoreGroupId, String webSiteId, String partyId, BigDecimal quantity, BigDecimal listPrice,
            String currencyUomId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Checking_price_condition + productPriceCond, MODULE);
        }
        int compare = 0;

        if (x.PRIP_PRODUCT_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            compare = UtilMisc.toList(productId, virtualProductId).contains(productPriceCond.getString(x.condValue)) ? 0 : 1;
        } else if (x.PRIP_PROD_CAT_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            // if a ProductCategoryMember exists for this productId and the specified productCategoryId
            String productCategoryId = productPriceCond.getString(x.condValue);
            // and from/thru date within range
            List<GenericValue> productCategoryMembers = DaoQuery.use(delegator).from(x.ProductCategoryMember)
                    .where(x.productId, productId, x.productCategoryId, productCategoryId)
                    .cache(true)
                    .filterByDate(nowTimestamp)
                    .queryList();
            // then 0 (equals), otherwise 1 (not equals)
            if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                compare = 0;
            } else {
                compare = 1;
            }

            // if there is a virtualProductId, try that given that this one has failed
            // NOTE: this is important becuase of the common scenario where a virtual product is a member of a category but the variants
            // will typically NOT be
            // NOTE: we may want to parameterize this in the future, ie with an indicator on the ProductPriceCond entity
            if (compare == 1 && UtilValidate.isNotEmpty(virtualProductId)) {
                // and from/thru date within range
                List<GenericValue> virtualProductCategoryMembers = DaoQuery.use(delegator).from(x.ProductCategoryMember).where(x.productId,
                        virtualProductId, x.productCategoryId, productCategoryId).cache(true).filterByDate(nowTimestamp).queryList();
                if (UtilValidate.isNotEmpty(virtualProductCategoryMembers)) {
                    // we found a member record? great, then this condition is satisfied
                    compare = 0;
                }
            }
        } else if (x.PRIP_PROD_FEAT_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            // NOTE: DEJ20070130 don't retry this condition with the virtualProductId as well; this breaks various things you might want to do
            // with price rules, like have different pricing for a variant products with a certain distinguishing feature

            // if a ProductFeatureAppl exists for this productId and the specified productFeatureId
            String productFeatureId = productPriceCond.getString(x.condValue);
            // and from/thru date within range
            List<GenericValue> productFeatureAppls = DaoQuery.use(delegator).from(x.ProductFeatureAppl).where(x.productId, productId,
                    x.productFeatureId, productFeatureId).cache(true).filterByDate(nowTimestamp).queryList();
            // then 0 (equals), otherwise 1 (not equals)
            if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                compare = 0;
            } else {
                compare = 1;
            }
        } else if (x.PRIP_PROD_CLG_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isNotEmpty(prodCatalogId)) {
                compare = prodCatalogId.compareTo(productPriceCond.getString(x.condValue));
            } else {
                // this shouldn't happen because if prodCatalogId is null no PRIP_PROD_CLG_ID prices will be in the list
                compare = 1;
            }
        } else if (x.PRIP_PROD_SGRP_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isNotEmpty(productStoreGroupId)) {
                compare = productStoreGroupId.compareTo(productPriceCond.getString(x.condValue));
            } else {
                compare = 1;
            }
        } else if (x.PRIP_WEBSITE_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isNotEmpty(webSiteId)) {
                compare = webSiteId.compareTo(productPriceCond.getString(x.condValue));
            } else {
                compare = 1;
            }
        } else if (x.PRIP_QUANTITY.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (quantity == null) {
                // if no quantity is passed in, assume all quantity conditions pass
                // NOTE: setting compare = 0 won't do the trick here because the condition won't always be or include and equal
                return true;
            } else {
                compare = quantity.compareTo(new BigDecimal(productPriceCond.getString(x.condValue)));
            }
        } else if (x.PRIP_PARTY_ID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isNotEmpty(partyId)) {
                compare = partyId.compareTo(productPriceCond.getString(x.condValue));
            } else {
                compare = 1;
            }
        } else if (x.PRIP_PARTY_GRP_MEM.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isEmpty(partyId)) {
                compare = 1;
            } else {
                String groupPartyId = productPriceCond.getString(x.condValue);
                if (partyId.equals(groupPartyId)) {
                    compare = 0;
                } else {
                    // look for PartyRelationship with
                    // partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is
                    // the group member, so the partyIdFrom is the groupPartyId
                    // and from/thru date within range
                    List<GenericValue> partyRelationshipList = DaoQuery.use(delegator).from(x.PartyRelationship).where(x.partyIdFrom, groupPartyId,
                            x.partyIdTo, partyId, x.partyRelationshipTypeId, x.GROUP_ROLLUP).cache(true).filterByDate(nowTimestamp).queryList();
                    // then 0 (equals), otherwise 1 (not equals)
                    if (UtilValidate.isNotEmpty(partyRelationshipList)) {
                        compare = 0;
                    } else {
                        compare = checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyId);
                    }
                }
            }
        } else if (x.PRIP_PARTY_CLASS.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (UtilValidate.isEmpty(partyId)) {
                compare = 1;
            } else {
                String partyClassificationGroupId = productPriceCond.getString(x.condValue);
                // find any PartyClassification
                // and from/thru date within range
                List<GenericValue> partyClassificationList = DaoQuery.use(delegator).from(x.PartyClassification).where(x.partyId, partyId,
                        x.partyClassificationGroupId, partyClassificationGroupId).cache(true).filterByDate(nowTimestamp).queryList();
                // then 0 (equals), otherwise 1 (not equals)
                if (UtilValidate.isNotEmpty(partyClassificationList)) {
                    compare = 0;
                } else {
                    compare = 1;
                }
            }
        } else if (x.PRIP_ROLE_TYPE.equals(productPriceCond.getString(x.inputParamEnumId))) {
            if (partyId != null) {
                // if a PartyRole exists for this partyId and the specified roleTypeId
                GenericValue partyRole = DaoQuery.use(delegator).from(x.PartyRole).where(x.partyId, partyId, x.roleTypeId,
                        productPriceCond.getString(x.condValue)).cache(true).queryOne();

                // then 0 (equals), otherwise 1 (not equals)
                if (partyRole != null) {
                    compare = 0;
                } else {
                    compare = 1;
                }
            } else {
                compare = 1;
            }
        } else if (x.PRIP_LIST_PRICE.equals(productPriceCond.getString(x.inputParamEnumId))) {
            BigDecimal listPriceValue = listPrice;

            compare = listPriceValue.compareTo(new BigDecimal(productPriceCond.getString(x.condValue)));
        } else if (x.PRIP_CURRENCY_UOMID.equals(productPriceCond.getString(x.inputParamEnumId))) {
            compare = currencyUomId.compareTo(productPriceCond.getString(x.condValue));
        } else {
            Debug.logWarning(x.An_un_supported_productPriceCond_input_parameter_lhs_was_used + productPriceCond.getString(x.inputParamEnumId)
                    + x.returning_false_ie_check_failed, MODULE);
            return false;
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Price_Condition_compare_done_compare + compare, MODULE);
        }

        if (x.PRC_EQ.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare == 0) return true;
        } else if (x.PRC_NEQ.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare != 0) return true;
        } else if (x.PRC_LT.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare < 0) return true;
        } else if (x.PRC_LTE.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare <= 0) return true;
        } else if (x.PRC_GT.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare > 0) return true;
        } else if (x.PRC_GTE.equals(productPriceCond.getString(x.operatorEnumId))) {
            if (compare >= 0) return true;
        } else {
            Debug.logWarning(x.An_un_supported_productPriceCond_condition_was_used + productPriceCond.getString(x.operatorEnumId)
                    + x.returning_false_ie_check_failed, MODULE);
            return false;
        }
        return false;
    }

    private static int checkConditionPartyHierarchy(Delegator delegator, Timestamp nowTimestamp, String groupPartyId, String partyId)
            throws GenericEntityException {
        List<GenericValue> partyRelationshipList = DaoQuery.use(delegator).from(x.PartyRelationship).where(x.partyIdTo, partyId,
                x.partyRelationshipTypeId, x.GROUP_ROLLUP).cache(true).filterByDate(nowTimestamp).queryList();
        for (GenericValue genericValue : partyRelationshipList) {
            String partyIdFrom = (String) genericValue.get(x.partyIdFrom);
            if (partyIdFrom.equals(groupPartyId)) {
                return 0;
            }
            if (0 == checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyIdFrom)) {
                return 0;
            }
        }
        return 1;
    }

    /**
     * Calculates the purchase price of a product
     */
    public static Map<String, Object> calculatePurchasePrice(DispatchContext dctx, CalculatePurchasePriceContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<>();

        List<GenericValue> orderItemPriceInfos = new LinkedList<>();
        boolean validPriceFound = false;
        BigDecimal price = BigDecimal.ZERO;

        ProductEntity product = context.getProduct();
        String productId = product.getProductId();
        String agreementId = context.getAgreementId();
        String currencyUomId = context.getCurrencyUomId();
        String partyId = context.getPartyId();
        BigDecimal quantity = context.getQuantity();
        Locale locale = context.getLocale();

        // a) Get the Price from the Agreement* data model
        if (Debug.infoOn()) {
            Debug.logInfo(x.Try_to_resolve_purchase_price_from_agreement + agreementId, MODULE);
        }
        if (UtilValidate.isNotEmpty(agreementId)) {
            //TODO Search before if agreement is associate to SupplierProduct.
            //confirm that agreement is price application on purchase type and contains a value for the product
            EntityCondition cond = EntityCondition.makeConditionMap(
                    x.agreementId, agreementId,
                    x.agreementItemTypeId, x.AGREEMENT_PRICING_PR,
                    x.agreementTypeId, x.PURCHASE_AGREEMENT,
                    x.productId, productId);
            try {
                List<GenericValue> agreementPrices = delegator.findList(x.AgreementItemAndProductAppl, cond,
                        UtilMisc.toSet(x.price, x.currencyUomId), null, null, true);
                if (UtilValidate.isNotEmpty(agreementPrices)) {
                    GenericValue priceFound = null;
                    //resolve price on given currency. If not define, try to convert a present price
                    priceFound = EntityUtil.getFirst(EntityUtil.filterByAnd(agreementPrices, UtilMisc.toMap(x.currencyUomId, currencyUomId)));
                    if (Debug.infoOn()) {
                        Debug.logInfo(x.AgreementItem_824a0b99 + agreementPrices, MODULE);
                        Debug.logInfo(x.currencyUomId_229d064c + currencyUomId, MODULE);
                        Debug.logInfo(x.priceFound + priceFound, MODULE);
                    }
                    if (priceFound == null) {
                        priceFound = EntityUtil.getFirst(agreementPrices);
                        try {
                            Map<String, Object> priceConvertMap = UtilMisc.toMap(x.uomId, priceFound.getString(x.currencyUomId), x.uomIdTo,
                                    currencyUomId, x.originalValue, priceFound.getBigDecimal(x.price), x.defaultDecimalScale, 2L,
                                    x.defaultRoundingMode, x.HalfUp);
                            Map<String, Object> priceResults = dispatcher.runSync(x.convertUom, priceConvertMap);
                            if (ServiceUtil.isError(priceResults) || (priceResults.get(x.convertedValue) == null)) {
                                Debug.logWarning(x.Unable_to_convert + priceFound + x.for_product + productId, MODULE);
                            } else {
                                price = (BigDecimal) priceResults.get(x.convertedValue);
                                validPriceFound = true;
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }
                    } else {
                        price = priceFound.getBigDecimal(x.price);
                        validPriceFound = true;
                    }
                }
                if (validPriceFound) {
                    GenericValue agreement = delegator.findOne(x.Agreement, true, UtilMisc.toMap(x.agreementId, agreementId));
                    StringBuilder priceInfoDescription = new StringBuilder();
                    priceInfoDescription.append(UtilProperties.getMessage(RESOURCE, x.ProductAgreementUse, locale));
                    priceInfoDescription.append(x.str_1e5c2f36);
                    priceInfoDescription.append(agreementId);
                    priceInfoDescription.append(x.str_01af9139);
                    priceInfoDescription.append(agreement.get(x.description));
                    GenericValue orderItemPriceInfo = delegator.makeValue(x.OrderItemPriceInfo);
                    // make sure description is <= than 250 chars
                    String priceInfoDescriptionString = priceInfoDescription.toString();
                    if (priceInfoDescriptionString.length() > 250) {
                        priceInfoDescriptionString = priceInfoDescriptionString.substring(0, 250);
                    }
                    orderItemPriceInfo.set(x.description, priceInfoDescriptionString);
                    orderItemPriceInfos.add(orderItemPriceInfo);
                }
            } catch (GenericEntityException gee) {
                Debug.logError(gee, MODULE);
                return ServiceUtil.returnError(gee.getMessage());
            }
        }

        // b) If no price can be found, get the lastPrice from the SupplierProduct entity
        if (!validPriceFound) {
            Map<String, Object> priceContext = UtilMisc.toMap(x.currencyUomId, currencyUomId, x.partyId, partyId, x.productId, productId,
                    x.quantity, quantity, x.agreementId, agreementId);
            List<GenericValue> productSuppliers = null;
            try {
                Map<String, Object> priceResult = dispatcher.runSync(x.getSuppliersForProduct, priceContext);
                if (ServiceUtil.isError(priceResult)) {
                    String errMsg = ServiceUtil.getErrorMessage(priceResult);
                    Debug.logError(errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
                productSuppliers = UtilGenerics.cast(priceResult.get(x.supplierProducts));
            } catch (GenericServiceException gse) {
                Debug.logError(gse, MODULE);
                return ServiceUtil.returnError(gse.getMessage());
            }
            if (productSuppliers != null) {
                for (GenericValue productSupplier: productSuppliers) {
                    if (!validPriceFound) {
                        price = ((BigDecimal) productSupplier.get(x.lastPrice));
                        validPriceFound = true;
                    }
                    // add a orderItemPriceInfo element too, without orderId or orderItemId
                    StringBuilder priceInfoDescription = new StringBuilder();
                    priceInfoDescription.append(UtilProperties.getMessage(RESOURCE, x.ProductSupplier, locale));
                    priceInfoDescription.append(x.str_42cbdb3c);
                    priceInfoDescription.append(UtilProperties.getMessage(RESOURCE, x.ProductSupplierMinimumOrderQuantity, locale));
                    priceInfoDescription.append(productSupplier.getBigDecimal(x.minimumOrderQuantity));
                    priceInfoDescription.append(UtilProperties.getMessage(RESOURCE, x.ProductSupplierLastPrice, locale));
                    priceInfoDescription.append(productSupplier.getBigDecimal(x.lastPrice));
                    priceInfoDescription.append(x.str_4ff447b8);
                    GenericValue orderItemPriceInfo = delegator.makeValue(x.OrderItemPriceInfo);
                    // make sure description is <= than 250 chars
                    String priceInfoDescriptionString = priceInfoDescription.toString();
                    if (priceInfoDescriptionString.length() > 250) {
                        priceInfoDescriptionString = priceInfoDescriptionString.substring(0, 250);
                    }
                    orderItemPriceInfo.set(x.description, priceInfoDescriptionString);
                    orderItemPriceInfos.add(orderItemPriceInfo);
                }
            }
        }

        // c) If no price can be found, get the averageCost from the ProductPrice entity
        if (!validPriceFound) {
            List<GenericValue> prices = null;
            try {
                prices = DaoQuery.use(delegator).from(x.ProductPrice).where(x.productId, productId, x.productPricePurposeId,
                        x.PURCHASE).orderBy(x.fromDate_f5440273).queryList();

                // if no prices are found; find the prices of the parent product
                if (UtilValidate.isEmpty(prices)) {
                    GenericValue parentProduct = ProductWorker.getParentProduct(productId, delegator);
                    if (parentProduct != null) {
                        String parentProductId = parentProduct.getString(x.productId);
                        prices = DaoQuery.use(delegator).from(x.ProductPrice).where(x.productId, parentProductId, x.productPricePurposeId,
                                x.PURCHASE).orderBy(x.fromDate_f5440273).queryList();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            // filter out the old prices
            prices = EntityUtil.filterByDate(prices);

            // first check for the AVERAGE_COST price type
            List<GenericValue> pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap(x.productPriceTypeId, x.AVERAGE_COST));
            if (UtilValidate.isEmpty(pricesToUse)) {
                // next go with default price
                pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap(x.productPriceTypeId, x.DEFAULT_PRICE));
                if (UtilValidate.isEmpty(pricesToUse)) {
                    // finally use list price
                    pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap(x.productPriceTypeId, x.LIST_PRICE));
                }
            }

            // use the most current price
            GenericValue thisPrice = EntityUtil.getFirst(pricesToUse);
            if (thisPrice != null) {
                price = thisPrice.getBigDecimal(x.price);
                validPriceFound = true;
            }
        }

        result.put(x.price, price);
        result.put(x.validPriceFound, validPriceFound);
        result.put(x.orderItemPriceInfos, orderItemPriceInfos);
        return result;
    }

    private static final class DaoQuery {
        private static final String DAO_CLASS_PREFIX = x.org_apache_ofbiz_persistence_dao;
        private static final String DAO_CLASS_SUFFIX = x.Dao;

        private final Delegator delegator;
        private String entityName;
        private final Map<String, Object> whereMap = new LinkedHashMap<>();
        private final List<String> orderByFields = new ArrayList<>();
        private boolean needDateFilter;
        private Timestamp filterMoment;

        private DaoQuery(Delegator delegator) {
            this.delegator = Objects.requireNonNull(delegator, x.delegator_must_not_be_null);
        }

        static DaoQuery use(Delegator delegator) {
            return new DaoQuery(delegator);
        }

        DaoQuery from(String entityName) {
            this.entityName = entityName;
            return this;
        }

        DaoQuery where(Object... fieldValuePairs) {
            if (fieldValuePairs.length == 1 && fieldValuePairs[0] instanceof Map) {
                @SuppressWarnings(x.unchecked)
                Map<String, ? extends Object> map = (Map<String, ? extends Object>) fieldValuePairs[0];
                return where(map);
            }

            if (fieldValuePairs.length % 2 != 0) {
                throw new IllegalArgumentException(x.fieldValuePairs_must_be_key_value_pairs);
            }

            for (int i = 0; i < fieldValuePairs.length; i += 2) {
                String fieldName = String.valueOf(fieldValuePairs[i]);
                whereMap.put(fieldName, fieldValuePairs[i + 1]);
            }
            return this;
        }

        DaoQuery where(Map<String, ? extends Object> fields) {
            if (fields != null) {
                whereMap.putAll(fields);
            }
            return this;
        }

        DaoQuery orderBy(String... fields) {
            if (fields != null) {
                for (String field : fields) {
                    if (UtilValidate.isNotEmpty(field)) {
                        orderByFields.add(field);
                    }
                }
            }
            return this;
        }

        DaoQuery cache() {
            return this;
        }

        DaoQuery cache(boolean ignored) {
            return this;
        }

        DaoQuery filterByDate() {
            this.needDateFilter = true;
            this.filterMoment = null;
            return this;
        }

        DaoQuery filterByDate(Timestamp moment) {
            this.needDateFilter = true;
            this.filterMoment = moment;
            return this;
        }

        List<GenericValue> queryList() throws GenericEntityException {
            if (UtilValidate.isEmpty(entityName)) {
                throw new GenericEntityException(x.Entity_name_must_be_specified_before_query_execution);
            }

            if (x.AgreementItemAndProductAppl.equals(entityName)) {
                return filterByDateIfNeeded(queryAgreementItemAndProductAppl());
            }

            Dao<?, ?, ?> dao = resolveDao(entityName);
            Condition queryCondition = buildCondition();

            try {
                List<GenericValue> values = dao.query(queryCondition, (rs, labels) -> readGenericValues(rs, labels, entityName));
                return filterByDateIfNeeded(values);
            } catch (SQLException e) {
                throw new GenericEntityException(x.Failed_to_query_entity_via_DAO + entityName, e);
            }
        }

        GenericValue queryOne() throws GenericEntityException {
            return EntityUtil.getFirst(queryList());
        }

        GenericValue queryFirst() throws GenericEntityException {
            return EntityUtil.getFirst(queryList());
        }

        private Condition buildCondition() {
            Condition whereCondition = buildWhereCondition();
            if (orderByFields.isEmpty()) {
                return whereCondition;
            }

            Criteria criteria = Filters.criteria();
            if (!whereMap.isEmpty()) {
                criteria.where(whereCondition);
            }

            Map<String, SortDirection> orders = new LinkedHashMap<>();
            for (String orderByField : orderByFields) {
                if (orderByField.startsWith(x.str_3bc15c8a)) {
                    orders.put(orderByField.substring(1), SortDirection.DESC);
                } else {
                    orders.put(orderByField, SortDirection.ASC);
                }
            }
            criteria.orderBy(orders);

            return criteria;
        }

        private Condition buildWhereCondition() {
            if (whereMap.isEmpty()) {
                return Filters.alwaysTrue();
            }

            List<Condition> conditions = new ArrayList<>(whereMap.size());
            for (Map.Entry<String, Object> entry : whereMap.entrySet()) {
                if (entry.getValue() == null) {
                    conditions.add(Filters.isNull(entry.getKey()));
                } else {
                    conditions.add(Filters.eq(entry.getKey(), entry.getValue()));
                }
            }

            return Filters.and(conditions);
        }

        private List<GenericValue> filterByDateIfNeeded(List<GenericValue> values) {
            if (!needDateFilter || UtilValidate.isEmpty(values)) {
                return values;
            }

            if (filterMoment != null) {
                return EntityUtil.filterByDate(values, filterMoment);
            }

            return EntityUtil.filterByDate(values, true);
        }

        private Dao<?, ?, ?> resolveDao(String entityName) throws GenericEntityException {
            try {
                @SuppressWarnings({ x.rawtypes, x.unchecked })
                Class<Dao<?, ?, ?>> daoClass = (Class) Class.forName(DAO_CLASS_PREFIX + entityName + DAO_CLASS_SUFFIX);
                @SuppressWarnings({ x.rawtypes, x.unchecked })
                Dao<?, ?, ?> dao = (Dao<?, ?, ?>) DaoRegistry.getDao(delegator, entityName, (Class) daoClass);
                return dao;
            } catch (ClassNotFoundException e) {
                throw new GenericEntityException(x.No_DAO_implementation_found_for_entity + entityName, e);
            }
        }

        private List<GenericValue> queryAgreementItemAndProductAppl() throws GenericEntityException {
            Object agreementId = whereMap.get(x.agreementId);
            Object productId = whereMap.get(x.productId);
            Object currencyUomId = whereMap.get(x.currencyUomId);

            if (agreementId == null || productId == null) {
                return new LinkedList<>();
            }

            List<GenericValue> productAppls = DaoQuery.use(delegator)
                    .from(x.AgreementProductAppl)
                    .where(x.agreementId, agreementId, x.productId, productId)
                    .queryList();

            List<GenericValue> results = new LinkedList<>();
            for (GenericValue productAppl : productAppls) {
                GenericValue agreementItem = DaoQuery.use(delegator)
                        .from(x.AgreementItem)
                        .where(x.agreementId, productAppl.getString(x.agreementId), x.agreementItemSeqId, productAppl.getString(x.agreementItemSeqId))
                        .queryOne();

                if (agreementItem == null) {
                    continue;
                }

                Map<String, Object> mergedFields = new HashMap<>();
                mergedFields.putAll(UtilGenerics.cast(productAppl));
                mergedFields.putAll(UtilGenerics.cast(agreementItem));

                GenericValue mergedValue = delegator.makeValue(x.AgreementItemAndProductAppl, mergedFields);
                if (currencyUomId == null || currencyUomId.equals(mergedValue.get(x.currencyUomId))) {
                    results.add(mergedValue);
                }
            }

            return results;
        }

        private List<GenericValue> readGenericValues(ResultSet rs, List<String> labels, String entityName) throws SQLException {
            List<GenericValue> results = new LinkedList<>();
            while (rs.next()) {
                Map<String, Object> fields = new HashMap<>();
                for (int i = 0; i < labels.size(); i++) {
                    fields.put(labels.get(i), rs.getObject(i + 1));
                }
                results.add(delegator.makeValue(entityName, fields));
            }
            return results;
        }
    }
}

