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
package org.apache.ofbiz.product.category;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProdCatalogCategoryDao;
import org.apache.ofbiz.persistence.dao.ProdCatalogDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryMemberDao;
import org.apache.ofbiz.persistence.dao.ProductCategoryRollupDao;
import org.apache.ofbiz.persistence.dao.ProductStoreDao;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.CategoryServicesContext;
/**
 * CategoryServices - Category Services
 */
public class CategoryServices {

    private static final String MODULE = CategoryServices.class.getName();
    private static final String RES_ERROR = x.ProductErrorUiLabels;

    public static Map<String, Object> getCategoryMembers(DispatchContext dctx, CategoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductCategoryDao productCategoryDao = DaoRegistry.getDao(delegator, x.ProductCategory, ProductCategoryDao.class);
        String categoryId = (String) context.get(x.categoryId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue productCategory = null;
        List<GenericValue> members = null;

        try {
            Object productCategoryEntity = productCategoryDao.get(categoryId).orElse(null);
            productCategory = productCategoryEntity == null ? null
                    : delegator.makeValue(x.ProductCategory, Beans.beanToMap(productCategoryEntity));
            members = EntityUtil.filterByDate(productCategory.getRelated(x.ProductCategoryMember, null,
                    UtilMisc.toList(x.sequenceNum), true), true);
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Category + productCategory + x.Member_Size + members.size() + x.Members + members, MODULE);
            }
        } catch (GenericEntityException | SQLException e) {
            Debug.logError(e, x.Problem_reading_product_categories + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.categoryservices_problems_reading_category_entity,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.category, productCategory);
        result.put(x.categoryMembers, members);
        return result;
    }

    public static Map<String, Object> getPreviousNextProducts(DispatchContext dctx, CategoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductCategoryDao productCategoryDao = DaoRegistry.getDao(delegator, x.ProductCategory, ProductCategoryDao.class);
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember,
                ProductCategoryMemberDao.class);
        String categoryId = (String) context.get(x.categoryId);
        String productId = (String) context.get(x.productId);
        boolean activeOnly = (context.get(x.activeOnly) != null ? (Boolean) context.get(x.activeOnly) : true);
        Integer index = (Integer) context.get(x.index);
        Timestamp introductionDateLimit = (Timestamp) context.get(x.introductionDateLimit);
        Timestamp releaseDateLimit = (Timestamp) context.get(x.releaseDateLimit);
        Locale locale = (Locale) context.get(x.locale);

