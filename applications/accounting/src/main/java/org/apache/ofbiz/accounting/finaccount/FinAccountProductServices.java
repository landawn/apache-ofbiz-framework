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

package org.apache.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FinAccountTypeDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureApplDao;
import org.apache.ofbiz.persistence.dao.ProductFeatureDao;
import org.apache.ofbiz.persistence.entity.FinAccountTypeEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplEntity;
import org.apache.ofbiz.persistence.entity.ProductFeatureEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FinAccountProductServicesContext;
/**
 * FinAccountProductServices - Financial Accounts created from product purchases
 * (i.e. gift certificates)
 */
public class FinAccountProductServices {

    private static final String MODULE = FinAccountProductServices.class.getName();
    private static final String RES_ORDER_ERROR = x.OrderErrorUiLabels;
    private static final String RES_ERROR = x.AccountingErrorUiLabels;

    public static Map<String, Object> createPartyFinAccountFromPurchase(DispatchContext dctx, FinAccountProductServicesContext context) {
        // this service should always be called via FULFILLMENT_EXTASYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue orderItem = (GenericValue) context.get(x.orderItem);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        // order ID for tracking
        String orderId = orderItem.getString(x.orderId);
        String orderItemSeqId = orderItem.getString(x.orderItemSeqId);

        // the order header for store info
        GenericValue orderHeader;
        try {
            orderHeader = orderItem.getRelatedOne(x.OrderHeader, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Unable_to_get_OrderHeader_from_OrderItem, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER_ERROR,
                    x.OrderCannotGetOrderHeader, UtilMisc.toMap(x.orderId, orderId), locale));
        }

