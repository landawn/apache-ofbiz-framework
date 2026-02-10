/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.order.order;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.OrderHeaderDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.ProductEntity;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.widget.renderer.Paginator;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.OrderLookupServicesContext;
/**
 * OrderLookupServices
 */
public class OrderLookupServices {

    private static final String MODULE = OrderLookupServices.class.getName();

    public static Map<String, Object> findOrders(DispatchContext dctx, OrderLookupServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Integer viewIndex = Paginator.getViewIndex(context, x.viewIndex, 1);
        Integer viewSize = Paginator.getViewSize(context, x.viewSize);

        String showAll = (String) context.get(x.showAll);
        String useEntryDate = (String) context.get(x.useEntryDate);
        Locale locale = (Locale) context.get(x.locale);
        if (showAll == null) {
            showAll = x.N;
        }

        // list of fields to select (initial list)
        Set<String> fieldsToSelect = new LinkedHashSet<>();
        fieldsToSelect.add(x.orderId);
        fieldsToSelect.add(x.orderName);
        fieldsToSelect.add(x.statusId);
        fieldsToSelect.add(x.orderTypeId);
        fieldsToSelect.add(x.orderDate);
        fieldsToSelect.add(x.currencyUom);
        fieldsToSelect.add(x.grandTotal);
        fieldsToSelect.add(x.remainingSubTotal);

        // sorting by order date newest first
        List<String> orderBy = UtilMisc.toList(x.orderDate_6205b430, x.orderId_08c97713);

        // list to hold the parameters
        List<String> paramList = new LinkedList<>();

        // list of conditions
        List<EntityCondition> conditions = new LinkedList<>();

        // check security flag for purchase orders
        boolean canViewPo = security.hasEntityPermission(x.ORDERMGR, x.PURCHASE_VIEW, userLogin);
        if (!canViewPo) {
            conditions.add(EntityCondition.makeCondition(x.orderTypeId, EntityOperator.NOT_EQUAL, x.PURCHASE_ORDER));
        }

        // dynamic view entity
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity(x.OH, x.OrderHeader);
        dve.addAliasAll(x.OH, x.emptyString, null); // no prefix
        dve.addRelation(x.one_nofk, x.emptyString, x.OrderType, UtilMisc.toList(new ModelKeyMap(x.orderTypeId, x.orderTypeId)));
        dve.addRelation(x.one_nofk, x.emptyString, x.StatusItem, UtilMisc.toList(new ModelKeyMap(x.statusId, x.statusId)));

        // start the lookup
        String orderId = (String) context.get(x.orderId);
        if (UtilValidate.isNotEmpty(orderId)) {
            paramList.add(x.orderId_59b511c9 + orderId);
            conditions.add(makeExpr(x.orderId, orderId));
        }

        // the base order header fields
        List<String> orderTypeList = UtilGenerics.cast(context.get(x.orderTypeId));
        if (orderTypeList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String orderTypeId : orderTypeList) {
                paramList.add(x.orderTypeId_d977414d + orderTypeId);

                if (!(x.PURCHASE_ORDER.equals(orderTypeId)) || ((x.PURCHASE_ORDER.equals(orderTypeId) && canViewPo))) {
                    orExprs.add(EntityCondition.makeCondition(x.orderTypeId, EntityOperator.EQUALS, orderTypeId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String orderName = (String) context.get(x.orderName);
        if (UtilValidate.isNotEmpty(orderName)) {
            paramList.add(x.orderName_e6f17db8 + orderName);
            conditions.add(makeExpr(x.orderName, orderName, true));
        }

        List<String> orderStatusList = UtilGenerics.cast(context.get(x.orderStatusId));
        if (orderStatusList != null) {
            List<EntityCondition> orExprs = new LinkedList<>();
            for (String orderStatusId : orderStatusList) {
                paramList.add(x.orderStatusId_bfb29575 + orderStatusId);
                if (x.PENDING.equals(orderStatusId)) {
                    List<EntityExpr> pendExprs = new LinkedList<>();
                    pendExprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.ORDER_CREATED));
                    pendExprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.ORDER_PROCESSING));
                    pendExprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.ORDER_APPROVED));
                    orExprs.add(EntityCondition.makeCondition(pendExprs, EntityOperator.OR));
                } else {
                    orExprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, orderStatusId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> productStoreList = UtilGenerics.cast(context.get(x.productStoreId));
        if (productStoreList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String productStoreId : productStoreList) {
                paramList.add(x.productStoreId_73804b67 + productStoreId);
                orExprs.add(EntityCondition.makeCondition(x.productStoreId, EntityOperator.EQUALS, productStoreId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> webSiteList = UtilGenerics.cast(context.get(x.orderWebSiteId));
        if (webSiteList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String webSiteId : webSiteList) {
                paramList.add(x.webSiteId_d6e82b4f + webSiteId);
                orExprs.add(EntityCondition.makeCondition(x.webSiteId, EntityOperator.EQUALS, webSiteId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> saleChannelList = UtilGenerics.cast(context.get(x.salesChannelEnumId));
        if (saleChannelList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String salesChannelEnumId : saleChannelList) {
                paramList.add(x.salesChannelEnumId_d6c9f2ca + salesChannelEnumId);
                orExprs.add(EntityCondition.makeCondition(x.salesChannelEnumId, EntityOperator.EQUALS, salesChannelEnumId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String createdBy = (String) context.get(x.createdBy);
        if (UtilValidate.isNotEmpty(createdBy)) {
            paramList.add(x.createdBy_6365b3ad + createdBy);
            conditions.add(makeExpr(x.createdBy, createdBy));
        }

        String terminalId = (String) context.get(x.terminalId);
        if (UtilValidate.isNotEmpty(terminalId)) {
            paramList.add(x.terminalId_3171cfbe + terminalId);
            conditions.add(makeExpr(x.terminalId, terminalId));
        }

        String transactionId = (String) context.get(x.transactionId);
        if (UtilValidate.isNotEmpty(transactionId)) {
            paramList.add(x.transactionId_9ac711cf + transactionId);
            conditions.add(makeExpr(x.transactionId, transactionId));
        }

        String externalId = (String) context.get(x.externalId);
        if (UtilValidate.isNotEmpty(externalId)) {
            paramList.add(x.externalId_8e335182 + externalId);
            conditions.add(makeExpr(x.externalId, externalId));
        }

        String internalCode = (String) context.get(x.internalCode);
        if (UtilValidate.isNotEmpty(internalCode)) {
            paramList.add(x.internalCode_dfd6f831 + internalCode);
            conditions.add(makeExpr(x.internalCode, internalCode));
        }

        String dateField = x.Y.equals(useEntryDate) ? x.entryDate : x.orderDate;
        String minDate = (String) context.get(x.minDate);
        if (UtilValidate.isNotEmpty(minDate) && minDate.length() > 8) {
            minDate = minDate.trim();
            if (minDate.length() < 14) {
                minDate = minDate + x.str_b858cb28 + x._00_00_00_000;
            }
            paramList.add(x.minDate_6ccb84c0 + minDate);

            try {
                Object converted = ObjectType.simpleTypeOrObjectConvert(minDate, x.Timestamp, null, null);
                if (converted != null) {
                    conditions.add(EntityCondition.makeCondition(dateField, EntityOperator.GREATER_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
        }

        String maxDate = (String) context.get(x.maxDate);
        if (UtilValidate.isNotEmpty(maxDate) && maxDate.length() > 8) {
            maxDate = maxDate.trim();
            if (maxDate.length() < 14) {
                maxDate = maxDate + x.str_b858cb28 + x._23_59_59_999;
            }
            paramList.add(x.maxDate_2b186807 + maxDate);

            try {
                Object converted = ObjectType.simpleTypeOrObjectConvert(maxDate, x.Timestamp, null, null);
                if (converted != null) {
                    conditions.add(EntityCondition.makeCondition(x.orderDate, EntityOperator.LESS_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
        }

        // party (role) fields
        String userLoginId = (String) context.get(x.userLoginId);
        String partyId = (String) context.get(x.partyId);
        List<String> roleTypeList = UtilGenerics.cast(context.get(x.roleTypeId));

        if (UtilValidate.isNotEmpty(userLoginId) && UtilValidate.isEmpty(partyId)) {
            GenericValue ul = null;
            try {
                UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                UserLoginEntity userLoginEntity = userLoginDao.get(userLoginId).orElse(null);
                if (userLoginEntity != null) {
                    ul = delegator.makeValue(x.UserLogin, Beans.beanToMap(userLoginEntity));
                }
            } catch (Exception e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
            if (ul != null) {
                partyId = ul.getString(x.partyId);
            }
        }

        String isViewed = (String) context.get(x.isViewed);
        if (UtilValidate.isNotEmpty(isViewed)) {
            paramList.add(x.isViewed_6b67b015 + isViewed);
            conditions.add(makeExpr(x.isViewed, isViewed));
        }

        // Shipment Method
        String shipmentMethod = (String) context.get(x.shipmentMethod);
        if (UtilValidate.isNotEmpty(shipmentMethod)) {
            String carrierPartyId = shipmentMethod.substring(0, shipmentMethod.indexOf('@'));
            String shippingMethodTypeId = shipmentMethod.substring(shipmentMethod.indexOf('@') + 1);
            dve.addMemberEntity(x.OISG, x.OrderItemShipGroup);
            dve.addAlias(x.OISG, x.shipmentMethodTypeId);
            dve.addAlias(x.OISG, x.carrierPartyId);
            dve.addViewLink(x.OH, x.OISG, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));

            if (UtilValidate.isNotEmpty(carrierPartyId)) {
                paramList.add(x.carrierPartyId_a0a8e006 + carrierPartyId);
                conditions.add(makeExpr(x.carrierPartyId, carrierPartyId));
            }

            if (UtilValidate.isNotEmpty(shippingMethodTypeId)) {
                paramList.add(x.shippingMethodTypeId + shippingMethodTypeId);
                conditions.add(makeExpr(x.shipmentMethodTypeId, shippingMethodTypeId));
            }
        }
        // PaymentGatewayResponse
        String gatewayAvsResult = (String) context.get(x.gatewayAvsResult);
        String gatewayScoreResult = (String) context.get(x.gatewayScoreResult);
        if (UtilValidate.isNotEmpty(gatewayAvsResult) || UtilValidate.isNotEmpty(gatewayScoreResult)) {
            dve.addMemberEntity(x.OPP, x.OrderPaymentPreference);
            dve.addMemberEntity(x.PGR, x.PaymentGatewayResponse);
            dve.addAlias(x.OPP, x.orderPaymentPreferenceId);
            dve.addAlias(x.PGR, x.gatewayAvsResult);
            dve.addAlias(x.PGR, x.gatewayScoreResult);
            dve.addViewLink(x.OH, x.OPP, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));
            dve.addViewLink(x.OPP, x.PGR, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderPaymentPreferenceId, x.orderPaymentPreferenceId)));
        }

        if (UtilValidate.isNotEmpty(gatewayAvsResult)) {
            paramList.add(x.gatewayAvsResult_305a01a2 + gatewayAvsResult);
            conditions.add(EntityCondition.makeCondition(x.gatewayAvsResult, gatewayAvsResult));
        }

        if (UtilValidate.isNotEmpty(gatewayScoreResult)) {
            paramList.add(x.gatewayScoreResult_0e9b4bef + gatewayScoreResult);
            conditions.add(EntityCondition.makeCondition(x.gatewayScoreResult, gatewayScoreResult));
        }

        // add the role data to the view
        if (roleTypeList != null || partyId != null) {
            dve.addMemberEntity(x.OT, x.OrderRole);
            dve.addAlias(x.OT, x.partyId);
            dve.addAlias(x.OT, x.roleTypeId);
            dve.addViewLink(x.OH, x.OT, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));
        }

        if (UtilValidate.isNotEmpty(partyId)) {
            paramList.add(x.partyId_3b2da66c + partyId);
            fieldsToSelect.add(x.partyId);
            conditions.add(makeExpr(x.partyId, partyId));
        }

        if (roleTypeList != null) {
            fieldsToSelect.add(x.roleTypeId);
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String roleTypeId : roleTypeList) {
                paramList.add(x.roleTypeId_535b78d0 + roleTypeId);
                orExprs.add(makeExpr(x.roleTypeId, roleTypeId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        // order item fields
        String correspondingPoId = (String) context.get(x.correspondingPoId);
        String subscriptionId = (String) context.get(x.subscriptionId);
        String productId = (String) context.get(x.productId);
        String budgetId = (String) context.get(x.budgetId);
        String quoteId = (String) context.get(x.quoteId);

        String goodIdentificationTypeId = (String) context.get(x.goodIdentificationTypeId);
        String goodIdentificationIdValue = (String) context.get(x.goodIdentificationIdValue);
        boolean hasGoodIdentification = UtilValidate.isNotEmpty(goodIdentificationTypeId) && UtilValidate.isNotEmpty(goodIdentificationIdValue);

        if (correspondingPoId != null || subscriptionId != null || productId != null || budgetId != null || quoteId != null
                || hasGoodIdentification) {
            dve.addMemberEntity(x.OI, x.OrderItem);
            dve.addAlias(x.OI, x.correspondingPoId);
            dve.addAlias(x.OI, x.subscriptionId);
            dve.addAlias(x.OI, x.productId);
            dve.addAlias(x.OI, x.budgetId);
            dve.addAlias(x.OI, x.quoteId);
            dve.addViewLink(x.OH, x.OI, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));

            if (hasGoodIdentification) {
                dve.addMemberEntity(x.GOODID, x.GoodIdentification);
                dve.addAlias(x.GOODID, x.goodIdentificationTypeId);
                dve.addAlias(x.GOODID, x.idValue);
                dve.addViewLink(x.OI, x.GOODID, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.productId, x.productId)));
                paramList.add(x.goodIdentificationTypeId_4bbbd512 + goodIdentificationTypeId);
                conditions.add(makeExpr(x.goodIdentificationTypeId, goodIdentificationTypeId));
                paramList.add(x.goodIdentificationIdValue_d81c0b10 + goodIdentificationIdValue);
                conditions.add(makeExpr(x.idValue, goodIdentificationIdValue));
            }
        }

        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            paramList.add(x.correspondingPoId_ded5be37 + correspondingPoId);
            conditions.add(makeExpr(x.correspondingPoId, correspondingPoId));
        }

        if (UtilValidate.isNotEmpty(subscriptionId)) {
            paramList.add(x.subscriptionId_1653d932 + subscriptionId);
            conditions.add(makeExpr(x.subscriptionId, subscriptionId));
        }

        if (UtilValidate.isNotEmpty(productId)) {
            paramList.add(x.productId_bdd79be6 + productId);
            if (productId.startsWith(x.str_4345cb1f) || productId.startsWith(x.str_df58248c) || productId.endsWith(x.str_4345cb1f) || productId.endsWith(x.str_df58248c)) {
                conditions.add(makeExpr(x.productId, productId));
            } else {
                GenericValue product = null;
                try {
                    ProductDao productDao = DaoRegistry.getDao(delegator, x.Product, ProductDao.class);
                    ProductEntity productEntity = productDao.get(productId).orElse(null);
                    if (productEntity != null) {
                        product = delegator.makeValue(x.Product, Beans.beanToMap(productEntity));
                    }
                } catch (Exception e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                }
                if (product != null) {
                    String isVirtual = product.getString(x.isVirtual);
                    if (isVirtual != null && x.Y.equals(isVirtual)) {
                        List<EntityExpr> orExprs = new LinkedList<>();
                        orExprs.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId));

                        Map<String, Object> varLookup = null;
                        List<GenericValue> variants = null;
                        try {
                            varLookup = dispatcher.runSync(x.getAllProductVariants, UtilMisc.toMap(x.productId, productId));
                            if (ServiceUtil.isError(varLookup)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(varLookup));
                            }
                            variants = UtilGenerics.cast(varLookup.get(x.assocProducts));

                        } catch (GenericServiceException e) {
                            Debug.logWarning(e.getMessage(), MODULE);
                        }
                        if (variants != null) {
                            for (GenericValue v : variants) {
                                orExprs.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, v.getString(x.productIdTo)));
                            }
                        }
                        conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
                    } else {
                        conditions.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId));
                    }
                } else {
                    String failMsg = UtilProperties.getMessage(x.OrderErrorUiLabels, x.OrderFindOrderProductInvalid,
                            UtilMisc.toMap(x.productId, productId), locale);
                    return ServiceUtil.returnFailure(failMsg);
                }
            }
        }

        if (UtilValidate.isNotEmpty(budgetId)) {
            paramList.add(x.budgetId_23c89c0d + budgetId);
            conditions.add(makeExpr(x.budgetId, budgetId));
        }

        if (UtilValidate.isNotEmpty(quoteId)) {
            paramList.add(x.quoteId_20588149 + quoteId);
            conditions.add(makeExpr(x.quoteId, quoteId));
        }

        // payment preference fields
        String billingAccountId = (String) context.get(x.billingAccountId);
        String finAccountId = (String) context.get(x.finAccountId);
        String cardNumber = (String) context.get(x.cardNumber);
        String accountNumber = (String) context.get(x.accountNumber);
        String paymentStatusId = (String) context.get(x.paymentStatusId);

        if (UtilValidate.isNotEmpty(paymentStatusId)) {
            paramList.add(x.paymentStatusId_8ea9a6ec + paymentStatusId);
            conditions.add(makeExpr(x.paymentStatusId, paymentStatusId));
        }
        if (finAccountId != null || cardNumber != null || accountNumber != null || paymentStatusId != null) {
            dve.addMemberEntity(x.OP, x.OrderPaymentPreference);
            dve.addAlias(x.OP, x.finAccountId);
            dve.addAlias(x.OP, x.paymentMethodId);
            dve.addAlias(x.OP, x.paymentStatusId, x.statusId, null, false, false, null);
            dve.addViewLink(x.OH, x.OP, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));
        }

        // search by billing account ID
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            paramList.add(x.billingAccountId_3ac2c60c + billingAccountId);
            conditions.add(makeExpr(x.billingAccountId, billingAccountId));
        }

        // search by fin account ID
        if (UtilValidate.isNotEmpty(finAccountId)) {
            paramList.add(x.finAccountId_b0ca29c7 + finAccountId);
            conditions.add(makeExpr(x.finAccountId, finAccountId));
        }

        // search by card number
        if (UtilValidate.isNotEmpty(cardNumber)) {
            dve.addMemberEntity(x.CC, x.CreditCard);
            dve.addAlias(x.CC, x.cardNumber);
            dve.addViewLink(x.OP, x.CC, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.paymentMethodId, x.paymentMethodId)));

            paramList.add(x.cardNumber_c4ae34f0 + cardNumber);
            conditions.add(makeExpr(x.cardNumber, cardNumber));
        }

        // search by eft account number
        if (UtilValidate.isNotEmpty(accountNumber)) {
            dve.addMemberEntity(x.EF, x.EftAccount);
            dve.addAlias(x.EF, x.accountNumber);
            dve.addViewLink(x.OP, x.EF, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.paymentMethodId, x.paymentMethodId)));

            paramList.add(x.accountNumber_b7afab7d + accountNumber);
            conditions.add(makeExpr(x.accountNumber, accountNumber));
        }

        // shipment/inventory item
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        String softIdentifier = (String) context.get(x.softIdentifier);
        String serialNumber = (String) context.get(x.serialNumber);
        String shipmentId = (String) context.get(x.shipmentId);

        if (shipmentId != null || inventoryItemId != null || softIdentifier != null || serialNumber != null) {
            dve.addMemberEntity(x.II, x.ItemIssuance);
            dve.addAlias(x.II, x.shipmentId);
            dve.addAlias(x.II, x.inventoryItemId);
            dve.addViewLink(x.OH, x.II, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));

            if (softIdentifier != null || serialNumber != null) {
                dve.addMemberEntity(x.IV, x.InventoryItem);
                dve.addAlias(x.IV, x.softIdentifier);
                dve.addAlias(x.IV, x.serialNumber);
                dve.addViewLink(x.II, x.IV, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.inventoryItemId, x.inventoryItemId)));
            }
        }

        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            paramList.add(x.inventoryItemId_96043f42 + inventoryItemId);
            conditions.add(makeExpr(x.inventoryItemId, inventoryItemId));
        }

        if (UtilValidate.isNotEmpty(softIdentifier)) {
            paramList.add(x.softIdentifier_16cb873e + softIdentifier);
            conditions.add(makeExpr(x.softIdentifier, softIdentifier, true));
        }

        if (UtilValidate.isNotEmpty(serialNumber)) {
            paramList.add(x.serialNumber_1d9b2d12 + serialNumber);
            conditions.add(makeExpr(x.serialNumber, serialNumber, true));
        }

        if (UtilValidate.isNotEmpty(shipmentId)) {
            paramList.add(x.shipmentId_39b0412c + shipmentId);
            conditions.add(makeExpr(x.shipmentId, shipmentId));
        }

        // back order checking
        String hasBackOrders = (String) context.get(x.hasBackOrders);
        if (UtilValidate.isNotEmpty(hasBackOrders)) {
            dve.addMemberEntity(x.IR, x.OrderItemShipGrpInvRes);
            dve.addAlias(x.IR, x.quantityNotAvailable);
            dve.addViewLink(x.OH, x.IR, Boolean.FALSE, UtilMisc.toList(new ModelKeyMap(x.orderId, x.orderId)));

            paramList.add(x.hasBackOrders_068d7492 + hasBackOrders);
            if (x.Y.equals(hasBackOrders)) {
                conditions.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.NOT_EQUAL, null));
                conditions.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.GREATER_THAN, BigDecimal.ZERO));
            } else if (x.N.equals(hasBackOrders)) {
                List<EntityExpr> orExpr = new LinkedList<>();
                orExpr.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.EQUALS, null));
                orExpr.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.EQUALS, BigDecimal.ZERO));
                conditions.add(EntityCondition.makeCondition(orExpr, EntityOperator.OR));
            }
        }

        // Get all orders according to specific ship to country with "Only Include" or "Do not Include".
        String countryGeoId = (String) context.get(x.countryGeoId);
        String includeCountry = (String) context.get(x.includeCountry);
        if (UtilValidate.isNotEmpty(countryGeoId) && UtilValidate.isNotEmpty(includeCountry)) {
            paramList.add(x.countryGeoId_ae42a2a2 + countryGeoId);
            paramList.add(x.includeCountry_3c82fe3f + includeCountry);
            // add condition to dynamic view
            dve.addMemberEntity(x.OCM, x.OrderContactMech);
            dve.addMemberEntity(x.PA, x.PostalAddress);
            dve.addAlias(x.OCM, x.contactMechId);
            dve.addAlias(x.OCM, x.contactMechPurposeTypeId);
            dve.addAlias(x.PA, x.countryGeoId);
            dve.addViewLink(x.OH, x.OCM, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.orderId));
            dve.addViewLink(x.OCM, x.PA, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

            EntityConditionList<EntityExpr> exprs = null;
            if (x.Y.equals(includeCountry)) {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(x.contactMechPurposeTypeId, x.SHIPPING_LOCATION),
                            EntityCondition.makeCondition(x.countryGeoId, countryGeoId)), EntityOperator.AND);
            } else {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(x.contactMechPurposeTypeId, x.SHIPPING_LOCATION),
                            EntityCondition.makeCondition(x.countryGeoId, EntityOperator.NOT_EQUAL, countryGeoId)), EntityOperator.AND);
            }
            conditions.add(exprs);
        }

        // create the main condition
        EntityCondition cond = null;
        if (!conditions.isEmpty() || x.Y.equalsIgnoreCase(showAll)) {
            cond = EntityCondition.makeCondition(conditions, EntityOperator.AND);
        }

        if (Debug.verboseOn()) {
            Debug.logInfo(x.Find_order_query + cond.toString(), MODULE);
        }

        List<GenericValue> orderList = new LinkedList<>();
        int orderCount = 0;

        // get the index for the partial list
        int lowIndex = 0;
        int highIndex = 0;

        if (cond != null) {
            PagedList<GenericValue> pagedOrderList = null;
            try {
                OrderHeaderDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class);
                pagedOrderList = orderHeaderDao.queryPagedList(delegator, dve, cond, fieldsToSelect, orderBy, viewIndex - 1, viewSize);

                orderCount = pagedOrderList.getSize();
                lowIndex = pagedOrderList.getStartIndex();
                highIndex = pagedOrderList.getEndIndex();
                orderList = pagedOrderList.getData();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create the result map
        Map<String, Object> result = ServiceUtil.returnSuccess();

        // filter out requested inventory problems
        filterInventoryProblems(context, result, orderList, paramList);

        // format the param list
        String paramString = StringUtil.join(paramList, x.amp);

        result.put(x.highIndex, highIndex);
        result.put(x.lowIndex, lowIndex);
        result.put(x.viewIndex, viewIndex);
        result.put(x.viewSize, viewSize);
        result.put(x.showAll, showAll);

        result.put(x.paramList, (paramString != null ? paramString : x.emptyString));
        result.put(x.orderList, orderList);
        result.put(x.orderListSize, orderCount);

        return result;
    }

    public static void filterInventoryProblems(OrderLookupServicesContext context, Map<String, Object> result, List<GenericValue>
            orderList, List<String> paramList) {
        List<String> filterInventoryProblems = new LinkedList<>();

        String doFilter = (String) context.get(x.filterInventoryProblems);
        if (doFilter == null) {
            doFilter = x.N;
        }

        if (x.Y.equals(doFilter) && !orderList.isEmpty()) {
            paramList.add(x.filterInventoryProblems_Y);
            for (GenericValue orderHeader : orderList) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                BigDecimal backorderQty = orh.getOrderBackorderQuantity();
                if (backorderQty.compareTo(BigDecimal.ZERO) == 1) {
                    filterInventoryProblems.add(orh.getOrderId());
                }
            }
        }

        List<String> filterPOsOpenPastTheirETA = new LinkedList<>();
        List<String> filterPOsWithRejectedItems = new LinkedList<>();
        List<String> filterPartiallyReceivedPOs = new LinkedList<>();

        String filterPOReject = (String) context.get(x.filterPOsWithRejectedItems);
        String filterPOPast = (String) context.get(x.filterPOsOpenPastTheirETA);
        String filterPartRec = (String) context.get(x.filterPartiallyReceivedPOs);
        if (filterPOReject == null) {
            filterPOReject = x.N;
        }
        if (filterPOPast == null) {
            filterPOPast = x.N;
        }
        if (filterPartRec == null) {
            filterPartRec = x.N;
        }

        boolean doPoFilter = false;
        if (x.Y.equals(filterPOReject)) {
            paramList.add(x.filterPOsWithRejectedItems_Y);
            doPoFilter = true;
        }
        if (x.Y.equals(filterPOPast)) {
            paramList.add(x.filterPOsOpenPastTheirETA_Y);
            doPoFilter = true;
        }
        if (x.Y.equals(filterPartRec)) {
            paramList.add(x.filterPartiallyReceivedPOs_Y);
            doPoFilter = true;
        }

        if (doPoFilter && !orderList.isEmpty()) {
            for (GenericValue orderHeader : orderList) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                String orderType = orh.getOrderTypeId();
                String orderId = orh.getOrderId();

                if (x.PURCHASE_ORDER.equals(orderType)) {
                    if (x.Y.equals(filterPOReject) && orh.getRejectedOrderItems()) {
                        filterPOsWithRejectedItems.add(orderId);
                    } else if (x.Y.equals(filterPOPast) && orh.getPastEtaOrderItems(orderId)) {
                        filterPOsOpenPastTheirETA.add(orderId);
                    } else if (x.Y.equals(filterPartRec) && orh.getPartiallyReceivedItems()) {
                        filterPartiallyReceivedPOs.add(orderId);
                    }
                }
            }
        }

        result.put(x.filterInventoryProblemsList, filterInventoryProblems);
        result.put(x.filterPOsWithRejectedItemsList, filterPOsWithRejectedItems);
        result.put(x.filterPOsOpenPastTheirETAList, filterPOsOpenPastTheirETA);
        result.put(x.filterPartiallyReceivedPOsList, filterPartiallyReceivedPOs);
    }

    protected static EntityExpr makeExpr(String fieldName, String value) {
        return makeExpr(fieldName, value, false);
    }

    protected static EntityExpr makeExpr(String fieldName, String value, boolean forceLike) {
        EntityComparisonOperator<?, ?> op = forceLike ? EntityOperator.LIKE : EntityOperator.EQUALS;

        if (value.startsWith(x.str_df58248c)) {
            op = EntityOperator.LIKE;
            value = x.str_4345cb1f + value.substring(1);
        } else if (value.startsWith(x.str_4345cb1f)) {
            op = EntityOperator.LIKE;
        }

        if (value.endsWith(x.str_df58248c)) {
            op = EntityOperator.LIKE;
            value = value.substring(0, value.length() - 1) + x.str_4345cb1f;
        } else if (value.endsWith(x.str_4345cb1f)) {
            op = EntityOperator.LIKE;
        }

        if (forceLike) {
            if (!value.startsWith(x.str_4345cb1f)) {
                value = x.str_4345cb1f + value;
            }
            if (!value.endsWith(x.str_4345cb1f)) {
                value = value + x.str_4345cb1f;
            }
        }

        return EntityCondition.makeCondition(fieldName, op, value);
    }
}