        if (index == null && productId == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.categoryservices_problems_getting_next_products, locale));
        }

        List<String> orderByFields = UtilGenerics.cast(context.get(x.orderByFields));
        if (orderByFields == null) orderByFields = new LinkedList<>();
        String entityName = getCategoryFindEntityName(delegator, orderByFields, introductionDateLimit, releaseDateLimit);

        GenericValue productCategory;
        List<GenericValue> productCategoryMembers;
        try {
            Object productCategoryEntity = productCategoryDao.get(categoryId).orElse(null);
            productCategory = productCategoryEntity == null ? null
                    : delegator.makeValue(x.ProductCategory, Beans.beanToMap(productCategoryEntity));
            productCategoryMembers = productCategoryMemberDao.findListByWhere(delegator, entityName,
                    UtilMisc.toMap(x.productCategoryId, categoryId), null, orderByFields, true);
        } catch (GenericEntityException | SQLException e) {
            Debug.logInfo(e, x.Error_finding_previous_next_product_info + e.toString(), MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.categoryservices_error_find_next_products,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (activeOnly) {
            productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
        }
        List<EntityCondition> filterConditions = new LinkedList<>();
        if (introductionDateLimit != null) {
            EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition(x.introductionDate,
                    EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.introductionDate,
                    EntityOperator.LESS_THAN_EQUAL_TO, introductionDateLimit));
            filterConditions.add(condition);
        }
        if (releaseDateLimit != null) {
            EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition(x.releaseDate, EntityOperator.EQUALS, null),
                    EntityOperator.OR, EntityCondition.makeCondition(x.releaseDate, EntityOperator.LESS_THAN_EQUAL_TO, releaseDateLimit));
            filterConditions.add(condition);
        }
        if (!filterConditions.isEmpty()) {
            productCategoryMembers = EntityUtil.filterByCondition(productCategoryMembers, EntityCondition.makeCondition(filterConditions,
                    EntityOperator.AND));
        }

        if (productId != null && index == null) {
            for (GenericValue v: productCategoryMembers) {
                if (v.getString(x.productId).equals(productId)) {
                    index = productCategoryMembers.indexOf(v);
                }
            }
        }

        if (index == null) {
            // this is not going to be an error condition because we don't want it to be so critical, ie rolling back the transaction and such
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.categoryservices_product_not_found, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.category, productCategory);

        String previous = null;
        String next = null;

        if (index - 1 >= 0 && index - 1 < productCategoryMembers.size()) {
            previous = productCategoryMembers.get(index - 1).getString(x.productId);
            result.put(x.previousProductId, previous);
        } else {
            previous = productCategoryMembers.get(productCategoryMembers.size() - 1).getString(x.productId);
            result.put(x.previousProductId, previous);
        }

        if (index + 1 < productCategoryMembers.size()) {
            next = productCategoryMembers.get(index + 1).getString(x.productId);
            result.put(x.nextProductId, next);
        } else {
            next = productCategoryMembers.get(0).getString(x.productId);
            result.put(x.nextProductId, next);
        }
        return result;
    }

    private static String getCategoryFindEntityName(Delegator delegator, List<String> orderByFields, Timestamp introductionDateLimit,
                                                    Timestamp releaseDateLimit) {
        // allow orderByFields to contain fields from the Product entity, if there are such fields
        String entityName = introductionDateLimit == null && releaseDateLimit == null ? x.ProductCategoryMember : x.ProductAndCategoryMember;
        if (orderByFields == null) {
            return entityName;
        }
        if (orderByFields.isEmpty()) {
            orderByFields.add(x.sequenceNum);
            orderByFields.add(x.productId);
        }

        ModelEntity productModel = delegator.getModelEntity(x.Product);
        ModelEntity productCategoryMemberModel = delegator.getModelEntity(x.ProductCategoryMember);
        for (String orderByField: orderByFields) {
            // Get the real field name from the order by field removing ascending/descending order
            if (UtilValidate.isNotEmpty(orderByField)) {
                int startPos = 0;
                int endPos = orderByField.length();

                if (orderByField.endsWith(x.DESC_296a4488)) {
                    endPos -= 5;
                } else if (orderByField.endsWith(x.ASC)) {
                    endPos -= 4;
                } else if (orderByField.startsWith(x.str_3bc15c8a)) {
                    startPos++;
                } else if (orderByField.startsWith(x.str_a979ef10)) {
                    startPos++;
                }

                if (startPos != 0 || endPos != orderByField.length()) {
                    orderByField = orderByField.substring(startPos, endPos);
                }
            }

            if (!productCategoryMemberModel.isField(orderByField)) {
                if (productModel.isField(orderByField)) {
                    entityName = x.ProductAndCategoryMember;
                    // that's what we wanted to find out, so we can quit now
                    break;
                //} else {
                    // ahh!! bad field name, don't worry, it will blow up in the query
                }
            }
        }
        return entityName;
    }

    public static Map<String, Object> getProductCategoryAndLimitedMembers(DispatchContext dctx, CategoryServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        ProductCategoryDao productCategoryDao = DaoRegistry.getDao(delegator, x.ProductCategory, ProductCategoryDao.class);
        ProductStoreDao productStoreDao = DaoRegistry.getDao(delegator, x.ProductStore, ProductStoreDao.class);
        ProductCategoryMemberDao productCategoryMemberDao = DaoRegistry.getDao(delegator, x.ProductCategoryMember,
                ProductCategoryMemberDao.class);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productCategoryId = (String) context.get(x.productCategoryId);
        boolean limitView = (Boolean) context.get(x.limitView);
        int defaultViewSize = (Integer) context.get(x.defaultViewSize);
        Timestamp introductionDateLimit = (Timestamp) context.get(x.introductionDateLimit);
        Timestamp releaseDateLimit = (Timestamp) context.get(x.releaseDateLimit);

        List<String> orderByFields = UtilGenerics.cast(context.get(x.orderByFields));
        if (orderByFields == null) orderByFields = new LinkedList<>();
        String entityName = getCategoryFindEntityName(delegator, orderByFields, introductionDateLimit, releaseDateLimit);

        String prodCatalogId = (String) context.get(x.prodCatalogId);

        boolean useCacheForMembers = (context.get(x.useCacheForMembers) == null || (Boolean) context.get(x.useCacheForMembers));
        boolean activeOnly = (context.get(x.activeOnly) == null || (Boolean) context.get(x.activeOnly));

        // checkViewAllow defaults to false, must be set to true and pass the prodCatalogId to enable
        boolean checkViewAllow = (prodCatalogId != null && context.get(x.checkViewAllow) != null
                && (Boolean) context.get(x.checkViewAllow));

        String viewProductCategoryId = null;
        if (checkViewAllow) {
            viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
        }

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        int viewIndex = 0;
        if (context.containsKey(x.viewIndexString)) {
            try {
                viewIndex = Integer.parseInt((String) context.get(x.viewIndexString));
            } catch (Exception e) {
                viewIndex = 0;
            }
        }

        int viewSize = defaultViewSize;
        if (context.containsKey(x.viewSizeString)) {
            try {
                viewSize = Integer.parseInt((String) context.get(x.viewSizeString));
            } catch (NumberFormatException e) {
                Debug.logWarning(x.Fail_to_parse_viewSizeString
                        + context.get(x.viewSizeString)
                        + x.str_b858cb28 + e.getMessage(), MODULE);
            }
        }

        GenericValue productCategory;
        try {
            Object productCategoryEntity = productCategoryDao.get(productCategoryId).orElse(null);
            productCategory = productCategoryEntity == null ? null
                    : delegator.makeValue(x.ProductCategory, Beans.beanToMap(productCategoryEntity));
        } catch (SQLException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            productCategory = null;
        }

        int listSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        if (limitView) {
            // get the indexes for the partial list
            lowIndex = ((viewIndex * viewSize) + 1);
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            lowIndex = 0;
            highIndex = 0;
        }
        boolean filterOutOfStock = false;
        try {
            String productStoreId = (String) context.get(x.productStoreId);
            if (UtilValidate.isNotEmpty(productStoreId)) {
                GenericValue productStore = productStoreDao.findOneByWhere(delegator, x.ProductStore,
                        UtilMisc.toMap(x.productStoreId, productStoreId), null, null, false);
                if (productStore != null && x.N.equals(productStore.getString(x.showOutOfStockProducts))) {
                    filterOutOfStock = true;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }

        List<GenericValue> productCategoryMembers = null;
        if (productCategory != null) {
            try {
                if (useCacheForMembers) {
                    productCategoryMembers = productCategoryMemberDao.findListByWhere(delegator, entityName,
                            UtilMisc.toMap(x.productCategoryId, productCategoryId), null, orderByFields, true);
                    if (activeOnly) {
                        productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    }
                    List<EntityCondition> filterConditions = new LinkedList<>();
                    if (introductionDateLimit != null) {
                        EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition(x.introductionDate,
                                EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.introductionDate,
                                EntityOperator.LESS_THAN_EQUAL_TO, introductionDateLimit));
                        filterConditions.add(condition);
                    }
                    if (releaseDateLimit != null) {
                        EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition(x.releaseDate,
                                EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(x.releaseDate,
                                EntityOperator.LESS_THAN_EQUAL_TO, releaseDateLimit));
                        filterConditions.add(condition);
                    }
                    if (!filterConditions.isEmpty()) {
                        productCategoryMembers = EntityUtil.filterByCondition(productCategoryMembers, EntityCondition.makeCondition(filterConditions,
                                EntityOperator.AND));
                    }
                    // filter out of stock products
                    if (filterOutOfStock) {
                        try {
                            productCategoryMembers = ProductWorker.filterOutOfStockProducts(productCategoryMembers, dispatcher, delegator);
                        } catch (GeneralException e) {
                            Debug.logWarning(x.Problem_filtering_out_of_stock_products + e.getMessage(), MODULE);
                        }
                    }
                    // filter out the view allow before getting the sublist
                    if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
                        productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                    }

                    // set the index and size
                    listSize = productCategoryMembers.size();
                    if (limitView) {
                        // limit high index to (filtered) listSize
                        if (highIndex > listSize) {
                            highIndex = listSize;
                        }
                        // if lowIndex > listSize, the input is wrong => reset to first page
                        if (lowIndex > listSize) {
                            viewIndex = 0;
                            lowIndex = 1;
                            highIndex = Math.min(viewSize, highIndex);
                        }
                        // get only between low and high indexes
                        if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                            productCategoryMembers = productCategoryMembers.subList(lowIndex - 1, highIndex);
                        }
                    } else {
                        lowIndex = 1;
                        highIndex = listSize;
                    }
                } else {
                    List<EntityCondition> mainCondList = new LinkedList<>();
                    mainCondList.add(EntityCondition.makeCondition(x.productCategoryId, EntityOperator.EQUALS,
                            productCategory.getString(x.productCategoryId)));
                    if (activeOnly) {
                        mainCondList.add(EntityCondition.makeCondition(x.fromDate, EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null),
                                EntityOperator.OR, EntityCondition.makeCondition(x.thruDate, EntityOperator.GREATER_THAN, nowTimestamp)));
                    }
                    if (introductionDateLimit != null) {
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.introductionDate, EntityOperator.EQUALS,
                                null), EntityOperator.OR, EntityCondition.makeCondition(x.introductionDate, EntityOperator.LESS_THAN_EQUAL_TO,
                                introductionDateLimit)));
                    }
                    if (releaseDateLimit != null) {
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.releaseDate, EntityOperator.EQUALS, null),
                                EntityOperator.OR, EntityCondition.makeCondition(x.releaseDate, EntityOperator.LESS_THAN_EQUAL_TO,
                                        releaseDateLimit)));
                    }
                    EntityCondition mainCond = EntityCondition.makeCondition(mainCondList, EntityOperator.AND);

                    // set distinct on using list iterator
                    EntityFindOptions findOptions = new EntityFindOptions();
                    findOptions.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
                    findOptions.setMaxRows(highIndex);
                    try (EntityListIterator pli = productCategoryMemberDao.findIteratorByWhere(delegator, entityName, mainCond,
                            null, orderByFields, findOptions)) {
                        // get the partial list for this page
                        if (limitView) {
                            if (viewProductCategoryId != null) {
                                // do manual checking to filter view allow
                                productCategoryMembers = new LinkedList<>();
                                GenericValue nextValue;
                                int chunkSize = 0;
                                listSize = 0;

                                while ((nextValue = pli.next()) != null) {
                                    String productId = nextValue.getString(x.productId);
                                    if (CategoryWorker.isProductInCategory(delegator, productId, viewProductCategoryId)) {
                                        if (listSize + 1 >= lowIndex && chunkSize < viewSize) {
                                            productCategoryMembers.add(nextValue);
                                            chunkSize++;
                                        }
                                        listSize++;
                                    }
                                }
                            } else {
                                productCategoryMembers = pli.getPartialList(lowIndex, viewSize);
                                listSize = pli.getResultsSizeAfterPartialList();
                            }
                        } else {
                            productCategoryMembers = pli.getCompleteList();
                            if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
                                // filter out the view allow
                                productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers,
                                        viewProductCategoryId);
                            }
                            listSize = productCategoryMembers.size();
                            lowIndex = 1;
                            highIndex = listSize;
                        }
                    }
                    // filter out of stock products
                    if (filterOutOfStock) {
                        try {
                            productCategoryMembers = ProductWorker.filterOutOfStockProducts(productCategoryMembers, dispatcher, delegator);
                            listSize = productCategoryMembers.size();
                        } catch (GeneralException e) {
                            Debug.logWarning(x.Problem_filtering_out_of_stock_products + e.getMessage(), MODULE);
                        }
                    }

                    // null safety
                    if (productCategoryMembers == null) {
                        productCategoryMembers = new LinkedList<>();
                    }

                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put(x.viewIndex, viewIndex);
        result.put(x.viewSize, viewSize);
        result.put(x.lowIndex, lowIndex);
        result.put(x.highIndex, highIndex);
        result.put(x.listSize, listSize);
        if (productCategory != null) result.put(x.productCategory, productCategory);
        if (productCategoryMembers != null) result.put(x.productCategoryMembers, productCategoryMembers);
        return result;
    }

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    @SuppressWarnings(x.unchecked)
    public static String getChildCategoryTree(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute(x.delegator);
        ProdCatalogDao prodCatalogDao = DaoRegistry.getDao(delegator, x.ProdCatalog, ProdCatalogDao.class);
        ProductCategoryDao productCategoryDao = DaoRegistry.getDao(delegator, x.ProductCategory, ProductCategoryDao.class);
        ProductCategoryRollupDao productCategoryRollupDao = DaoRegistry.getDao(delegator, x.ProductCategoryRollup,
                ProductCategoryRollupDao.class);
        ProdCatalogCategoryDao prodCatalogCategoryDao = DaoRegistry.getDao(delegator, x.ProdCatalogCategory,
                ProdCatalogCategoryDao.class);
        String productCategoryId = request.getParameter(x.productCategoryId);
        String isCatalog = request.getParameter(x.isCatalog);
        String isCategoryType = request.getParameter(x.isCategoryType);
        String onclickFunction = request.getParameter(x.onclickFunction);
        String additionParam = request.getParameter(x.additionParam);
        String hrefString = request.getParameter(x.hrefString);
        String hrefString2 = request.getParameter(x.hrefString2);
        String entityName = null;
        String primaryKeyName = null;

        if (x._true.equals(isCatalog)) {
            entityName = x.ProdCatalog;
            primaryKeyName = x.prodCatalogId;
        } else {
            entityName = x.ProductCategory;
            primaryKeyName = x.productCategoryId;
        }

        List<Map<Object, Object>> categoryList = new LinkedList<>();
        List<GenericValue> childOfCats;
        List<String> sortList = org.apache.ofbiz.base.util.UtilMisc.toList(x.sequenceNum, x.title);

        try {
            GenericValue category = null;
            if (x.ProdCatalog.equals(entityName)) {
                Object prodCatalogEntity = prodCatalogDao.get(productCategoryId).orElse(null);
                if (prodCatalogEntity != null) {
                    category = delegator.makeValue(x.ProdCatalog, Beans.beanToMap(prodCatalogEntity));
                }
            } else {
                Object productCategoryEntity = productCategoryDao.get(productCategoryId).orElse(null);
                if (productCategoryEntity != null) {
                    category = delegator.makeValue(x.ProductCategory, Beans.beanToMap(productCategoryEntity));
                }
            }
            if (category != null) {
                if (x._true.equals(isCatalog) && x._false.equals(isCategoryType)) {
                    CategoryWorker.getRelatedCategories(request, x.ChildCatalogList, CatalogWorker.getCatalogTopCategoryId(request,
                            productCategoryId), true);
                    childOfCats = EntityUtil.filterByDate((List<GenericValue>) request.getAttribute(x.ChildCatalogList));

                } else if (x._false.equals(isCatalog) && x._false.equals(isCategoryType)) {
                    childOfCats = productCategoryRollupDao.findListByWhere(delegator, x.ProductCategoryRollupAndChild,
                            UtilMisc.toMap(x.parentProductCategoryId, productCategoryId), null, null, false, true);
                } else {
                    List<GenericValue> prodCatalogCategories = new LinkedList<>();
                    for (Object prodCatalogCategoryEntity : prodCatalogCategoryDao.list(Filters.eq(x.prodCatalogId, productCategoryId))) {
                        prodCatalogCategories.add(delegator.makeValue(x.ProdCatalogCategory, Beans.beanToMap(prodCatalogCategoryEntity)));
                    }
                    childOfCats = EntityUtil.filterByDate(prodCatalogCategories, true);
                }
                if (UtilValidate.isNotEmpty(childOfCats)) {
                    for (GenericValue childOfCat : childOfCats) {
                        Object catId = null;
                        String catNameField = null;

                        catId = childOfCat.get(x.productCategoryId);
                        catNameField = x.CATEGORY_NAME;

                        Map<Object, Object> josonMap = new HashMap<>();
                        List<GenericValue> childList = null;

                        // Get the child list of chosen category
                        childList = productCategoryRollupDao.findListByWhere(delegator, x.ProductCategoryRollup,
                                UtilMisc.toMap(x.parentProductCategoryId, catId), null, null, false, true);

                        // Get the chosen category information for the categoryContentWrapper
                        Object cateEntity = productCategoryDao.get((String) catId).orElse(null);
                        GenericValue cate = cateEntity == null ? null : delegator.makeValue(x.ProductCategory, Beans.beanToMap(cateEntity));

                        // If chosen category's child exists, then put the arrow before category icon
                        if (UtilValidate.isNotEmpty(childList)) {
                            josonMap.put(x.state, x.closed);
                        }
                        Map<String, Object> dataMap = new HashMap<>();
                        Map<String, String> dataAttrMap = new HashMap<>();
                        CategoryContentWrapper categoryContentWrapper = new CategoryContentWrapper(cate, request);
                        String title = null;
                        if (UtilValidate.isNotEmpty(categoryContentWrapper.get(catNameField, x.html))) {
                            title = new StringBuffer(categoryContentWrapper.get(catNameField, x.html).toString())
                                    .append(x.str_42cbdb3c).append(catId).append(x.str_4ff447b8).toString();
                            dataMap.put(x.title, title);
                        } else {
                            title = catId.toString();
                            dataMap.put(x.title, catId);
                        }
                        dataAttrMap.put(x.onClick, new StringBuffer(onclickFunction).append(x.str_c71c42f1).append(catId).append(additionParam)
                                .append(x.str_10ae764a).toString());

                        String hrefStr = hrefString + catId;
                        if (UtilValidate.isNotEmpty(hrefString2)) {
                            hrefStr = hrefStr + hrefString2;
                        }
                        dataAttrMap.put(x.href, hrefStr);
                        dataMap.put(x.attr, dataAttrMap);
                        josonMap.put(x.data, dataMap);
                        Map<String, Object> attrMap = new HashMap<>();
                        attrMap.put(x.id, catId);
                        attrMap.put(x.isCatalog, false);
                        attrMap.put(x.rel, x.CATEGORY);
                        josonMap.put(x.attr, attrMap);
                        josonMap.put(x.sequenceNum, childOfCat.get(x.sequenceNum));
                        josonMap.put(x.title, title);
                        categoryList.add(josonMap);
                    }
                    List<Map<Object, Object>> sortedCategoryList = UtilMisc.sortMaps(categoryList, sortList);
                    request.setAttribute(x.treeData, sortedCategoryList);
                }
            }
        } catch (GenericEntityException | SQLException e) {
            Debug.logWarning(e, MODULE);
            return x.error;
        }
        return x.success;
    }
}
