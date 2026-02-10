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

package org.apache.ofbiz.manufacturing.bom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.OrderShipmentDao;
import org.apache.ofbiz.persistence.dao.ProductAssocDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.ShipmentPackageDao;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.BOMServicesContext;
/** Bills of Materials' services implementation.
 * These services are useful when dealing with product's
 * bills of materials.
 */
public class BOMServices {

    private static final String MODULE = BOMServices.class.getName();
    private static final String RESOURCE = x.ManufacturingUiLabels;

    /** Returns the product's low level code (llc) i.e. the maximum depth
     * in which the productId can be found in any of the
     * bills of materials of bomType type.
     * If the bomType input field is not passed then the depth is searched for all the bom types and the lowest depth is returned.
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the product's low level code (llc) i.e. the maximum depth
     */
    public static Map<String, Object> getMaxDepth(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get(x.productId);
        String fromDateStr = (String) context.get(x.fromDate);
        String bomType = (String) context.get(x.bomType);
        Locale locale = (Locale) context.get(x.locale);

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }
        List<String> bomTypes = new LinkedList<>();
        if (bomType == null) {
            try {
                UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.ProductAssocType, UserLoginDao.class);
                List<GenericValue> bomTypesValues = userLoginDao.findByAnd(delegator, x.ProductAssocType,
                        UtilMisc.toMap(x.parentTypeId, x.PRODUCT_COMPONENT), null, false);
                for (GenericValue bomTypesValue : bomTypesValues) {
                    bomTypes.add(bomTypesValue.getString(x.productAssocTypeId));
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorRunningMaxDethAlgorithm,
                        UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
            }
        } else {
            bomTypes.add(bomType);
        }

        int depth = 0;
        int maxDepth = 0;
        try {
            for (String oneBomType : bomTypes) {
                depth = BOMHelper.getMaxDepth(productId, oneBomType, fromDate, delegator);
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorRunningMaxDethAlgorithm,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        result.put(x.depth, (long) maxDepth);

        return result;
    }

    /** Updates the product's low level code (llc)
     * Given a product id, computes and updates the product's low level code (field billOfMaterialLevel in Product entity).
     * It also updates the llc of all the product's descendants.
     * For the llc only the manufacturing bom ("MANUF_COMPONENT") is considered.
     * @param dctx the distach context
     * @param context the context
     * @return the results of the updates the product's low level code
    */
    public static Map<String, Object> updateLowLevelCode(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get(x.productIdTo);
        Boolean alsoComponents = (Boolean) context.get(x.alsoComponents);
        Locale locale = (Locale) context.get(x.locale);
        if (alsoComponents == null) {
            alsoComponents = Boolean.TRUE;
        }
        Boolean alsoVariants = (Boolean) context.get(x.alsoVariants);
        if (alsoVariants == null) {
            alsoVariants = Boolean.TRUE;
        }

        Long llc = null;
        try {
            ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
            ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
            GenericValue product = productDao.findOneByWhere(delegator, x.Product, UtilMisc.toMap(x.productId, productId), null, null, false);
            Map<String, Object> depthResult = dispatcher.runSync(x.getMaxDepth,
                    UtilMisc.toMap(x.productId, productId, x.bomType, x.MANUF_COMPONENT));
            if (ServiceUtil.isError(depthResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depthResult));
            }
            llc = (Long) depthResult.get(x.depth);
            // If the product is a variant of a virtual, then the billOfMaterialLevel cannot be
            // lower than the billOfMaterialLevel of the virtual product.
            List<GenericValue> virtualProducts = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                    UtilMisc.toMap(x.productIdTo, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);
            int virtualMaxDepth = 0;
            for (GenericValue oneVirtualProductAssoc : virtualProducts) {
                int virtualDepth = 0;
                GenericValue virtualProduct = productDao.findOneByWhere(delegator, x.Product,
                        UtilMisc.toMap(x.productId, oneVirtualProductAssoc.getString(x.productId)), null, null, false);
                if (virtualProduct.get(x.billOfMaterialLevel) != null) {
                    virtualDepth = virtualProduct.getLong(x.billOfMaterialLevel).intValue();
                } else {
                    virtualDepth = 0;
                }
                if (virtualDepth > virtualMaxDepth) {
                    virtualMaxDepth = virtualDepth;
                }
            }
            if (virtualMaxDepth > llc.intValue()) {
                llc = (long) virtualMaxDepth;
            }
            product.set(x.billOfMaterialLevel, llc);
            product.store();
            if (alsoComponents) {
                Map<String, Object> treeResult = dispatcher.runSync(x.getBOMTree, UtilMisc.toMap(x.productId, productId,
                        x.bomType, x.MANUF_COMPONENT));
                if (ServiceUtil.isError(treeResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(treeResult));
                }
                BOMTree tree = (BOMTree) treeResult.get(x.tree);
                List<BOMNode> products = new LinkedList<>();
                tree.print(products, llc.intValue());
                for (BOMNode oneNode : products) {
                    GenericValue oneProduct = oneNode.getProduct();
                    int lev = 0;
                    if (oneProduct.get(x.billOfMaterialLevel) != null) {
                        lev = oneProduct.getLong(x.billOfMaterialLevel).intValue();
                    }
                    if (lev < oneNode.getDepth()) {
                        oneProduct.set(x.billOfMaterialLevel, (long) oneNode.getDepth());
                        oneProduct.store();
                    }
                }
            }
            if (alsoVariants) {
                List<GenericValue> variantProducts = productAssocDao.findListByWhere(delegator, x.ProductAssoc,
                        UtilMisc.toMap(x.productId, productId, x.productAssocTypeId, x.PRODUCT_VARIANT), null, null, false, true);
                for (GenericValue oneVariantProductAssoc : variantProducts) {
                    GenericValue variantProduct = productDao.findOneByWhere(delegator, x.Product,
                            UtilMisc.toMap(x.productId, oneVariantProductAssoc.getString(x.productId)), null, null, false);
                    variantProduct.set(x.billOfMaterialLevel, llc);
                    variantProduct.store();
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorRunningUpdateLowLevelCode,
                    UtilMisc.toMap(x.errorString, ge.getMessage()), locale));
        }
        result.put(x.lowLevelCode, llc);
        return result;
    }