        String productId = orderItem.getString(x.productId);
        GenericValue featureAndAppl;
        try {
            ProductFeatureApplDao productFeatureApplDao = DaoRegistry.getDao(delegator, x.ProductFeatureAppl, ProductFeatureApplDao.class);
            ProductFeatureDao productFeatureDao = DaoRegistry.getDao(delegator, x.ProductFeature, ProductFeatureDao.class);

            List<ProductFeatureApplEntity> productFeatureApplEntities = productFeatureApplDao.list(Filters.and(
                    Filters.eq(x.productId, productId),
                    Filters.eq(x.productFeatureApplTypeId, x.STANDARD_FEATURE)));

            List<GenericValue> productFeatureAppls = new ArrayList<>(productFeatureApplEntities.size());
            for (ProductFeatureApplEntity productFeatureApplEntity : productFeatureApplEntities) {
                productFeatureAppls.add(delegator.makeValue(x.ProductFeatureAppl, Beans.beanToMap(productFeatureApplEntity)));
            }
            productFeatureAppls = EntityUtil.filterByDate(productFeatureAppls);

            featureAndAppl = null;
            for (GenericValue productFeatureAppl : productFeatureAppls) {
                ProductFeatureEntity productFeatureEntity = productFeatureDao.get(productFeatureAppl.getString(x.productFeatureId)).orElse(null);
                if (productFeatureEntity != null && x.TYPE.equals(productFeatureEntity.getProductFeatureTypeId())) {
                    featureAndAppl = delegator.makeValue(x.ProductFeatureAndAppl, Beans.beanToMap(productFeatureEntity));
                    break;
                }
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // financial account data; pulled from the TYPE feature of the product
        String finAccountTypeId = x.BALANCE_ACCOUNT; // default
        String finAccountName = x.Customer_Financial_Account;
        if (featureAndAppl != null) {
            if (UtilValidate.isNotEmpty(featureAndAppl.getString(x.idCode))) {
                finAccountTypeId = featureAndAppl.getString(x.idCode);
            }
            if (UtilValidate.isNotEmpty(featureAndAppl.getString(x.description))) {
                finAccountName = featureAndAppl.getString(x.description);
            }
        }

        // locate the financial account type
        GenericValue finAccountType;
        try {
            FinAccountTypeDao finAccountTypeDao = DaoRegistry.getDao(delegator, x.FinAccountType, FinAccountTypeDao.class);
            FinAccountTypeEntity finAccountTypeEntity = finAccountTypeDao.get(finAccountTypeId).orElse(null);
            finAccountType = finAccountTypeEntity == null ? null : delegator.makeValue(x.FinAccountType, Beans.beanToMap(finAccountTypeEntity));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String replenishEnumId = finAccountType.getString(x.replenishEnumId);

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            Debug.logFatal(x.Unable_to_create_financial_accout_no_productStoreId_on_OrderHeader + orderId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountCannotCreate,
                    UtilMisc.toMap(x.orderId, orderId), locale));
        }

        // party ID (owner)
        GenericValue billToParty = orh.getBillToParty();
        String partyId = null;
        if (billToParty != null) {
            partyId = billToParty.getString(x.partyId);
        }

        // payment method info
        List<GenericValue> payPrefs = orh.getPaymentPreferences();
        String paymentMethodId = null;
        if (payPrefs != null) {
            for (GenericValue pref : payPrefs) {
                // needs to be a CC or EFT account
                String type = pref.getString(x.paymentMethodTypeId);
                if (x.CREDIT_CARD.equals(type) || x.EFT_ACCOUNT.equals(type)) {
                    paymentMethodId = pref.getString(x.paymentMethodId);
                }
            }
        }
        // some person data for expanding
        GenericValue partyGroup = null;
        GenericValue person = null;
        GenericValue party = null;

        if (billToParty != null) {
            try {
                party = billToParty.getRelatedOne(x.Party, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (party != null) {
                String partyTypeId = party.getString(x.partyTypeId);
                if (x.PARTY_GROUP.equals(partyTypeId)) {
                    partyGroup = billToParty;
                } else if (x.PERSON.equals(partyTypeId)) {
                    person = billToParty;
                }
            }
        }

        // create the context for FSE
        Map<String, Object> expContext = new HashMap<>();
        expContext.put(x.orderHeader, orderHeader);
        expContext.put(x.orderItem, orderItem);
        expContext.put(x.party, party);
        expContext.put(x.person, person);
        expContext.put(x.partyGroup, partyGroup);

        // expand the name field to dynamically add information
        FlexibleStringExpander exp = FlexibleStringExpander.getInstance(finAccountName);
        finAccountName = exp.expandString(expContext);

        // price/amount/quantity to create initial deposit amount
        BigDecimal quantity = orderItem.getBigDecimal(x.quantity);
        BigDecimal price = orderItem.getBigDecimal(x.unitPrice);
        BigDecimal deposit = price.multiply(quantity).setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());

        // create the financial account
        Map<String, Object> createCtx = new HashMap<>();
        String finAccountId;

        createCtx.put(x.finAccountTypeId, finAccountTypeId);
        createCtx.put(x.finAccountName, finAccountName);
        createCtx.put(x.productStoreId, productStoreId);
        createCtx.put(x.ownerPartyId, partyId);
        createCtx.put(x.currencyUomId, currency);
        createCtx.put(x.statusId, x.FNACT_ACTIVE);
        createCtx.put(x.userLogin, userLogin);

        // if we auto-replenish this type; set the level to the initial deposit
        if (replenishEnumId != null && x.FARP_AUTOMATIC.equals(replenishEnumId)) {
            createCtx.put(x.replenishLevel, deposit);
            createCtx.put(x.replenishPaymentId, paymentMethodId);
        }

        Map<String, Object> createResp;
        try {
            createResp = dispatcher.runSync(x.createFinAccountForStore, createCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(createResp), MODULE);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResp));
        }

        finAccountId = (String) createResp.get(x.finAccountId);

        // create the owner role
        Map<String, Object> roleCtx = new HashMap<>();
        roleCtx.put(x.partyId, partyId);
        roleCtx.put(x.roleTypeId, x.OWNER);
        roleCtx.put(x.finAccountId, finAccountId);
        roleCtx.put(x.userLogin, userLogin);
        roleCtx.put(x.fromDate, UtilDateTime.nowTimestamp());
        Map<String, Object> roleResp;
        try {
            roleResp = dispatcher.runSync(x.createFinAccountRole, roleCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(roleResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(roleResp), MODULE);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(roleResp));
        }

        // create the initial deposit
        Map<String, Object> depositCtx = new HashMap<>();
        depositCtx.put(x.finAccountId, finAccountId);
        depositCtx.put(x.productStoreId, productStoreId);
        depositCtx.put(x.currency, currency);
        depositCtx.put(x.partyId, partyId);
        depositCtx.put(x.orderId, orderId);
        depositCtx.put(x.orderItemSeqId, orderItemSeqId);
        depositCtx.put(x.amount, deposit);
        depositCtx.put(x.reasonEnumId, x.FATR_IDEPOSIT);
        depositCtx.put(x.userLogin, userLogin);

        Map<String, Object> depositResp;
        try {
            depositResp = dispatcher.runSync(x.finAccountDeposit, depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(depositResp), MODULE);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depositResp));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.finAccountId, finAccountId);
        return result;
    }
}
