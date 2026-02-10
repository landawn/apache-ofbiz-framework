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
package org.apache.ofbiz.product.product;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.GoodIdentificationDao;
import org.apache.ofbiz.persistence.dao.ProductAssocDao;
import org.apache.ofbiz.persistence.dao.ProductAttributeDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryMemberDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryRollupDao;
import org.apache.ofbiz.persistence.dao.ProductContentDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureCatGrpApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureGroupApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureGroupDao;
import org.apache.ofbiz.persistence.dao.ProductPriceDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ProductUtilServicesContext;
/**
 * Product Services
 */
public final class ProductUtilServices {

    private static final String MODULE = ProductUtilServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final String RES_ERROR = x.ProductErrorUiLabels;

    private ProductUtilServices() {
    }

    /**
     * First expire all ProductAssocs for all disc variants, then disc all virtuals that have all expired variant ProductAssocs
     */
    public static Map<String, Object> discVirtualsWithDiscVariants(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;
        EntityCondition conditionOne = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(x.isVariant, EntityOperator.EQUALS, x.Y),
                EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)), EntityOperator.AND);

        try (EntityListIterator eliOne = productDao.findIteratorByWhere(delegator, x.Product, conditionOne, null, null, null)) {
            GenericValue productOne = null;
            int numSoFarOne = 0;
            while ((productOne = eliOne.next()) != null) {
                String virtualProductId = ProductWorker.getVariantVirtualId(productOne);
                GenericValue virtualProduct = productDao.findOneByWhere(delegator, x.Product, UtilMisc.toMap(x.productId, virtualProductId),
                        null, null, false);
                if (virtualProduct == null) {
                    continue;
                }
                List<GenericValue> passocList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                        UtilMisc.toMap(x.productId, virtualProductId, x.productIdTo, productOne.get(x.productId), x.productAssocTypeId,
                                x.PRODUCT_VARIANT),
                        null, null, false, true);
                if (!passocList.isEmpty()) {
                    for (GenericValue passoc : passocList) {
                        passoc.set(x.thruDate, nowTimestamp);
                        passoc.store();
                    }

                    numSoFarOne++;
                    if (numSoFarOne % 500 == 0) {
                        Debug.logInfo(x.Expired_variant_ProductAssocs_for + numSoFarOne + x.sales_discontinued_variant_products, MODULE);
                    }
                }
            }
            // get all non-discontinued virtuals, see if all variant ProductAssocs are expired, if discontinue
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(x.isVirtual, EntityOperator.EQUALS, x.Y),
                    EntityCondition.makeCondition(EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.GREATER_THAN_EQUAL_TO,
                                    nowTimestamp))), EntityOperator.AND);
            try (EntityListIterator eli = productDao.findIteratorByWhere(delegator, x.Product, condition, null, null, null)) {
                GenericValue product = null;
                int numSoFar = 0;
                while ((product = eli.next()) != null) {
                    List<GenericValue> passocList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                            UtilMisc.toMap(x.productId, product.get(x.productId), x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false,
                            true);
                    if (passocList.isEmpty()) {
                        product.set(x.salesDiscontinuationDate, nowTimestamp);
                        delegator.store(product);

                        numSoFar++;
                        if (numSoFar % 500 == 0) {
                            Debug.logInfo(x.Sales_discontinued + numSoFar + x.virtual_products_that_have_no_valid_variants, MODULE);
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
                errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_discVirtualsWithDiscVariants,
                        messageMap, locale);
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_discVirtualsWithDiscVariants,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * for all disc products, remove from category memberships
     */
    public static Map<String, Object> removeCategoryMembersOfDiscProducts(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember,
                ProductCategoryMemberDao.class);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)), EntityOperator.AND);

        try (EntityListIterator eli = productDao.findIteratorByWhere(delegator, x.Product, condition, null, null, null)) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                String productId = product.getString(x.productId);
                List<GenericValue> productCategoryMemberList = productCategoryMemberDao.findListByWhere(delegator, x.ProductCategoryMember,
                        UtilMisc.toMap(x.productId, productId), null, null, false);
                if (!productCategoryMemberList.isEmpty()) {
                    for (GenericValue productCategoryMember : productCategoryMemberList) {
                        // coded this way rather than a removeByAnd so it can be easily changed...
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo(x.Removed_category_members_for + numSoFar + x.sales_discontinued_products, MODULE);
                    }
                }
            }
            Debug.logInfo(x.Completed_Removed_category_members_for + numSoFar + x.sales_discontinued_products, MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_removeCategoryMembersOfDiscProducts,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeDuplicateOpenEndedCategoryMembers(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember,
                ProductCategoryMemberDao.class);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity(x.PCM, x.ProductCategoryMember);
        dve.addAlias(x.PCM, x.productId, null, null, null, Boolean.TRUE, null);
        dve.addAlias(x.PCM, x.productCategoryId, null, null, null, Boolean.TRUE, null);
        dve.addAlias(x.PCM, x.fromDate, null, null, null, null, null);
        dve.addAlias(x.PCM, x.thruDate, null, null, null, null, null);
        dve.addAlias(x.PCM, x.productIdCount, x.productId, null, null, null, x.count);

        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN, nowTimestamp),
                EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null)), EntityOperator.AND);
        EntityCondition havingCond = EntityCondition.makeCondition(x.productIdCount, EntityOperator.GREATER_THAN, 1L);

        try (EntityListIterator eli = productCategoryMemberDao.findIteratorByCondition(delegator, dve, condition, havingCond,
                UtilMisc.toList(x.productId, x.productCategoryId, x.productIdCount), null, null)) {
            GenericValue pcm = null;
            int numSoFar = 0;
            while ((pcm = eli.next()) != null) {
                List<GenericValue> productCategoryMemberList = productCategoryMemberDao.findListByWhere(delegator, x.ProductCategoryMember,
                        UtilMisc.toMap(x.productId, pcm.get(x.productId), x.productCategoryId, pcm.get(x.productCategoryId)), null, null, false);
                if (productCategoryMemberList.size() > 1) {
                    // remove all except the first...
                    productCategoryMemberList.remove(0);
                    for (GenericValue productCategoryMember : productCategoryMemberList) {
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo(x.Removed_category_members_for + numSoFar + x.products_with_duplicate_category_members, MODULE);
                    }
                }
            }
            Debug.logInfo(x.Completed_Removed_category_members_for + numSoFar + x.products_with_duplicate_category_members, MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_removeDuplicateOpenEndedCategoryMembers,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> makeStandAloneFromSingleVariantVirtuals(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;

        Debug.logInfo(x.Starting_makeStandAloneFromSingleVariantVirtuals, MODULE);

        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity(x.PVIRT, x.Product);
        dve.addMemberEntity(x.PVA, x.ProductAssoc);
        dve.addViewLink(x.PVIRT, x.PVA, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.productId, x.productId)));
        dve.addAlias(x.PVIRT, x.productId, null, null, null, Boolean.TRUE, null);
        dve.addAlias(x.PVIRT, x.salesDiscontinuationDate, null, null, null, null, null);
        dve.addAlias(x.PVA, x.productAssocTypeId, null, null, null, null, null);
        dve.addAlias(x.PVA, x.fromDate, null, null, null, null, null);
        dve.addAlias(x.PVA, x.thruDate, null, null, null, null, null);
        dve.addAlias(x.PVA, x.productIdToCount, x.productIdTo, null, null, null, x.count_distinct);
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(x.productAssocTypeId, EntityOperator.EQUALS, x.PRODUCT_VARIANT),
                EntityCondition.makeCondition(EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.EQUALS, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.GREATER_THAN, nowTimestamp))), EntityOperator.AND);
        EntityCondition havingCond = EntityCondition.makeCondition(x.productIdToCount, EntityOperator.EQUALS, 1L);

        try (EntityListIterator eliOne = productAssocDao.findIteratorByCondition(delegator, dve, condition, havingCond,
                UtilMisc.toList(x.productId, x.productIdToCount), null, null)) {
            List<GenericValue> valueList = eliOne.getCompleteList();

            Debug.logInfo(x.Found + valueList.size() + x.virtual_products_with_one_variant_to_turn_into_a_stand_alone_product, MODULE);

            int numWithOneOnly = 0;
            for (GenericValue value : valueList) {
                // has only one variant period, is it valid? should already be discontinued if not
                String productId = value.getString(x.productId);
                List<GenericValue> paList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                        UtilMisc.toMap(x.productId, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);
                // verify the query; tested on a bunch, looks good
                if (paList.size() != 1) {
                    Debug.logInfo(x.Virtual_product_with_ID + productId + x.should_have_1_assoc_has + paList.size(), MODULE);
                } else {
                    // for all virtuals with one variant move all info from virtual to variant and remove virtual, make variant as not a variant
                    dispatcher.runSync(x.mergeVirtualWithSingleVariant, UtilMisc.<String, Object>toMap(x.productId, productId, x.removeOld,
                            Boolean.TRUE, x.userLogin, userLogin));
                    numWithOneOnly++;
                    if (numWithOneOnly % 100 == 0) {
                        Debug.logInfo(x.Made + numWithOneOnly + x.virtual_products_with_only_one_valid_variant_stand_alone_products, MODULE);
                    }
                }
            }

            EntityCondition conditionWithDates = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(x.productAssocTypeId, EntityOperator.EQUALS, x.PRODUCT_VARIANT),
                    EntityCondition.makeCondition(EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition(x.salesDiscontinuationDate, EntityOperator.GREATER_THAN, nowTimestamp)),
                    EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null), EntityOperator.OR,
                            EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))), EntityOperator.AND);
            try (EntityListIterator eliMulti = productAssocDao.findIteratorByCondition(delegator, dve, conditionWithDates, havingCond,
                    UtilMisc.toList(x.productId, x.productIdToCount), null, null)) {
                List<GenericValue> valueMultiList = eliMulti.getCompleteList();
                Debug.logInfo(x.Found + valueMultiList.size() + x.virtual_products_with_one_VALID_variant_to_pull_the_variant_from
                        + x.to_make_a_stand_alone_product, MODULE);

                int numWithOneValid = 0;
                for (GenericValue value : valueMultiList) {
                    // has only one valid variant
                    String productId = value.getString(x.productId);

                    List<GenericValue> paList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                            UtilMisc.toMap(x.productId, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);

                    // verify the query; tested on a bunch, looks good
                    if (paList.size() != 1) {
                        Debug.logInfo(x.Virtual_product_with_ID + productId + x.should_have_1_assoc_has + paList.size(), MODULE);
                    } else {
                        // for all virtuals with one valid variant move info from virtual to variant, put variant in categories from virtual, remove
                        // virtual from all categories but leave "family" otherwise intact, mark variant as not a variant
                        dispatcher.runSync(x.mergeVirtualWithSingleVariant, UtilMisc.<String, Object>toMap(x.productId, productId, x.removeOld,
                                Boolean.FALSE, x.userLogin, userLogin));

                        numWithOneValid++;
                        if (numWithOneValid % 100 == 0) {
                            Debug.logInfo(x.Made + numWithOneValid + x.virtual_products_with_one_valid_variant_stand_alone_products, MODULE);
                        }
                    }
                }
                Debug.logInfo(x.Found_virtual_products_with_one_valid_variant + numWithOneValid + x.with_one_variant_only + numWithOneOnly,
                        MODULE);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
                errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_makeStandAloneFromSingleVariantVirtuals,
                        messageMap, locale);
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_makeStandAloneFromSingleVariantVirtuals,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> mergeVirtualWithSingleVariant(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String productId = (String) context.get(x.productId);
        Boolean removeOldBool = (Boolean) context.get(x.removeOld);
        boolean removeOld = removeOldBool;
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;

        Boolean testBool = (Boolean) context.get(x.test);
        boolean test = false;
        if (testBool != null) {
            test = testBool;
        }

        try {
            GenericValue product = productDao.findOneByWhere(delegator, x.Product, UtilMisc.toMap(x.productId, productId), null, null, false);
            Debug.logInfo(x.Processing_virtual_product_with_one_variant_with_ID + productId + x.and_name
                    + product.getString(x.internalName), MODULE);

            List<GenericValue> paList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                    UtilMisc.toMap(x.productId, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);
            if (paList.size() > 1) {
                Map<String, String> messageMap = UtilMisc.toMap(x.productId, productId);
                errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_found_more_than_one_valid_variant_for_virtual_ID,
                        messageMap, locale);
                Debug.logInfo(errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }

            if (paList.isEmpty()) {
                Map<String, String> messageMap = UtilMisc.toMap(x.productId, productId);
                errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_did_not_find_any_valid_variants_for_virtual_ID,
                        messageMap, locale);
                Debug.logInfo(errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }

            GenericValue productAssoc = EntityUtil.getFirst(paList);
            if (removeOld) {
                // remove the productAssoc before getting down so it isn't copied over...
                if (test) {
                    Debug.logInfo(x.Test_mode_would_remove + productAssoc, MODULE);
                } else {
                    productAssoc.remove();
                }
            } else {
                // don't remove, just expire to avoid running again in the future
                productAssoc.set(x.thruDate, nowTimestamp);
                if (test) {
                    Debug.logInfo(x.Test_mode_would_store + productAssoc, MODULE);
                } else {
                    productAssoc.store();
                }
            }
            String variantProductId = productAssoc.getString(x.productIdTo);

            // Product
            GenericValue variantProduct = productDao.findOneByWhere(delegator, x.Product, UtilMisc.toMap(x.productId, variantProductId), null,
                    null, false);

            Debug.logInfo(x.variant_has_ID + variantProductId + x.and_name + variantProduct.getString(x.internalName), MODULE);

            // start with the values from the virtual product, override from the variant...
            GenericValue newVariantProduct = delegator.makeValue(x.Product, product);
            newVariantProduct.setAllFields(variantProduct, false, x.emptyString, null);
            newVariantProduct.set(x.isVariant, x.N);
            if (test) {
                Debug.logInfo(x.Test_mode_would_store + newVariantProduct, MODULE);
            } else {
                newVariantProduct.store();
            }

            // ProductCategoryMember - always remove these to pull the virtual from any categories it might have been in
            duplicateRelated(product, x.emptyString, x.ProductCategoryMember, x.productId, variantProductId, nowTimestamp, true, delegator, test);

            // ProductFeatureAppl
            duplicateRelated(product, x.emptyString, x.ProductFeatureAppl, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductContent
            duplicateRelated(product, x.emptyString, x.ProductContent, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductPrice
            duplicateRelated(product, x.emptyString, x.ProductPrice, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);

            // GoodIdentification
            duplicateRelated(product, x.emptyString, x.GoodIdentification, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductAttribute
            duplicateRelated(product, x.emptyString, x.ProductAttribute, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductAssoc
            duplicateRelated(product, x.Main, x.ProductAssoc, x.productId, variantProductId, nowTimestamp, removeOld, delegator, test);
            duplicateRelated(product, x.Assoc, x.ProductAssoc, x.productIdTo, variantProductId, nowTimestamp, removeOld, delegator, test);

            if (removeOld) {
                if (test) {
                    Debug.logInfo(x.Test_mode_would_remove_related_ProductKeyword_with_dummy_key
                            + product.getRelatedDummyPK(x.ProductKeyword), MODULE);
                    Debug.logInfo(x.Test_mode_would_remove + product, MODULE);
                } else {
                    product.removeRelated(x.ProductKeyword);
                    product.remove();
                }
            }

            if (test) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ProductMergeVirtualWithSingleVariant, locale));
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_makeStandAloneFromSingleVariantVirtuals,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    protected static void duplicateRelated(GenericValue product, String title, String relatedEntityName, String productIdField, String
            variantProductId, Timestamp nowTimestamp, boolean removeOld, Delegator delegator, boolean test) throws GenericEntityException {
        List<GenericValue> relatedList = EntityUtil.filterByDate(product.getRelated(title + relatedEntityName, null, null, false), nowTimestamp);
        for (GenericValue relatedValue : relatedList) {
            GenericValue newRelatedValue = (GenericValue) relatedValue.clone();
            newRelatedValue.set(productIdField, variantProductId);

            // create a new one? see if one already exists with different from/thru dates
            ModelEntity modelEntity = relatedValue.getModelEntity();
            if (modelEntity.isField(x.fromDate)) {
                GenericPK findValue = newRelatedValue.getPrimaryKey();
                // can't just set to null, need to remove the value so it isn't a constraint in the query
                findValue.remove(x.fromDate);
                List<GenericValue> existingValueList = EntityUtil.filterByDate(
                        findRelatedValuesByWhere(delegator, relatedEntityName, findValue, false), nowTimestamp);
                if (!existingValueList.isEmpty()) {
                    if (test) {
                        Debug.logInfo(x.Found + existingValueList.size() + x.existing_values_for_related_entity_name
                                + relatedEntityName + x.not_copying_findValue_is + findValue, MODULE);
                    }
                    continue;
                }
                newRelatedValue.set(x.fromDate, nowTimestamp);
            }

            if (countRelatedValuesByWhere(delegator, relatedEntityName,
                    EntityCondition.makeCondition(newRelatedValue.getPrimaryKey(), EntityOperator.AND)) == 0) {
                if (test) {
                    Debug.logInfo(x.Test_mode_would_create + newRelatedValue, MODULE);
                } else {
                    newRelatedValue.create();
                }
            }
        }
        if (removeOld) {
            if (test) {
                Debug.logInfo(x.Test_mode_would_remove_related + title + relatedEntityName + x.with_dummy_key
                        + product.getRelatedDummyPK(title + relatedEntityName), MODULE);
            } else {
                product.removeRelated(title + relatedEntityName);
            }
        }
    }

    private static List<GenericValue> findRelatedValuesByWhere(Delegator delegator, String relatedEntityName, Object whereClause,
            boolean useCache) throws GenericEntityException {
        switch (relatedEntityName) {
        case x.ProductCategoryMember:
            return DaoRegistry.getDao(delegator, x.ProductCategoryMember, ProductCategoryMemberDao.class).findListByWhere(delegator,
                    x.ProductCategoryMember, whereClause, null, null, useCache);
        case x.ProductFeatureAppl:
            return DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class).findListByWhere(delegator,
                    x.ProductFeatureAppl, whereClause, null, null, useCache);
        case x.ProductContent:
            return DaoRegistry.getDao(delegator, x.ProductContent, ProductContentDao.class).findListByWhere(delegator, x.ProductContent,
                    whereClause, null, null, useCache);
        case x.ProductPrice:
            return DaoRegistry.getDao(delegator, x.ProductPrice, ProductPriceDao.class).findListByWhere(delegator, x.ProductPrice, whereClause,
                    null, null, useCache);
        case x.GoodIdentification:
            return DaoRegistry.getDao(delegator, x.GoodIdentification, GoodIdentificationDao.class).findListByWhere(delegator,
                    x.GoodIdentification, whereClause, null, null, useCache);
        case x.ProductAttribute:
            return DaoRegistry.getDao(delegator, x.ProductAttribute, ProductAttributeDao.class).findListByWhere(delegator, x.ProductAttribute,
                    whereClause, null, null, useCache);
        case x.ProductAssoc:
            return DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class).findListByWhere(delegator, x.ProductAssoc, whereClause,
                    null, null, useCache);
        default:
            throw new IllegalArgumentException(x.Unsupported_related_entity_for_DAO_query + relatedEntityName);
        }
    }

    private static long countRelatedValuesByWhere(Delegator delegator, String relatedEntityName, Object whereClause)
            throws GenericEntityException {
        switch (relatedEntityName) {
        case x.ProductCategoryMember:
            return DaoRegistry.getDao(delegator, x.ProductCategoryMember, ProductCategoryMemberDao.class).countByWhere(delegator,
                    x.ProductCategoryMember, whereClause, null, null);
        case x.ProductFeatureAppl:
            return DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class).countByWhere(delegator, x.ProductFeatureAppl,
                    whereClause, null, null);
        case x.ProductContent:
            return DaoRegistry.getDao(delegator, x.ProductContent, ProductContentDao.class).countByWhere(delegator, x.ProductContent,
                    whereClause, null, null);
        case x.ProductPrice:
            return DaoRegistry.getDao(delegator, x.ProductPrice, ProductPriceDao.class).countByWhere(delegator, x.ProductPrice, whereClause,
                    null, null);
        case x.GoodIdentification:
            return DaoRegistry.getDao(delegator, x.GoodIdentification, GoodIdentificationDao.class).countByWhere(delegator,
                    x.GoodIdentification, whereClause, null, null);
        case x.ProductAttribute:
            return DaoRegistry.getDao(delegator, x.ProductAttribute, ProductAttributeDao.class).countByWhere(delegator, x.ProductAttribute,
                    whereClause, null, null);
        case x.ProductAssoc:
            return DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class).countByWhere(delegator, x.ProductAssoc, whereClause,
                    null, null);
        default:
            throw new IllegalArgumentException(x.Unsupported_related_entity_for_DAO_count + relatedEntityName);
        }
    }


    /**
     * reset all product image names with a certain pattern, ex: /images/products/${size}/${productId}.jpg
     * NOTE: only works on fields of Product right now
     */
    public static Map<String, Object> setAllProductImageNames(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
        String pattern = (String) context.get(x.pattern);
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;

        if (UtilValidate.isEmpty(pattern)) {
            Map<String, Object> imageContext = new HashMap<>();
            imageContext.putAll(context);
            imageContext.put(x.tenantId, delegator.getDelegatorTenantId());
            String imageFilenameFormat = EntityUtilProperties.getPropertyValue(x.catalog, x.image_filename_format, delegator);
            String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                    x.image_url_prefix, delegator), imageContext);
            imageUrlPrefix = imageUrlPrefix.endsWith(x.str_42099b4a) ? imageUrlPrefix.substring(0, imageUrlPrefix.length() - 1) : imageUrlPrefix;
            pattern = imageUrlPrefix + x.str_42099b4a + imageFilenameFormat;
        }

        try (EntityListIterator eli = productDao.findIteratorByWhere(delegator, x.Product, null, null, null, null)) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                String productId = (String) product.get(x.productId);
                Map<String, String> smallMap = UtilMisc.toMap(x.size, x.small, x.productId, productId);
                Map<String, String> mediumMap = UtilMisc.toMap(x.size, x.medium, x.productId, productId);
                Map<String, String> largeMap = UtilMisc.toMap(x.size, x.large, x.productId, productId);
                Map<String, String> detailMap = UtilMisc.toMap(x.size, x.detail, x.productId, productId);

                if (x.Y.equals(product.getString(x.isVirtual))) {
                    // find the first variant, use it's ID for the names...
                    List<GenericValue> productAssocList = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                            UtilMisc.toMap(x.productId, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);
                    if (!productAssocList.isEmpty()) {
                        GenericValue productAssoc = EntityUtil.getFirst(productAssocList);
                        smallMap.put(x.productId, productAssoc.getString(x.productIdTo));
                        mediumMap.put(x.productId, productAssoc.getString(x.productIdTo));
                        product.set(x.smallImageUrl, FlexibleStringExpander.expandString(pattern, smallMap));
                        product.set(x.mediumImageUrl, FlexibleStringExpander.expandString(pattern, mediumMap));
                    } else {
                        product.set(x.smallImageUrl, null);
                        product.set(x.mediumImageUrl, null);
                    }
                    product.set(x.largeImageUrl, null);
                    product.set(x.detailImageUrl, null);
                } else {
                    product.set(x.smallImageUrl, FlexibleStringExpander.expandString(pattern, smallMap));
                    product.set(x.mediumImageUrl, FlexibleStringExpander.expandString(pattern, mediumMap));
                    product.set(x.largeImageUrl, FlexibleStringExpander.expandString(pattern, largeMap));
                    product.set(x.detailImageUrl, FlexibleStringExpander.expandString(pattern, detailMap));
                }

                product.store();
                numSoFar++;
                if (numSoFar % 500 == 0) {
                    Debug.logInfo(x.Image_URLs_set_for + numSoFar + x.products_c3e2c249, MODULE);
                }
            }
            Debug.logInfo(x.Completed_Image_URLs_set_for + numSoFar + x.products_c3e2c249, MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_setAllProductImageNames, messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearAllVirtualProductImageNames(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;

        try (EntityListIterator eli = productDao.findIteratorByWhere(delegator, x.Product, UtilMisc.toMap(x.isVirtual, x.Y), null, null, null)) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                product.set(x.smallImageUrl, null);
                product.set(x.mediumImageUrl, null);
                product.set(x.largeImageUrl, null);
                product.set(x.detailImageUrl, null);
                product.store();
                numSoFar++;
                if (numSoFar % 500 == 0) {
                    Debug.logInfo(x.Image_URLs_cleared_for + numSoFar + x.products_c3e2c249, MODULE);
                }
            }
            Debug.logInfo(x.Completed_Image_URLs_set_for + numSoFar + x.products_c3e2c249, MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_entity_error_running_clearAllVirtualProductImageNames,
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> attachProductFeaturesToCategory(DispatchContext dctx, ProductUtilServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String productCategoryId = (String) context.get(x.productCategoryId);
        String doSubCategoriesStr = (String) context.get(x.doSubCategories);
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;

        // default to true
        boolean doSubCategories = !x.N.equals(doSubCategoriesStr);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        Set<String> productFeatureTypeIdsToExclude = new HashSet<>();
        String excludeProp = EntityUtilProperties.getPropertyValue(x.prodsearch, x.attach_feature_type_exclude, delegator);
        if (UtilValidate.isNotEmpty(excludeProp)) {
            List<String> typeList = StringUtil.split(excludeProp, x.str_5c10b5b2);
            productFeatureTypeIdsToExclude.addAll(typeList);
        }

        Set<String> productFeatureTypeIdsToInclude = null;
        String includeProp = EntityUtilProperties.getPropertyValue(x.prodsearch, x.attach_feature_type_include, delegator);
        if (UtilValidate.isNotEmpty(includeProp)) {
            List<String> typeList = StringUtil.split(includeProp, x.str_5c10b5b2);
            if (!typeList.isEmpty()) {
                productFeatureTypeIdsToInclude = new LinkedHashSet<>(typeList);
            }
        }

        try {
            attachProductFeaturesToCategory(productCategoryId, productFeatureTypeIdsToInclude, productFeatureTypeIdsToExclude,
                    delegator, doSubCategories, nowTimestamp);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, x.productutilservices_error_in_attachProductFeaturesToCategory, messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Get all features associated with products and associate them with a feature group attached to the category for each feature type;
     * includes products associated with this category only, but will also associate all feature groups of sub-categories with this category,
     * optionally calls this method for all sub-categories too
     */
    public static void attachProductFeaturesToCategory(String productCategoryId, Set<String> productFeatureTypeIdsToInclude, Set<String>
            productFeatureTypeIdsToExclude, Delegator delegator, boolean doSubCategories, Timestamp nowTimestamp) throws GenericEntityException {
        ProductCategoryRollupDao productCategoryRollupDao = DaoRegistry.getDao(delegator, x.ProductCategoryRollup,
                ProductCategoryRollupDao.class);
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember,
                ProductCategoryMemberDao.class);
        ProductFeatureApplDao productFeatureApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class);
        ProductFeatureGroupDao productFeatureGroupDao = DaoRegistry.getDao(delegator, x.ProductFeatureGroup, ProductFeatureGroupDao.class);
        ProductFeatureGroupApplDao productFeatureGroupApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureGroupAppl,
                ProductFeatureGroupApplDao.class);
        ProductFeatureCatGrpApplDao productFeatureCatGrpApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureCatGrpAppl,
                ProductFeatureCatGrpApplDao.class);
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // do sub-categories first so all feature groups will be in place
        List<GenericValue> subCategoryList = productCategoryRollupDao.findListByWhere(delegator, x.ProductCategoryRollup,
                UtilMisc.toMap(x.parentProductCategoryId, productCategoryId), null, null, false);
        if (doSubCategories) {
            for (GenericValue productCategoryRollup : subCategoryList) {
                attachProductFeaturesToCategory(productCategoryRollup.getString(x.productCategoryId), productFeatureTypeIdsToInclude,
                        productFeatureTypeIdsToExclude, delegator, true, nowTimestamp);
            }
        }

        // now get all features for this category and make associated feature groups
        Map<String, Set<String>> productFeatureIdByTypeIdSetMap = new HashMap<>();
        List<GenericValue> productCategoryMemberList = productCategoryMemberDao.findListByWhere(delegator, x.ProductCategoryMember,
                UtilMisc.toMap(x.productCategoryId, productCategoryId), null, null, false);
        for (GenericValue productCategoryMember : productCategoryMemberList) {
            String productId = productCategoryMember.getString(x.productId);
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId),
                    EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN_EQUAL_TO,
                                    nowTimestamp))), EntityOperator.AND);

            try (EntityListIterator productFeatureAndApplEli = productFeatureApplDao.findIteratorByWhere(delegator,
                    x.ProductFeatureAndAppl, condition, null, null, null)) {
                GenericValue productFeatureAndAppl = null;
                while ((productFeatureAndAppl = productFeatureAndApplEli.next()) != null) {
                    String productFeatureId = productFeatureAndAppl.getString(x.productFeatureId);
                    String productFeatureTypeId = productFeatureAndAppl.getString(x.productFeatureTypeId);
                    if (UtilValidate.isNotEmpty(productFeatureTypeIdsToInclude) && !productFeatureTypeIdsToInclude.contains(productFeatureTypeId)) {
                        continue;
                    }
                    if (productFeatureTypeIdsToExclude != null && productFeatureTypeIdsToExclude.contains(productFeatureTypeId)) {
                        continue;
                    }
                    Set<String> productFeatureIdSet = productFeatureIdByTypeIdSetMap.get(productFeatureTypeId);
                    if (productFeatureIdSet == null) {
                        productFeatureIdSet = new HashSet<>();
                        productFeatureIdByTypeIdSetMap.put(productFeatureTypeId, productFeatureIdSet);
                    }
                    productFeatureIdSet.add(productFeatureId);
                }

                for (Map.Entry<String, Set<String>> entry : productFeatureIdByTypeIdSetMap.entrySet()) {
                    String productFeatureTypeId = entry.getKey();
                    Set<String> productFeatureIdSet = entry.getValue();

                    String productFeatureGroupId = productCategoryId + x.str_53a0acfa + productFeatureTypeId;
                    if (productFeatureGroupId.length() > 20) {
                        Debug.logWarning(x.Manufactured_productFeatureGroupId_was_greater_than_20_characters_means_that_we_had_some_long
                                + x.productCategoryId_and_or_productFeatureTypeId_values_at_the_category_part_should_be_unique_since_it_is_first
                                + x.so_if_the_feature_type_isn_t_unique_it_just_means_more_than_one_type_of_feature_will_go_into_the_category,
                                MODULE);
                        productFeatureGroupId = productFeatureGroupId.substring(0, 20);
                    }

                    GenericValue productFeatureGroup = productFeatureGroupDao.findOneByWhere(delegator, x.ProductFeatureGroup,
                            UtilMisc.toMap(x.productFeatureGroupId, productFeatureGroupId), null, null, false);
                    if (productFeatureGroup == null) {
                        // auto-create the group
                        String description = x.Feature_Group_for_type + productFeatureTypeId + x.features_in_category + productCategoryId + x.str_4ff447b8;
                        productFeatureGroup = delegator.makeValue(x.ProductFeatureGroup, UtilMisc.toMap(x.productFeatureGroupId,
                                productFeatureGroupId, x.description, description));
                        productFeatureGroup.create();

                        GenericValue productFeatureCatGrpAppl = delegator.makeValue(x.ProductFeatureCatGrpAppl,
                                UtilMisc.toMap(x.productFeatureGroupId, productFeatureGroupId, x.productCategoryId, productCategoryId,
                                        x.fromDate, nowTimestamp));
                        productFeatureCatGrpAppl.create();
                    }

                    // now put all of the features in the group, if there is not already a valid feature placement there...
                    for (String productFeatureId : productFeatureIdSet) {
                        condition = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition(x.productFeatureId, EntityOperator.EQUALS, productFeatureId),
                                EntityCondition.makeCondition(x.productFeatureGroupId, EntityOperator.EQUALS, productFeatureGroupId),
                                EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                                EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null),
                                        EntityOperator.OR, EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN_EQUAL_TO,
                                                nowTimestamp))), EntityOperator.AND);
                        if (productFeatureGroupApplDao.countByWhere(delegator, x.ProductFeatureGroupAppl, condition, null, null) == 0) {
                            // if no valid ones, create one
                            GenericValue productFeatureGroupAppl = delegator.makeValue(x.ProductFeatureGroupAppl,
                                    UtilMisc.toMap(x.productFeatureGroupId, productFeatureGroupId, x.productFeatureId, productFeatureId,
                                            x.fromDate, nowTimestamp));
                            productFeatureGroupAppl.create();
                        }
                    }
                }

                // now get all feature groups associated with sub-categories and associate them with this category
                for (GenericValue productCategoryRollup : subCategoryList) {
                    String subProductCategoryId = productCategoryRollup.getString(x.productCategoryId);
                    condition = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS, subProductCategoryId),
                            EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                            EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null), EntityOperator.OR,
                                    EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))),
                            EntityOperator.AND);
                    try (EntityListIterator productFeatureCatGrpApplEli = productFeatureCatGrpApplDao.findIteratorByWhere(delegator,
                            x.ProductFeatureCatGrpAppl, condition, null, null, null)) {
                        GenericValue productFeatureCatGrpAppl = null;
                        while ((productFeatureCatGrpAppl = productFeatureCatGrpApplEli.next()) != null) {
                            String productFeatureGroupId = productFeatureCatGrpAppl.getString(x.productFeatureGroupId);
                            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                                    EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS, productCategoryId),
                                    EntityCondition.makeCondition(x.productFeatureGroupId, EntityOperator.EQUALS, productFeatureGroupId),
                                    EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                                    EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null),
                                            EntityOperator.OR, EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN_EQUAL_TO,
                                                    nowTimestamp))), EntityOperator.AND);
                            if (productFeatureCatGrpApplDao.countByWhere(delegator, x.ProductFeatureCatGrpAppl, checkCondition, null, null)
                                    == 0) {
                                // if no valid ones, create one
                                GenericValue productFeatureGroupAppl = delegator.makeValue(x.ProductFeatureCatGrpAppl,
                                        UtilMisc.toMap(x.productFeatureGroupId, productFeatureGroupId, x.productCategoryId, productCategoryId,
                                                x.fromDate, nowTimestamp));
                                productFeatureGroupAppl.create();
                            }
                        }
                    }
                }
            }
        }
    }
}