    /** Updates the product's low level code (llc) for all the products in the Product entity.
     * For the llc only the manufacturing bom ("MANUF_COMPONENT") is considered.
     * @param dctx the distach context
     * @param context the context
     * @return the results of the updates the product's low level code
    */
    public static Map<String, Object> initLowLevelCode(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        try {
            ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
            List<GenericValue> products = productDao.findListByWhere(delegator, x.Product, null, null,
                    UtilMisc.toList(x.isVirtual_DESC), false);
            Long zero = 0L;
            List<GenericValue> allProducts = new LinkedList<>();
            for (GenericValue product : products) {
                product.set(x.billOfMaterialLevel, zero);
                allProducts.add(product);
            }
            delegator.storeAll(allProducts);
            Debug.logInfo(x.Low_Level_Code_set_to_0_for_all_the_products, MODULE);

            for (GenericValue product : products) {
                try {
                    Map<String, Object> depthResult = dispatcher.runSync(x.updateLowLevelCode, UtilMisc.<String, Object>toMap(x.productIdTo,
                            product.getString(x.productId), x.alsoComponents, Boolean.FALSE, x.alsoVariants, Boolean.FALSE));
                    if (ServiceUtil.isError(depthResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depthResult));
                    }
                    Debug.logInfo(x.Product_4aaa1f6d + product.getString(x.productId) + x.Low_Level_Code + depthResult.get(x.lowLevelCode)
                            + x.str_4ff447b8, MODULE);
                } catch (GenericServiceException exc) {
                    Debug.logWarning(exc.getMessage(), MODULE);
                }
            }
            // FIXME: also all the variants llc should be updated?
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ManufacturingBomErrorRunningInitLowLevelCode, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        return result;
    }

