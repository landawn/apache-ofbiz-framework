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

package org.apache.ofbiz.accounting.agreement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.AgreementItemDao;
import org.apache.ofbiz.persistence.dao.AgreementProductApplDao;
import org.apache.ofbiz.persistence.dao.AgreementTermDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductAssocDao;
import org.apache.ofbiz.persistence.entity.AgreementItemEntity;
import org.apache.ofbiz.persistence.entity.AgreementProductApplEntity;
import org.apache.ofbiz.persistence.entity.AgreementTermEntity;
import org.apache.ofbiz.persistence.entity.ProductAssocEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.AgreementServicesContext;
/**
 * Services for Agreement (Accounting)
 */

public class AgreementServices {

    private static final String MODULE = AgreementServices.class.getName();
    // set some BigDecimal properties
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.finaccount_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.finaccount_rounding);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    /**
     * Determines commission receiving parties and amounts for the provided product, price, and quantity
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     *      productId   String      Product Id
     *      invoiceItemTypeId   String      Invoice Type
     *      amount      BigDecimal  Entire amount
     *      quantity    BigDecimal  Quantity
     * @return Map with the result of the service, the output parameters.
     *      commissions List        List of Maps each containing
     *              partyIdFrom     String  commission paying party
     *              partyIdTo       String  commission receiving party
     *              commission      BigDecimal  Commission
     *              days            Long    term days
     *              currencyUomId   String  Currency
     *              productId       String  Product Id
     */
    public static Map<String, Object> getCommissionForProduct(DispatchContext ctx, AgreementServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String errMsg = null;
        List<Map<String, Object>> commissions = new LinkedList<>();

        try {
            AgreementProductApplDao agreementProductApplDao = DaoRegistry.getDao(delegator, x.AgreementProductAppl, AgreementProductApplDao.class);
            AgreementItemDao agreementItemDao = DaoRegistry.getDao(delegator, x.AgreementItem, AgreementItemDao.class);
            ProductAssocDao productAssocDao = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class);
            AgreementTermDao agreementTermDao = DaoRegistry.getDao(delegator, x.AgreementTerm, AgreementTermDao.class);

            BigDecimal amount = ((BigDecimal) context.get(x.amount));
            BigDecimal quantity = (BigDecimal) context.get(x.quantity);
            quantity = quantity == null ? BigDecimal.ONE : quantity;
            boolean negative = amount.signum() < 0;
            // Ensure that price and quantity are positive since the terms may not be linear.
            amount = amount.abs();
            quantity = quantity.abs();
            String productId = (String) context.get(x.productId);
            String invoiceItemTypeId = (String) context.get(x.invoiceItemTypeId);
            String invoiceItemSeqId = (String) context.get(x.invoiceItemSeqId);
            String invoiceId = (String) context.get(x.invoiceId);

            // Collect agreementItems applicable to this orderItem/returnItem
            // TODO: partyIds should be part of this query!
            List<AgreementProductApplEntity> agreementProductApplEntities = agreementProductApplDao.list(Filters.eq(x.productId, productId));
            List<GenericValue> agreementProductAppls = new LinkedList<>();
            for (AgreementProductApplEntity agreementProductApplEntity : agreementProductApplEntities) {
                agreementProductAppls.add(delegator.makeValue(x.AgreementProductAppl, Beans.beanToMap(agreementProductApplEntity)));
            }
            agreementProductAppls = EntityUtil.filterByDate(agreementProductAppls);

            List<GenericValue> agreementItems = new LinkedList<>();
            for (GenericValue agreementProductAppl : agreementProductAppls) {
                List<AgreementItemEntity> agreementItemEntities = agreementItemDao.list(Filters.and(
                        Filters.eq(x.agreementId, agreementProductAppl.getString(x.agreementId)),
                        Filters.eq(x.agreementItemSeqId, agreementProductAppl.getString(x.agreementItemSeqId)),
                        Filters.eq(x.agreementItemTypeId, x.AGREEMENT_COMMISSION)));

                for (AgreementItemEntity agreementItemEntity : agreementItemEntities) {
                    GenericValue agreementItem = delegator.makeValue(x.AgreementItem, Beans.beanToMap(agreementItemEntity));
                    GenericValue agreementItemAndProductAppl = delegator.makeValue(x.AgreementItemAndProductAppl);
                    agreementItemAndProductAppl.setAllFields(agreementProductAppl, false, null, false);
                    agreementItemAndProductAppl.setAllFields(agreementItem, false, null, false);
                    agreementItems.add(agreementItemAndProductAppl);
                }
            }

            // Try the first available virtual product if this is a variant product
            if (agreementItems.isEmpty()) {
                List<ProductAssocEntity> productAssocEntities = productAssocDao.list(Filters.and(
                        Filters.eq(x.productIdTo, productId),
                        Filters.eq(x.productAssocTypeId, x.PRODUCT_VARIANT)));
                List<GenericValue> productAssocs = new LinkedList<>();
                for (ProductAssocEntity productAssocEntity : productAssocEntities) {
                    productAssocs.add(delegator.makeValue(x.ProductAssoc, Beans.beanToMap(productAssocEntity)));
                }
                productAssocs = EntityUtil.filterByDate(productAssocs);
                GenericValue productAssoc = EntityUtil.getFirst(productAssocs);
                if (productAssoc != null) {
                    agreementProductApplEntities = agreementProductApplDao.list(Filters.eq(x.productId, productAssoc.getString(x.productId)));
                    agreementProductAppls = new LinkedList<>();
                    for (AgreementProductApplEntity agreementProductApplEntity : agreementProductApplEntities) {
                        agreementProductAppls.add(delegator.makeValue(x.AgreementProductAppl, Beans.beanToMap(agreementProductApplEntity)));
                    }
                    agreementProductAppls = EntityUtil.filterByDate(agreementProductAppls);

                    agreementItems = new LinkedList<>();
                    for (GenericValue agreementProductAppl : agreementProductAppls) {
                        List<AgreementItemEntity> agreementItemEntities = agreementItemDao.list(Filters.and(
                                Filters.eq(x.agreementId, agreementProductAppl.getString(x.agreementId)),
                                Filters.eq(x.agreementItemSeqId, agreementProductAppl.getString(x.agreementItemSeqId)),
                                Filters.eq(x.agreementItemTypeId, x.AGREEMENT_COMMISSION)));

                        for (AgreementItemEntity agreementItemEntity : agreementItemEntities) {
                            GenericValue agreementItem = delegator.makeValue(x.AgreementItem, Beans.beanToMap(agreementItemEntity));
                            GenericValue agreementItemAndProductAppl = delegator.makeValue(x.AgreementItemAndProductAppl);
                            agreementItemAndProductAppl.setAllFields(agreementProductAppl, false, null, false);
                            agreementItemAndProductAppl.setAllFields(agreementItem, false, null, false);
                            agreementItems.add(agreementItemAndProductAppl);
                        }
                    }
                }
            }

            for (GenericValue agreementItem : agreementItems) {
                List<AgreementTermEntity> agreementTermEntities = agreementTermDao.list(Filters.and(
                        Filters.eq(x.agreementId, agreementItem.getString(x.agreementId)),
                        Filters.eq(x.agreementItemSeqId, agreementItem.getString(x.agreementItemSeqId)),
                        Filters.eq(x.invoiceItemTypeId, invoiceItemTypeId)));
                List<GenericValue> terms = new LinkedList<>();
                for (AgreementTermEntity agreementTermEntity : agreementTermEntities) {
                    terms.add(delegator.makeValue(x.AgreementTerm, Beans.beanToMap(agreementTermEntity)));
                }
                if (!terms.isEmpty()) {
                    BigDecimal commission = ZERO;
                    BigDecimal min = new BigDecimal(x._1e12);   // Limit to 1 trillion commission
                    BigDecimal max = new BigDecimal(x._1e12_0b4c74b2);

                    // number of days due for commission, which will be the lowest termDays of all the AgreementTerms
                    long days = -1;
                    for (GenericValue term : terms) {
                        String termTypeId = term.getString(x.termTypeId);
                        BigDecimal termValue = term.getBigDecimal(x.termValue);
                        if (termValue != null) {
                            if (x.FIN_COMM_FIXED.equals(termTypeId)) {
                                commission = commission.add(termValue);
                            } else if (x.FIN_COMM_VARIABLE.equals(termTypeId)) {
                                // if variable percentage commission, need to divide by 100, because 5% is stored as termValue of 5.0
                                commission = commission.add(termValue.multiply(amount).divide(new BigDecimal(x._100), 12, ROUNDING));
                            } else if (x.FIN_COMM_MIN.equals(termTypeId)) {
                                min = termValue;
                            } else if (x.FIN_COMM_MAX.equals(termTypeId)) {
                                max = termValue;
                            }
                            // TODO: Add other type of terms and handling here
                        }

                        // see if we need to update the number of days for paying commission
                        Long termDays = term.getLong(x.termDays);
                        if (termDays != null) {
                            // if days is greater than zero, then it has been set with another value, so we use the lowest term days
                            // if days is less than zero, then it has not been set yet.
                            if (days > 0) {
                                days = Math.min(days, termDays);
                            } else {
                                days = termDays;
                            }
                        }
                    }
                    if (commission.compareTo(min) < 0) {
                        commission = min;
                    }
                    if (commission.compareTo(max) > 0) {
                        commission = max;
                    }
                    commission = negative ? commission.negate() : commission;
                    commission = commission.setScale(DECIMALS, ROUNDING);

                    Map<String, Object> partyCommissionResult = UtilMisc.toMap(
                            x.partyIdFrom, agreementItem.getString(x.partyIdFrom),
                            x.partyIdTo, agreementItem.getString(x.partyIdTo),
                            x.invoiceItemSeqId, invoiceItemSeqId,
                            x.invoiceId, invoiceId,
                            x.commission, commission,
                            x.quantity, quantity,
                            x.currencyUomId, agreementItem.getString(x.currencyUomId),
                            x.productId, productId);
                    if (days >= 0) {
                        partyCommissionResult.put(x.days, days);
                    }
                    if (!commissions.contains(partyCommissionResult)) {
                        commissions.add(partyCommissionResult);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(x.CommonUiLabels, x.CommonDatabaseProblem, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        return UtilMisc.toMap(
                x.commissions, commissions,
                ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
    }
}
