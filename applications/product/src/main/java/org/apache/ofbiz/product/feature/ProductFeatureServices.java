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
package org.apache.ofbiz.product.feature;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductAssocDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureGroupApplDao;
import org.apache.ofbiz.persistence.entity.ProductAssocEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureGroupApplEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ProductFeatureServicesContext;
/**
 * Services for product features
 */

public class ProductFeatureServices {

    private static final String MODULE = ProductFeatureServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;

    /*
     * Parameters: productFeatureCategoryId, productFeatureGroupId, productId, productFeatureApplTypeId
     * Result: productFeaturesByType, a Map of all product features from productFeatureCategoryId, group by productFeatureType ->
     * List of productFeatures
     * If the parameter were productFeatureCategoryId, the results are from ProductFeatures.  If productFeatureCategoryId were null
     * and there were a productFeatureGroupId,
     * the results are from ProductFeatureGroupAndAppl.  Otherwise, if there is a productId, the results are from ProductFeatureAndAppl.
     * The optional productFeatureApplTypeId causes results to be filtered by this parameter--only used in conjunction with productId.
     */
    public static Map<String, Object> getProductFeaturesByType(DispatchContext dctx, ProductFeatureServicesContext context) {
        Map<String, Object> results;
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        /* because we might need to search either for product features or for product features of a product, the search code has to be generic.
         * we will determine which entity and field to search on based on what the user has supplied us with.
         */
        String valueToSearch = (String) context.get(x.productFeatureCategoryId);
        String productFeatureApplTypeId = (String) context.get(x.productFeatureApplTypeId);

        String entityToSearch = x.ProductFeature;
        String fieldToSearch = x.productFeatureCategoryId;
        List<String> orderBy = UtilMisc.toList(x.productFeatureTypeId, x.description);

        if (valueToSearch == null && context.get(x.productFeatureGroupId) != null) {
            entityToSearch = x.ProductFeatureGroupAndAppl;
            fieldToSearch = x.productFeatureGroupId;
            valueToSearch = (String) context.get(x.productFeatureGroupId);
            // use same orderBy as with a productFeatureCategoryId search
        } else if (valueToSearch == null && context.get(x.productId) != null) {
            entityToSearch = x.ProductFeatureAndAppl;
            fieldToSearch = x.productId;
            valueToSearch = (String) context.get(x.productId);
            orderBy = UtilMisc.toList(x.sequenceNum, x.productFeatureApplTypeId, x.productFeatureTypeId, x.description);
        }

        if (valueToSearch == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ProductFeatureByType, locale));
        }

        try {
            ProductFeatureDao productFeatureDao = DaoRegistry.getDao(delegator, x.ProductFeature, ProductFeatureDao.class);
            List<GenericValue> allFeatures = new LinkedList<>();

            if (x.ProductFeature.equals(entityToSearch)) {
                List<ProductFeatureEntity> productFeatureEntities = productFeatureDao.list(Filters.eq(fieldToSearch, valueToSearch));
                for (ProductFeatureEntity productFeatureEntity : productFeatureEntities) {
                    allFeatures.add(delegator.makeValue(x.ProductFeature, Beans.beanToMap(productFeatureEntity)));
                }
            } else if (x.ProductFeatureGroupAndAppl.equals(entityToSearch)) {
                ProductFeatureGroupApplDao productFeatureGroupApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureGroupAppl,
                        ProductFeatureGroupApplDao.class);
                List<ProductFeatureGroupApplEntity> productFeatureGroupApplEntities = productFeatureGroupApplDao
                        .list(Filters.eq(fieldToSearch, valueToSearch));
                Map<String, ProductFeatureEntity> featureById = new HashMap<>();
                for (ProductFeatureGroupApplEntity productFeatureGroupApplEntity : productFeatureGroupApplEntities) {
                    String featureId = productFeatureGroupApplEntity.getProductFeatureId();
                    ProductFeatureEntity productFeatureEntity = featureById.get(featureId);
                    if (productFeatureEntity == null && !featureById.containsKey(featureId)) {
                        productFeatureEntity = productFeatureDao.get(featureId).orElse(null);
                        featureById.put(featureId, productFeatureEntity);
                    }
                    if (productFeatureEntity == null) {
                        continue;
                    }
                    Map<String, Object> mergedFields = new HashMap<>(Beans.beanToMap(productFeatureEntity));
                    mergedFields.putAll(Beans.beanToMap(productFeatureGroupApplEntity));
                    allFeatures.add(delegator.makeValue(x.ProductFeatureGroupAndAppl, mergedFields));
                }
            } else if (x.ProductFeatureAndAppl.equals(entityToSearch)) {
                ProductFeatureApplDao productFeatureApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class);
                List<ProductFeatureApplEntity> productFeatureApplEntities = productFeatureApplDao.list(Filters.eq(fieldToSearch, valueToSearch));
                Map<String, ProductFeatureEntity> featureById = new HashMap<>();
                for (ProductFeatureApplEntity productFeatureApplEntity : productFeatureApplEntities) {
                    String featureId = productFeatureApplEntity.getProductFeatureId();
                    ProductFeatureEntity productFeatureEntity = featureById.get(featureId);
                    if (productFeatureEntity == null && !featureById.containsKey(featureId)) {
                        productFeatureEntity = productFeatureDao.get(featureId).orElse(null);
                        featureById.put(featureId, productFeatureEntity);
                    }
                    if (productFeatureEntity == null) {
                        continue;
                    }
                    Map<String, Object> mergedFields = new HashMap<>(Beans.beanToMap(productFeatureEntity));
                    mergedFields.putAll(Beans.beanToMap(productFeatureApplEntity));
                    allFeatures.add(delegator.makeValue(x.ProductFeatureAndAppl, mergedFields));
                }
            }
            allFeatures.sort(createOrderComparator(orderBy));

            if (x.ProductFeatureAndAppl.equals(entityToSearch) && productFeatureApplTypeId != null) {
                allFeatures = EntityUtil.filterByAnd(allFeatures, UtilMisc.toMap(x.productFeatureApplTypeId, productFeatureApplTypeId));
            }

            List<String> featureTypes = new LinkedList<>();
            Map<String, List<GenericValue>> featuresByType = new LinkedHashMap<>();
            for (GenericValue feature: allFeatures) {
                String featureType = feature.getString(x.productFeatureTypeId);
                if (!featureTypes.contains(featureType)) {
                    featureTypes.add(featureType);
                }
                List<GenericValue> features = featuresByType.get(featureType);
                if (features == null) {
                    features = new LinkedList<>();
                    featuresByType.put(featureType, features);
                }
                features.add(feature);
            }

            results = ServiceUtil.returnSuccess();
            results.put(x.productFeatureTypes, featureTypes);
            results.put(x.productFeaturesByType, featuresByType);
        } catch (Exception ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }

    /*
     * Parameter: productId, productFeatureAppls (a List of ProductFeatureAndAppl entities of features applied to productId)
     * Result: variantProductIds: a List of productIds of variants with those features
     */
    public static Map<String, Object> getAllExistingVariants(DispatchContext dctx, ProductFeatureServicesContext context) {
        Map<String, Object> results;
        Delegator delegator = dctx.getDelegator();

        String productId = (String) context.get(x.productId);
        List<String> curProductFeatureAndAppls = UtilGenerics.cast(context.get(x.productFeatureAppls));
        List<String> existingVariantProductIds = new LinkedList<>();

        try {
            /*
             * get a list of all products which are associated with the current one as PRODUCT_VARIANT and for each one,
             * see if it has every single feature in the list of productFeatureAppls as a STANDARD_FEATURE.  If so, then
             * it qualifies and add it to the list of existingVariantProductIds.
             */
            ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
            ProductFeatureApplDao productFeatureApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class);
            List<ProductAssocEntity> productAssocEntities = productAssocDao.list(Filters.and(
                    Filters.eq(x.productId, productId),
                    Filters.eq(x.productAssocTypeId, x.PRODUCT_VARIANT)));
            List<GenericValue> productAssocs = new LinkedList<>();
            for (ProductAssocEntity productAssocEntity : productAssocEntities) {
                productAssocs.add(delegator.makeValue(x.ProductAssoc, Beans.beanToMap(productAssocEntity)));
            }
            productAssocs = EntityUtil.filterByDate(productAssocs);
            for (GenericValue productAssoc: productAssocs) {

                //for each associated product, if it has all standard features, display it's productId
                boolean hasAllFeatures = true;
                for (String productFeatureAndAppl: curProductFeatureAndAppls) {
                    Map<String, String> findByMap = UtilMisc.toMap(x.productId, productAssoc.getString(x.productIdTo),
                            x.productFeatureId, productFeatureAndAppl,
                            x.productFeatureApplTypeId, x.STANDARD_FEATURE);

                    List<ProductFeatureApplEntity> standardProductFeatureAndApplEntities = productFeatureApplDao.list(Filters.and(
                            Filters.eq(x.productId, findByMap.get(x.productId)),
                            Filters.eq(x.productFeatureId, findByMap.get(x.productFeatureId)),
                            Filters.eq(x.productFeatureApplTypeId, findByMap.get(x.productFeatureApplTypeId))));
                    List<GenericValue> standardProductFeatureAndAppls = new LinkedList<>();
                    for (ProductFeatureApplEntity productFeatureApplEntity : standardProductFeatureAndApplEntities) {
                        standardProductFeatureAndAppls.add(delegator.makeValue(x.ProductFeatureAppl, Beans.beanToMap(productFeatureApplEntity)));
                    }
                    standardProductFeatureAndAppls = EntityUtil.filterByDate(standardProductFeatureAndAppls);
                    if (UtilValidate.isEmpty(standardProductFeatureAndAppls)) {
                        hasAllFeatures = false;
                        break;
                    }
                }
                if (hasAllFeatures) {
                    // add to list of existing variants: productId=productAssoc.productIdTo
                    existingVariantProductIds.add(productAssoc.getString(x.productIdTo));
                }
            }
            results = ServiceUtil.returnSuccess();
            results.put(x.variantProductIds, existingVariantProductIds);
        } catch (Exception ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }

    private static Comparator<GenericValue> createOrderComparator(List<String> orderBy) {
        return (left, right) -> {
            for (String orderByField : orderBy) {
                boolean descending = false;
                String fieldName = orderByField;
                if (orderByField.startsWith(x.str_3bc15c8a)) {
                    descending = true;
                    fieldName = orderByField.substring(1);
                }

                int cmp = compareFieldValues(left.get(fieldName), right.get(fieldName));
                if (cmp != 0) {
                    return descending ? -cmp : cmp;
                }
            }

            return 0;
        };
    }

    private static int compareFieldValues(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal) {
            return ((BigDecimal) left).compareTo((BigDecimal) right);
        }
        if (left instanceof Comparable && right instanceof Comparable && left.getClass().isAssignableFrom(right.getClass())) {
            @SuppressWarnings(x.unchecked)
            Comparable<Object> comparableLeft = (Comparable<Object>) left;
            return comparableLeft.compareTo(right);
        }
        return left.toString().compareTo(right.toString());
    }

    /*
     * Parameter: productId (of the parent product which has SELECTABLE features)
     * Result: featureCombinations, a List of Maps containing, for each possible variant of the productid:
     * {defaultVariantProductId: id of this variant; curProductFeatureAndAppls: features applied to this variant;
     * existingVariantProductIds: List of productIds which are already variants with these features }
     */
    public static Map<String, Object> getVariantCombinations(DispatchContext dctx, ProductFeatureServicesContext context) {
        Map<String, Object> results;
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String productId = (String) context.get(x.productId);

        try {
            Map<String, Object> featuresResults = dispatcher.runSync(x.getProductFeaturesByType, UtilMisc.toMap(x.productId, productId));
            Map<String, List<GenericValue>> features;

            if (featuresResults.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                features = UtilGenerics.cast(featuresResults.get(x.productFeaturesByType));
            } else {
                return ServiceUtil.returnError((String) featuresResults.get(ModelService.ERROR_MESSAGE_LIST));
            }

            // need to keep 2 lists, oldCombinations and newCombinations, and keep swapping them after each looping.  Otherwise, you'll get a
            // concurrent modification exception
            List<Map<String, Object>> oldCombinations = new LinkedList<>();

            // loop through each feature type
            for (Map.Entry<String, List<GenericValue>> entry: features.entrySet()) {
                List<GenericValue> currentFeatures = entry.getValue();

                List<Map<String, Object>> newCombinations = new LinkedList<>();
                List<Map<String, Object>> combinations;

                // start with either existing combinations or from scratch
                if (!oldCombinations.isEmpty()) {
                    combinations = oldCombinations;
                } else {
                    combinations = new LinkedList<>();
                }

                // in both cases, use each feature of current feature type's idCode and
                // product feature and add it to the id code and product feature applications
                // of the next variant.  just a matter of whether we're starting with an
                // existing list of features and id code or from scratch.
                if (combinations.isEmpty()) {
                    for (GenericValue currentFeature: currentFeatures) {
                        if (x.SELECTABLE_FEATURE.equals(currentFeature.getString(x.productFeatureApplTypeId))) {
                            Map<String, Object> newCombination = new HashMap<>();
                            List<GenericValue> newFeatures = new LinkedList<>();
                            List<String> newFeatureIds = new LinkedList<>();
                            if (currentFeature.getString(x.idCode) != null) {
                                newCombination.put(x.defaultVariantProductId, productId + currentFeature.getString(x.idCode));
                            } else {
                                newCombination.put(x.defaultVariantProductId, productId);
                            }
                            newFeatures.add(currentFeature);
                            newFeatureIds.add(currentFeature.getString(x.productFeatureId));
                            newCombination.put(x.curProductFeatureAndAppls, newFeatures);
                            newCombination.put(x.curProductFeatureIds, newFeatureIds);
                            newCombinations.add(newCombination);
                        }
                    }
                } else {
                    for (Map<String, Object> combination: combinations) {
                        for (GenericValue currentFeature: currentFeatures) {
                            if (x.SELECTABLE_FEATURE.equals(currentFeature.getString(x.productFeatureApplTypeId))) {
                                Map<String, Object> newCombination = new HashMap<>();
                                // .clone() is important, or you'll keep adding to the same List for all the variants
                                // have to cast twice: once from get() and once from clone()
                                List<GenericValue> newFeatures = UtilMisc.makeListWritable(UtilGenerics.cast(combination
                                        .get(x.curProductFeatureAndAppls)));
                                List<String> newFeatureIds = UtilMisc.makeListWritable(UtilGenerics.cast(combination.get(x.curProductFeatureIds)));
                                if (currentFeature.getString(x.idCode) != null) {
                                    newCombination.put(x.defaultVariantProductId, combination.get(x.defaultVariantProductId)
                                            + currentFeature.getString(x.idCode));
                                } else {
                                    newCombination.put(x.defaultVariantProductId, combination.get(x.defaultVariantProductId));
                                }
                                newFeatures.add(currentFeature);
                                newFeatureIds.add(currentFeature.getString(x.productFeatureId));
                                newCombination.put(x.curProductFeatureAndAppls, newFeatures);
                                newCombination.put(x.curProductFeatureIds, newFeatureIds);
                                newCombinations.add(newCombination);
                            }
                        }
                    }
                }
                if (newCombinations.size() >= oldCombinations.size()) {
                    oldCombinations = newCombinations; // save the newly expanded list as oldCombinations
                }
            }

            int defaultCodeCounter = 1;
            Set<String> defaultVariantProductIds = new HashSet<>(); // this map will contain the codes already used (as keys)
            defaultVariantProductIds.add(productId);

            // now figure out which of these combinations already have productIds associated with them
            for (Map<String, Object> combination: oldCombinations) {
                // Verify if the default code is already used, if so add a numeric suffix
                if (defaultVariantProductIds.contains(combination.get(x.defaultVariantProductId))) {
                    combination.put(x.defaultVariantProductId, combination.get(x.defaultVariantProductId) + x.str_3bc15c8a + (defaultCodeCounter < 10 ? x._0
                            + defaultCodeCounter : x.emptyString + defaultCodeCounter));
                    defaultCodeCounter++;
                }
                defaultVariantProductIds.add((String) combination.get(x.defaultVariantProductId));
                results = dispatcher.runSync(x.getAllExistingVariants, UtilMisc.toMap(x.productId, productId,
                                             x.productFeatureAppls, combination.get(x.curProductFeatureIds)));
                combination.put(x.existingVariantProductIds, results.get(x.variantProductIds));
            }
            results = ServiceUtil.returnSuccess();
            results.put(x.featureCombinations, oldCombinations);
        } catch (GenericServiceException ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return results;
    }

    /*
     * Parameters: productCategoryId (String) and productFeatures (a List of ProductFeature GenericValues)
     * Result: products (a List of Product GenericValues)
     */
    public static Map<String, Object> getCategoryVariantProducts(DispatchContext dctx, ProductFeatureServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        List<GenericValue> productFeatures = UtilGenerics.cast(context.get(x.productFeatures));
        String productCategoryId = (String) context.get(x.productCategoryId);
        Locale locale = (Locale) context.get(x.locale);

        // get all the product members of the product category
        Map<String, Object> result;
        try {
            result = dispatcher.runSync(x.getProductCategoryMembers, UtilMisc.toMap(x.categoryId, productCategoryId));
        } catch (GenericServiceException ex) {
            Debug.logError(x.Cannot_get_category_memebers_for + productCategoryId + x.due_to_error + ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }

        List<GenericValue> memberProducts = UtilGenerics.cast(result.get(x.categoryMembers));
        if ((memberProducts != null) && (!memberProducts.isEmpty())) {
            // construct a Map of productFeatureTypeId -> productFeatureId from the productFeatures List
            Map<String, String> featuresByType = new HashMap<>();
            for (GenericValue nextFeature: productFeatures) {
                featuresByType.put(nextFeature.getString(x.productFeatureTypeId), nextFeature.getString(x.productFeatureId));
            }

            List<GenericValue> products = new LinkedList<>(); // final list of variant products
            for (GenericValue memberProduct: memberProducts) {
                // find variants for each member product of the category

                try {
                    result = dispatcher.runSync(x.getProductVariant, UtilMisc.toMap(x.productId, memberProduct.getString(x.productId),
                            x.selectedFeatures, featuresByType));
                } catch (GenericServiceException ex) {
                    Debug.logError(x.Cannot_get_product_variants_for + memberProduct.getString(x.productId) + x.due_to_error
                            + ex.getMessage(), MODULE);
                    return ServiceUtil.returnError(ex.getMessage());
                }

                List<GenericValue> variantProducts = UtilGenerics.cast(result.get(x.products));
                if ((variantProducts != null) && (!variantProducts.isEmpty())) {
                    products.addAll(variantProducts);
                } else {
                    Debug.logWarning(x.Product_c553701d + memberProduct.getString(x.productId) + x.did_not_have_any_variants_for_the_given_features, MODULE);
                }
            }

            if (products.isEmpty()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ProductCategoryNoVariants, locale));
            } else {
                results = ServiceUtil.returnSuccess();
                results.put(x.products, products);
            }

        } else {
            Debug.logWarning(x.No_products_found_in + productCategoryId, MODULE);
        }

        return results;
    }
}