    /** Returns the ProductAssoc generic value for a duplicate productIdKey
     * ancestor if present, null otherwise.
     * Useful to avoid loops when adding new assocs (components)
     * to a bill of materials.
     * @param dctx the distach context
     * @param context the context
     * @return returns the ProductAssoc generic value for a duplicate productIdKey ancestor if present
     */
    public static Map<String, Object> searchDuplicatedAncestor(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String productId = (String) context.get(x.productId);
        String productIdKey = (String) context.get(x.productIdTo);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        String bomType = (String) context.get(x.productAssocTypeId);
        if (fromDate == null) {
            fromDate = Timestamp.valueOf((new Date()).toString());
        }
        GenericValue duplicatedProductAssoc = null;
        try {
            duplicatedProductAssoc = BOMHelper.searchDuplicatedAncestor(productId, productIdKey, bomType, fromDate, delegator, dispatcher, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorRunningDuplicatedAncestorSearch,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        result.put(x.duplicatedProductAssoc, duplicatedProductAssoc);
        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns
     * an object (see {@link BOMTree}
     * and {@link BOMNode}) that represents a
     * configured bill of material tree.
     * Useful for tree traversal (breakdown, explosion, implosion).
     * @param dctx the distach context
     * @param context the context
     * @return return the bill of material tree
     */
    public static Map<String, Object> getBOMTree(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productId = (String) context.get(x.productId);
        String fromDateStr = (String) context.get(x.fromDate);
        String bomType = (String) context.get(x.bomType);
        Integer type = (Integer) context.get(x.type);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);
        if (type == null) {
            type = 0;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        BOMTree tree;
        try {
            tree = new BOMTree(productId, bomType, fromDate, type, delegator, dispatcher, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        if (quantity != null) {
            tree.setRootQuantity(quantity);
        }
        if (amount != null) {
            tree.setRootAmount(amount);
        }
        result.put(x.tree, tree);

        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns its (possibly configured) components in
     * a List of {@link BOMNode}).
     * @param dctx the distach context
     * @param context the context
     * @return return the list of manufacturing components
     */
    public static Map<String, Object> getManufacturingComponents(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        String fromDateStr = (String) context.get(x.fromDate);
        Boolean excludeWIPs = (Boolean) context.get(x.excludeWIPs);
        Locale locale = (Locale) context.get(x.locale);

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }
        if (excludeWIPs == null) {
            excludeWIPs = Boolean.TRUE;
        }

        //
        // Components
        //
        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        try {
            tree = new BOMTree(productId, x.MANUF_COMPONENT, fromDate, BOMTree.EXPLOSION_SINGLE_LEVEL, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(amount);
            tree.print(components, excludeWIPs);
            if (!components.isEmpty()) components.remove(0);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        //
        // Product routing
        //
        String workEffortId = null;
        try {
            Map<String, Object> routingInMap = UtilMisc.toMap(x.productId, productId, x.ignoreDefaultRouting, x.Y, x.userLogin, userLogin);
            Map<String, Object> routingOutMap = dispatcher.runSync(x.getProductRouting, routingInMap);
            if (ServiceUtil.isError(routingOutMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
            }
            GenericValue routing = (GenericValue) routingOutMap.get(x.routing);
            if (routing == null) {
                // try to find a routing linked to the virtual product
                routingInMap = UtilMisc.toMap(x.productId, tree.getRoot().getProduct().getString(x.productId), x.userLogin, userLogin);
                routingOutMap = dispatcher.runSync(x.getProductRouting, routingInMap);
                if (ServiceUtil.isError(routingOutMap)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
                }
                routing = (GenericValue) routingOutMap.get(x.routing);
            }
            if (routing != null) {
                workEffortId = routing.getString(x.workEffortId);
            }
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
        }
        if (workEffortId != null) {
            result.put(x.workEffortId, workEffortId);
        }
        result.put(x.components, components);

        // also return a componentMap (useful in scripts and simple language code)
        List<Map<String, Object>> componentsMap = new LinkedList<>();
        for (BOMNode node : components) {
            Map<String, Object> componentMap = new HashMap<>();
            componentMap.put(x.product, node.getProduct());
            componentMap.put(x.quantity, node.getQuantity());
            componentsMap.add(componentMap);
        }
        result.put(x.componentsMap, componentsMap);
        return result;
    }

    public static Map<String, Object> getNotAssembledComponents(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        BigDecimal amount = (BigDecimal) context.get(x.amount);
        String fromDateStr = (String) context.get(x.fromDate);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        List<BOMNode> notAssembledComponents = new LinkedList<>();
        try {
            tree = new BOMTree(productId, x.MANUF_COMPONENT, fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(amount);
            tree.print(components);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }
        for (BOMNode oneComponent : components) {
            if (!oneComponent.isManufactured()) {
                notAssembledComponents.add(oneComponent);
            }
        }
        result.put(x.notAssembledComponents, notAssembledComponents);
        return result;
    }

    // ---------------------------------------------
    // Service for the Product (Shipment) component
    //
    public static Map<String, Object> createShipmentPackages(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shipmentId = (String) context.get(x.shipmentId);
        ShipmentPackageDao shipmentPackageDao = DaoRegistry.getDao(delegator, x.ShipmentPackage, ShipmentPackageDao.class);
        OrderShipmentDao orderShipmentDao = DaoRegistry.getDao(delegator, x.OrderShipment, OrderShipmentDao.class);
        UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.ShipmentItem, UserLoginDao.class);

        try {
            List<GenericValue> packages = shipmentPackageDao.findByAnd(delegator, x.ShipmentPackage,
                    UtilMisc.toMap(x.shipmentId, shipmentId), null, false);
            if (UtilValidate.isNotEmpty(packages)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomPackageAlreadyFound, locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorLoadingShipmentPackages, locale));
        }
        // ShipmentItems are loaded
        List<GenericValue> shipmentItems = null;
        try {
            shipmentItems = userLoginDao.findByAnd(delegator, x.ShipmentItem, UtilMisc.toMap(x.shipmentId, shipmentId), null, false);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorLoadingShipmentItems, locale));
        }
        Map<String, Object> orderReadHelpers = new HashMap<>();
        Map<String, Object> partyOrderShipments = new HashMap<>();
        for (GenericValue shipmentItem : shipmentItems) {
            // Get the OrderShipments
            GenericValue orderShipment = null;
            try {
                orderShipment = orderShipmentDao.findFirstByWhere(delegator, x.OrderShipment,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentItemSeqId, shipmentItem.get(x.shipmentItemSeqId)),
                        null, null, false);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
            }
            if (orderShipment != null && !orderReadHelpers.containsKey(orderShipment.getString(x.orderId))) {
                orderReadHelpers.put(orderShipment.getString(x.orderId), new OrderReadHelper(delegator, orderShipment.getString(x.orderId)));
            }
            OrderReadHelper orderReadHelper = null;
            if (orderShipment != null) {
                orderReadHelper = (OrderReadHelper) orderReadHelpers.get(orderShipment.getString(x.orderId));
            }
            if (orderReadHelper != null) {
                Map<String, Object> orderShipmentReadMap = UtilMisc.toMap(x.orderShipment, orderShipment, x.orderReadHelper, orderReadHelper);
                String partyId = (orderReadHelper.getPlacingParty() != null ? orderReadHelper.getPlacingParty().getString(x.partyId) : null);
                // FIXME: is it the customer?
                if (partyId != null) {
                    if (!partyOrderShipments.containsKey(partyId)) {
                        List<Map<String, Object>> orderShipmentReadMapList = new LinkedList<>();
                        partyOrderShipments.put(partyId, orderShipmentReadMapList);
                    }
                    List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.cast(partyOrderShipments.get(partyId));
                    orderShipmentReadMapList.add(orderShipmentReadMap);
                }
            }
        }
        // For each party: try to expand the shipment item products
        // (search for components that needs to be packaged).
        for (Map.Entry<String, Object> partyOrderShipment : partyOrderShipments.entrySet()) {
            List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.cast(partyOrderShipment.getValue());
            for (Map<String, Object> stringObjectMap : orderShipmentReadMapList) {
                Map<String, Object> orderShipmentReadMap = UtilGenerics.cast(stringObjectMap);
                GenericValue orderShipment = (GenericValue) orderShipmentReadMap.get(x.orderShipment);
                OrderReadHelper orderReadHelper = (OrderReadHelper) orderShipmentReadMap.get(x.orderReadHelper);
                GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString(x.orderItemSeqId));
                // getProductsInPackages
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put(x.productId, orderItem.getString(x.productId));
                serviceContext.put(x.quantity, orderShipment.getBigDecimal(x.quantity));
                Map<String, Object> serviceResult = null;
                try {
                    serviceResult = dispatcher.runSync(x.getProductsInPackages, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                }
                List<BOMNode> productsInPackages = UtilGenerics.cast(serviceResult.get(x.productsInPackages));
                if (productsInPackages.size() == 1) {
                    BOMNode root = productsInPackages.get(0);
                    String rootProductId = (root.getSubstitutedNode() != null ? root.getSubstitutedNode().getProduct().getString(x.productId)
                            : root.getProduct().getString(x.productId));
                    if (orderItem.getString(x.productId).equals(rootProductId)) {
                        productsInPackages = null;
                    }
                }
                if (productsInPackages != null && productsInPackages.isEmpty()) {
                    productsInPackages = null;
                }
                if (UtilValidate.isNotEmpty(productsInPackages)) {
                    orderShipmentReadMap.put(x.productsInPackages, productsInPackages);
                }
            }
        }
        // Group together products and components
        // of the same box type.
        Map<String, GenericValue> boxTypes = new HashMap<>();
        for (Map.Entry<String, Object> partyOrderShipment : partyOrderShipments.entrySet()) {
            Map<String, List<Map<String, Object>>> boxTypeContent = new HashMap<>();
            List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.cast(partyOrderShipment.getValue());
            for (Map<String, Object> objectMap : orderShipmentReadMapList) {
                Map<String, Object> orderShipmentReadMap = UtilGenerics.cast(objectMap);
                GenericValue orderShipment = (GenericValue) orderShipmentReadMap.get(x.orderShipment);
                OrderReadHelper orderReadHelper = (OrderReadHelper) orderShipmentReadMap.get(x.orderReadHelper);
                List<BOMNode> productsInPackages = UtilGenerics.cast(orderShipmentReadMap.get(x.productsInPackages));
                if (productsInPackages != null) {
                    // there are subcomponents:
                    // this is a multi package shipment item
                    for (int j = 0; j < productsInPackages.size(); j++) {
                        BOMNode component = productsInPackages.get(j);
                        Map<String, Object> boxTypeContentMap = new HashMap<>();
                        boxTypeContentMap.put(x.content, orderShipmentReadMap);
                        boxTypeContentMap.put(x.componentIndex, j);
                        GenericValue product = component.getProduct();
                        String boxTypeId = product.getString(x.shipmentBoxTypeId);
                        if (boxTypeId != null) {
                            if (!boxTypes.containsKey(boxTypeId)) {
                                GenericValue boxType = null;
                                try {
                                    boxType = userLoginDao.findOne(delegator, x.ShipmentBoxType,
                                            UtilMisc.toMap(x.shipmentBoxTypeId, boxTypeId), false);
                                } catch (GenericEntityException e) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError,
                                            locale));
                                }
                                boxTypes.put(boxTypeId, boxType);
                                List<Map<String, Object>> box = new LinkedList<>();
                                boxTypeContent.put(boxTypeId, box);
                            }
                            List<Map<String, Object>> boxTypeContentList = UtilGenerics.cast(boxTypeContent.get(boxTypeId));
                            boxTypeContentList.add(boxTypeContentMap);
                        }
                    }
                } else {
                    // no subcomponents, the product has its own package:
                    // this is a single package shipment item
                    Map<String, Object> boxTypeContentMap = new HashMap<>();
                    boxTypeContentMap.put(x.content, orderShipmentReadMap);
                    GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString(x.orderItemSeqId));
                    GenericValue product = null;
                    try {
                        product = orderItem.getRelatedOne(x.Product, false);
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                    }
                    String boxTypeId = product.getString(x.shipmentBoxTypeId);
                    if (boxTypeId != null) {
                        if (!boxTypes.containsKey(boxTypeId)) {
                            GenericValue boxType = null;
                            try {
                                boxType = userLoginDao.findOne(delegator, x.ShipmentBoxType,
                                        UtilMisc.toMap(x.shipmentBoxTypeId, boxTypeId), false);
                            } catch (GenericEntityException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                            }

                            boxTypes.put(boxTypeId, boxType);
                            List<Map<String, Object>> box = new LinkedList<>();
                            boxTypeContent.put(boxTypeId, box);
                        }
                        List<Map<String, Object>> boxTypeContentList = UtilGenerics.cast(boxTypeContent.get(boxTypeId));
                        boxTypeContentList.add(boxTypeContentMap);
                    }
                }
            }
            // The packages and package contents are created.
            for (Map.Entry<String, List<Map<String, Object>>> boxTypeContentEntry : boxTypeContent.entrySet()) {
                String boxTypeId = boxTypeContentEntry.getKey();
                List<Map<String, Object>> contentList = UtilGenerics.cast(boxTypeContentEntry.getValue());
                GenericValue boxType = boxTypes.get(boxTypeId);
                BigDecimal boxWidth = boxType.getBigDecimal(x.boxLength);
                BigDecimal totalWidth = BigDecimal.ZERO;
                if (boxWidth == null) {
                    boxWidth = BigDecimal.ZERO;
                }
                String shipmentPackageSeqId = null;
                for (Map<String, Object> stringObjectMap : contentList) {
                    Map<String, Object> contentMap = UtilGenerics.cast(stringObjectMap);
                    Map<String, Object> content = UtilGenerics.cast(contentMap.get(x.content));
                    OrderReadHelper orderReadHelper = (OrderReadHelper) content.get(x.orderReadHelper);
                    List<BOMNode> productsInPackages = UtilGenerics.cast(content.get(x.productsInPackages));
                    GenericValue orderShipment = (GenericValue) content.get(x.orderShipment);

                    GenericValue product = null;
                    BigDecimal quantity = BigDecimal.ZERO;
                    boolean subProduct = contentMap.containsKey(x.componentIndex);
                    if (subProduct) {
                        // multi package
                        Integer index = (Integer) contentMap.get(x.componentIndex);
                        BOMNode component = productsInPackages.get(index);
                        product = component.getProduct();
                        quantity = component.getQuantity();
                    } else {
                        // single package
                        GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString(x.orderItemSeqId));
                        try {
                            product = orderItem.getRelatedOne(x.Product, false);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                        }
                        quantity = orderShipment.getBigDecimal(x.quantity);
                    }

                    BigDecimal productDepth = product.getBigDecimal(x.shippingDepth);
                    if (productDepth == null) {
                        productDepth = product.getBigDecimal(x.productDepth);
                    }
                    if (productDepth == null) {
                        productDepth = BigDecimal.ONE;
                    }

                    BigDecimal firstMaxNumOfProducts = boxWidth.subtract(totalWidth).divide(productDepth, 0, RoundingMode.FLOOR);
                    if (firstMaxNumOfProducts.compareTo(BigDecimal.ZERO) == 0) firstMaxNumOfProducts = BigDecimal.ONE;
                    //
                    BigDecimal maxNumOfProducts = boxWidth.divide(productDepth, 0, RoundingMode.FLOOR);
                    if (maxNumOfProducts.compareTo(BigDecimal.ZERO) == 0) maxNumOfProducts = BigDecimal.ONE;

                    BigDecimal remQuantity = quantity;
                    boolean isFirst = true;
                    while (remQuantity.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal maxQuantity = BigDecimal.ZERO;
                        if (isFirst) {
                            maxQuantity = firstMaxNumOfProducts;
                            isFirst = false;
                        } else {
                            maxQuantity = maxNumOfProducts;
                        }
                        BigDecimal qty = (remQuantity.compareTo(maxQuantity) < 0 ? remQuantity : maxQuantity);
                        // If needed, create the package
                        if (shipmentPackageSeqId == null) {
                            try {
                                Map<String, Object> serviceResult = dispatcher.runSync(x.createShipmentPackage,
                                        UtilMisc.<String, Object>toMap(x.shipmentId, orderShipment.getString(x.shipmentId), x.shipmentBoxTypeId,
                                                boxTypeId, x.userLogin, userLogin));
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }
                                shipmentPackageSeqId = (String) serviceResult.get(x.shipmentPackageSeqId);
                            } catch (GenericServiceException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                            }
                            totalWidth = BigDecimal.ZERO;
                        }
                        try {
                            Map<String, Object> inputMap = null;
                            if (subProduct) {
                                inputMap = UtilMisc.toMap(x.shipmentId, orderShipment.getString(x.shipmentId),
                                        x.shipmentPackageSeqId, shipmentPackageSeqId,
                                        x.shipmentItemSeqId, orderShipment.getString(x.shipmentItemSeqId),
                                        x.subProductId, product.getString(x.productId),
                                        x.userLogin, userLogin,
                                        x.subProductQuantity, qty);
                            } else {
                                inputMap = UtilMisc.toMap(x.shipmentId, orderShipment.getString(x.shipmentId),
                                        x.shipmentPackageSeqId, shipmentPackageSeqId,
                                        x.shipmentItemSeqId, orderShipment.getString(x.shipmentItemSeqId),
                                        x.userLogin, userLogin,
                                        x.quantity, qty);
                            }
                            Map<String, Object> serviceResult = dispatcher.runSync(x.createShipmentPackageContent, inputMap);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                            }
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingPackageConfiguratorError, locale));
                        }
                        totalWidth = totalWidth.add(qty.multiply(productDepth));
                        if (qty.compareTo(maxQuantity) == 0) shipmentPackageSeqId = null;
                        remQuantity = remQuantity.subtract(qty);
                    }
                }
            }
        }
        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns its (possibly configured) components in
     * a List of {@link BOMNode}).
     * @param dctx the distach context
     * @param context the context
     * @return returns the list of products in packages
     */
    public static Map<String, Object> getProductsInPackages(DispatchContext dctx, BOMServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String fromDateStr = (String) context.get(x.fromDate);

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        //
        // Components
        //
        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        try {
            tree = new BOMTree(productId, x.MANUF_COMPONENT, fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.getProductsInPackages(components);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }

        result.put(x.productsInPackages, components);

        return result;
    }

}
