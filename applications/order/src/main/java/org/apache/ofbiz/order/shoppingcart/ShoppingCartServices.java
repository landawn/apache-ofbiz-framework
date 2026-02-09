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
package org.apache.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo.CartShipItemInfo;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Shopping Cart Services
 */
public class ShoppingCartServices {

    private static final MathContext GEN_ROUNDING = new MathContext(10);
    private static final String MODULE = ShoppingCartServices.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";

    public static Map<String, Object> assignItemShipGroup(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        Integer fromGroupIndex = (Integer) context.get(org.apache.ofbiz.persistence.entity.x.fromGroupIndex);
        Integer toGroupIndex = (Integer) context.get(org.apache.ofbiz.persistence.entity.x.toGroupIndex);
        Integer itemIndex = (Integer) context.get(org.apache.ofbiz.persistence.entity.x.itemIndex);
        BigDecimal quantity = (BigDecimal) context.get(org.apache.ofbiz.persistence.entity.x.quantity);
        Boolean clearEmptyGroups = (Boolean) context.get(org.apache.ofbiz.persistence.entity.x.clearEmptyGroups);

        if (clearEmptyGroups == null) {
            clearEmptyGroups = Boolean.TRUE;
        }

        Debug.logInfo("From Group - " + fromGroupIndex + " To Group - " + toGroupIndex + "Item - " + itemIndex + "(" + quantity + ")", MODULE);
        if (fromGroupIndex.equals(toGroupIndex)) {
            // nothing to do
            return ServiceUtil.returnSuccess();
        }

        cart.positionItemToGroup(itemIndex, quantity,
                fromGroupIndex, toGroupIndex, clearEmptyGroups);
        Debug.logInfo("Called cart.positionItemToGroup()", MODULE);

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> setShippingOptions(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        Integer groupIndex = (Integer) context.get(org.apache.ofbiz.persistence.entity.x.groupIndex);
        String shippingContactMechId = (String) context.get(org.apache.ofbiz.persistence.entity.x.shippingContactMechId);
        String shipmentMethodString = (String) context.get(org.apache.ofbiz.persistence.entity.x.shipmentMethodString);
        String shippingInstructions = (String) context.get(org.apache.ofbiz.persistence.entity.x.shippingInstructions);
        String giftMessage = (String) context.get(org.apache.ofbiz.persistence.entity.x.giftMessage);
        Boolean maySplit = (Boolean) context.get(org.apache.ofbiz.persistence.entity.x.maySplit);
        Boolean isGift = (Boolean) context.get(org.apache.ofbiz.persistence.entity.x.isGift);
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        ShoppingCart.CartShipInfo csi = cart.getShipInfo(groupIndex);
        if (csi != null) {
            int idx = groupIndex;

            if (UtilValidate.isNotEmpty(shipmentMethodString)) {
                int delimiterPos = shipmentMethodString.indexOf('@');
                String shipmentMethodTypeId = null;
                String carrierPartyId = null;

                if (delimiterPos > 0) {
                    shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                    carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                }

                cart.setShipmentMethodTypeId(idx, shipmentMethodTypeId);
                cart.setCarrierPartyId(idx, carrierPartyId);
            }

            cart.setShippingInstructions(idx, shippingInstructions);
            cart.setShippingContactMechId(idx, shippingContactMechId);
            cart.setGiftMessage(idx, giftMessage);

            if (maySplit != null) {
                cart.setMaySplit(idx, maySplit);
            }
            if (isGift != null) {
                cart.setIsGift(idx, isGift);
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderCartShipGroupNotFound", UtilMisc.toMap("groupIndex",
                    groupIndex), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> setPaymentOptions(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderServiceNotYetImplemented", locale));
    }

    public static Map<String, Object> setOtherOptions(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        String orderAdditionalEmails = (String) context.get(org.apache.ofbiz.persistence.entity.x.orderAdditionalEmails);
        String correspondingPoId = (String) context.get(org.apache.ofbiz.persistence.entity.x.correspondingPoId);

        cart.setOrderAdditionalEmails(orderAdditionalEmails);
        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            cart.setPoNumber(correspondingPoId);
        } else {
            cart.setPoNumber(null);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> loadCartFromOrder(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.userLogin);
        String orderId = (String) context.get(org.apache.ofbiz.persistence.entity.x.orderId);
        Boolean skipInventoryChecks = (Boolean) context.get(org.apache.ofbiz.persistence.entity.x.skipInventoryChecks);
        Boolean skipProductChecks = (Boolean) context.get(org.apache.ofbiz.persistence.entity.x.skipProductChecks);
        boolean includePromoItems = Boolean.TRUE.equals(context.get(org.apache.ofbiz.persistence.entity.x.includePromoItems));
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);
        //FIXME: deepak:Personally I don't like the idea of passing flag but for orderItem quantity calculation we need this flag.
        String createAsNewOrder = (String) context.get(org.apache.ofbiz.persistence.entity.x.createAsNewOrder);
        List<GenericValue> orderTerms = null;
        List<GenericValue> orderContactMechs = null;

        if (UtilValidate.isEmpty(skipInventoryChecks)) {
            skipInventoryChecks = Boolean.FALSE;
        }
        if (UtilValidate.isEmpty(skipProductChecks)) {
            skipProductChecks = Boolean.FALSE;
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            orderTerms = orderHeader.getRelated(org.apache.ofbiz.persistence.entity.x.OrderTerm, null, null, false);
            orderContactMechs = EntityQuery.use(delegator).select("orderId", "contactMechId", "contactMechPurposeTypeId").from(
                    "OrderAndPartyContactMech").where("orderId", orderId).filterByDate("contactFromDate", "contactThruDate").distinct().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial require cart info
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String productStoreId = orh.getProductStoreId();
        String orderTypeId = orh.getOrderTypeId();
        String currency = orh.getCurrency();
        String website = orh.getWebSiteId();
        String currentStatusString = orh.getCurrentStatusString();

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, website, locale, currency);
        cart.setDoPromotions(!includePromoItems);
        cart.setOrderType(orderTypeId);
        cart.setChannelType(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.salesChannelEnumId));
        cart.setInternalCode(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.internalCode));
        if ("Y".equals(createAsNewOrder)) {
            cart.setOrderDate(UtilDateTime.nowTimestamp());
        } else {
            cart.setOrderDate(orderHeader.getTimestamp(org.apache.ofbiz.persistence.entity.x.orderDate));
        }
        cart.setOrderId(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.orderId));
        cart.setOrderName(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.orderName));
        cart.setOrderStatusId(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.statusId));
        cart.setOrderStatusString(currentStatusString);
        cart.setFacilityId(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.originFacilityId));
        cart.setAgreementId(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.agreementId));

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the role information
        GenericValue placingParty = orh.getPlacingParty();
        if (placingParty != null) {
            cart.setPlacingCustomerPartyId(placingParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        GenericValue billFromParty = orh.getBillFromParty();
        if (billFromParty != null) {
            cart.setBillFromVendorPartyId(billFromParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        GenericValue billToParty = orh.getBillToParty();
        if (billToParty != null) {
            cart.setBillToCustomerPartyId(billToParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        GenericValue shipToParty = orh.getShipToParty();
        if (shipToParty != null) {
            cart.setShipToCustomerPartyId(shipToParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        GenericValue endUserParty = orh.getEndUserParty();
        if (endUserParty != null) {
            cart.setEndUserCustomerPartyId(endUserParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
            cart.setOrderPartyId(endUserParty.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        // load order attributes
        List<GenericValue> orderAttributesList = null;
        try {
            orderAttributesList = EntityQuery.use(delegator).from("OrderAttribute").where("orderId", orderId).queryList();
            if (UtilValidate.isNotEmpty(orderAttributesList)) {
                for (GenericValue orderAttr : orderAttributesList) {
                    String name = orderAttr.getString(org.apache.ofbiz.persistence.entity.x.attrName);
                    String value = orderAttr.getString(org.apache.ofbiz.persistence.entity.x.attrValue);
                    cart.setOrderAttribute(name, value);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // load the payment infos
        List<GenericValue> orderPaymentPrefs = null;
        try {
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"));
            orderPaymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where(exprs).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isNotEmpty(orderPaymentPrefs)) {
            Iterator<GenericValue> oppi = orderPaymentPrefs.iterator();
            while (oppi.hasNext()) {
                GenericValue opp = oppi.next();
                String paymentId = opp.getString(org.apache.ofbiz.persistence.entity.x.paymentMethodId);
                if (paymentId == null) {
                    paymentId = opp.getString(org.apache.ofbiz.persistence.entity.x.paymentMethodTypeId);
                }
                BigDecimal maxAmount = opp.getBigDecimal(org.apache.ofbiz.persistence.entity.x.maxAmount);
                String overflow = opp.getString(org.apache.ofbiz.persistence.entity.x.overflowFlag);

                ShoppingCart.CartPaymentInfo cpi = null;

                if ((overflow == null || !"Y".equals(overflow)) && oppi.hasNext()) {
                    cpi = cart.addPaymentAmount(paymentId, maxAmount);
                    Debug.logInfo("Added Payment: " + paymentId + " / " + maxAmount, MODULE);
                } else {
                    cpi = cart.addPayment(paymentId);
                    Debug.logInfo("Added Payment: " + paymentId + " / [no max]", MODULE);
                }
                // for finance account the finAccountId needs to be set
                if ("FIN_ACCOUNT".equals(paymentId)) {
                    cpi.setFinAccountId(opp.getString(org.apache.ofbiz.persistence.entity.x.finAccountId));
                }
                // set the billing account and amount
                cart.setBillingAccount(orderHeader.getString(org.apache.ofbiz.persistence.entity.x.billingAccountId), orh.getBillingAccountMaxAmount());
            }
        } else {
            Debug.logInfo("No payment preferences found for order #" + orderId, MODULE);
        }
        // set the order term
        if (UtilValidate.isNotEmpty(orderTerms)) {
            for (GenericValue orderTerm : orderTerms) {
                BigDecimal termValue = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(orderTerm.getString(org.apache.ofbiz.persistence.entity.x.termValue))) {
                    termValue = new BigDecimal(orderTerm.getString(org.apache.ofbiz.persistence.entity.x.termValue));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(orderTerm.getString(org.apache.ofbiz.persistence.entity.x.termDays))) {
                    termDays = Long.parseLong(orderTerm.getString(org.apache.ofbiz.persistence.entity.x.termDays).trim());
                }
                String orderItemSeqId = orderTerm.getString(org.apache.ofbiz.persistence.entity.x.orderItemSeqId);
                cart.addOrderTerm(orderTerm.getString(org.apache.ofbiz.persistence.entity.x.termTypeId), orderItemSeqId, termValue, termDays, orderTerm.getString(org.apache.ofbiz.persistence.entity.x.textValue),
                        orderTerm.getString(org.apache.ofbiz.persistence.entity.x.description));
            }
        }
        if (UtilValidate.isNotEmpty(orderContactMechs)) {
            for (GenericValue orderContactMech : orderContactMechs) {
                cart.addContactMechId(orderContactMech.getString(org.apache.ofbiz.persistence.entity.x.contactMechPurposeTypeId), orderContactMech.getString(org.apache.ofbiz.persistence.entity.x.contactMechId));
            }
        }
        List<GenericValue> orderItemShipGroupList = orh.getOrderItemShipGroups();
        for (GenericValue orderItemShipGroup : orderItemShipGroupList) {
            // should be sorted by shipGroupSeqId
            int groupIdx = Integer.parseInt(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.shipGroupSeqId));
            CartShipInfo cartShipInfo = cart.getShipInfo(groupIdx - 1);
            if (cartShipInfo == null) {
                cartShipInfo = cart.getShipInfo(cart.addShipInfo());
            }
            cartShipInfo.setShipAfterDate(orderItemShipGroup.getTimestamp(org.apache.ofbiz.persistence.entity.x.shipAfterDate));
            cartShipInfo.setShipBeforeDate(orderItemShipGroup.getTimestamp(org.apache.ofbiz.persistence.entity.x.shipByDate));
            cartShipInfo.setShipmentMethodTypeId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.shipmentMethodTypeId));
            cartShipInfo.setCarrierPartyId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.carrierPartyId));
            cartShipInfo.setSupplierPartyId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.supplierPartyId));
            cartShipInfo.setMaySplit(orderItemShipGroup.getBoolean(org.apache.ofbiz.persistence.entity.x.maySplit));
            cartShipInfo.setGiftMessage(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.giftMessage));
            cartShipInfo.setContactMechId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.contactMechId));
            cartShipInfo.setShippingInstructions(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.shippingInstructions));
            cartShipInfo.setFacilityId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.facilityId));
            cartShipInfo.setVendorPartyId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.vendorPartyId));
            cartShipInfo.setShipGroupSeqId(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.shipGroupSeqId));
            cartShipInfo.addShipTaxAdj(orh.getOrderHeaderAdjustmentsTax(orderItemShipGroup.getString(org.apache.ofbiz.persistence.entity.x.shipGroupSeqId)));
        }

        List<GenericValue> orderItems = orh.getOrderItems();
        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(orderItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue item : orderItems) {
                // get the next item sequence id
                String orderItemSeqId = item.getString(org.apache.ofbiz.persistence.entity.x.orderItemSeqId);
                Matcher pmatcher = pattern.matcher(orderItemSeqId);
                orderItemSeqId = pmatcher.replaceAll("");
                // get product Id
                String productId = item.getString(org.apache.ofbiz.persistence.entity.x.productId);
                GenericValue product = null;
                // creates survey responses for Gift cards same as last Order created
                Map<String, Object> surveyResponseResult = null;
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if ("ITEM_REJECTED".equals(item.getString(org.apache.ofbiz.persistence.entity.x.statusId)) || "ITEM_CANCELLED".equals(item.getString(org.apache.ofbiz.persistence.entity.x.statusId))) {
                    continue;
                }
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    if ("DIGITAL_GOOD".equals(product.getString(org.apache.ofbiz.persistence.entity.x.productTypeId))) {
                        Map<String, Object> surveyResponseMap = new HashMap<>();
                        Map<String, Object> answers = new HashMap<>();
                        List<GenericValue> surveyResponseAndAnswers = EntityQuery.use(delegator).from("SurveyResponseAndAnswer").where("orderId",
                                orderId, "orderItemSeqId", orderItemSeqId).queryList();
                        if (UtilValidate.isNotEmpty(surveyResponseAndAnswers)) {
                            String surveyId = EntityUtil.getFirst(surveyResponseAndAnswers).getString("surveyId");
                            for (GenericValue surveyResponseAndAnswer : surveyResponseAndAnswers) {
                                answers.put((surveyResponseAndAnswer.get(org.apache.ofbiz.persistence.entity.x.surveyQuestionId).toString()),
                                        surveyResponseAndAnswer.get(org.apache.ofbiz.persistence.entity.x.textResponse));
                            }
                            surveyResponseMap.put("answers", answers);
                            surveyResponseMap.put("surveyId", surveyId);
                            surveyResponseResult = dispatcher.runSync("createSurveyResponse", surveyResponseMap);
                            if (ServiceUtil.isError(surveyResponseResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(surveyResponseResult));
                            }
                        }
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // do not include PROMO items
                if (!includePromoItems && item.get(org.apache.ofbiz.persistence.entity.x.isPromo) != null && "Y".equals(item.getString(org.apache.ofbiz.persistence.entity.x.isPromo))) {
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = item.getBigDecimal(org.apache.ofbiz.persistence.entity.x.selectedAmount);
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                //BigDecimal quantity = item.getBigDecimal("quantity");
                BigDecimal quantity = BigDecimal.ZERO;
                if ("ITEM_COMPLETED".equals(item.getString(org.apache.ofbiz.persistence.entity.x.statusId)) && "N".equals(createAsNewOrder)) {
                    quantity = item.getBigDecimal(org.apache.ofbiz.persistence.entity.x.quantity);
                } else {
                    quantity = OrderReadHelper.getOrderItemQuantity(item);
                }
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }

                BigDecimal unitPrice = null;
                if ("Y".equals(item.getString(org.apache.ofbiz.persistence.entity.x.isModifiedPrice))) {
                    unitPrice = item.getBigDecimal(org.apache.ofbiz.persistence.entity.x.unitPrice);
                }

                int itemIndex = -1;
                if (item.get(org.apache.ofbiz.persistence.entity.x.productId) == null) {
                    // non-product item
                    String itemType = item.getString(org.apache.ofbiz.persistence.entity.x.orderItemTypeId);
                    String desc = item.getString(org.apache.ofbiz.persistence.entity.x.itemDescription);
                    try {
                        // TODO: passing in null now for itemGroupNumber, but should reproduce from OrderItemGroup records
                        itemIndex = cart.addNonProductItem(itemType, desc, null, unitPrice, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String prodCatalogId = item.getString(org.apache.ofbiz.persistence.entity.x.prodCatalogId);

                    //prepare the rental data
                    Timestamp reservStart = null;
                    BigDecimal reservLength = null;
                    BigDecimal reservPersons = null;
                    String accommodationMapId = null;
                    String accommodationSpotId = null;

                    GenericValue workEffort = null;
                    String workEffortId = orh.getCurrentOrderItemWorkEffort(item);
                    if (workEffortId != null) {
                        try {
                            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                    if (workEffort != null && "ASSET_USAGE".equals(workEffort.getString(org.apache.ofbiz.persistence.entity.x.workEffortTypeId))) {
                        reservStart = workEffort.getTimestamp(org.apache.ofbiz.persistence.entity.x.estimatedStartDate);
                        reservLength = OrderReadHelper.getWorkEffortRentalLength(workEffort);
                        reservPersons = workEffort.getBigDecimal(org.apache.ofbiz.persistence.entity.x.reservPersons);
                        accommodationMapId = workEffort.getString(org.apache.ofbiz.persistence.entity.x.accommodationMapId);
                        accommodationSpotId = workEffort.getString(org.apache.ofbiz.persistence.entity.x.accommodationSpotId);

                    }    //end of rental data

                    //check for AGGREGATED products
                    ProductConfigWrapper configWrapper = null;
                    String configId = null;
                    try {
                        product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString(org.apache.ofbiz.persistence.entity.x.productTypeId),
                                "parentTypeId", "AGGREGATED")) {
                            GenericValue productAssoc = EntityQuery.use(delegator).from("ProductAssoc")
                                    .where("productAssocTypeId", "PRODUCT_CONF", "productIdTo", product.getString(org.apache.ofbiz.persistence.entity.x.productId))
                                    .filterByDate()
                                    .queryFirst();
                            if (productAssoc != null) {
                                productId = productAssoc.getString(org.apache.ofbiz.persistence.entity.x.productId);
                                configId = product.getString(org.apache.ofbiz.persistence.entity.x.configId);
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }

                    if (UtilValidate.isNotEmpty(configId)) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId, productStoreId,
                                prodCatalogId, website, currency, locale, userLogin);
                    }
                    try {
                        itemIndex = cart.addItemToEnd(productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons,
                                accommodationMapId, accommodationSpotId, null, null, prodCatalogId, configWrapper,
                                item.getString(org.apache.ofbiz.persistence.entity.x.orderItemTypeId), dispatcher, null, unitPrice == null ? null : false, skipInventoryChecks,
                                skipProductChecks);
                    } catch (ItemNotFoundException | CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setIsPromo(item.get(org.apache.ofbiz.persistence.entity.x.isPromo) != null && "Y".equals(item.getString(org.apache.ofbiz.persistence.entity.x.isPromo)));
                cartItem.setOrderItemSeqId(item.getString(org.apache.ofbiz.persistence.entity.x.orderItemSeqId));

                try {
                    cartItem.setItemGroup(cart.addItemGroup(item.getRelatedOne(org.apache.ofbiz.persistence.entity.x.OrderItemGroup, true)));
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                // attach surveyResponseId for each item
                if (UtilValidate.isNotEmpty(surveyResponseResult)) {
                    cartItem.setAttribute("surveyResponses", UtilMisc.toList(surveyResponseResult.get("surveyResponseId")));
                }
                // attach addition item information
                cartItem.setStatusId(item.getString(org.apache.ofbiz.persistence.entity.x.statusId));
                cartItem.setItemType(item.getString(org.apache.ofbiz.persistence.entity.x.orderItemTypeId));
                cartItem.setItemComment(item.getString(org.apache.ofbiz.persistence.entity.x.comments));
                cartItem.setQuoteId(item.getString(org.apache.ofbiz.persistence.entity.x.quoteId));
                cartItem.setQuoteItemSeqId(item.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId));
                cartItem.setProductCategoryId(item.getString(org.apache.ofbiz.persistence.entity.x.productCategoryId));
                cartItem.setDesiredDeliveryDate(item.getTimestamp(org.apache.ofbiz.persistence.entity.x.estimatedDeliveryDate));
                cartItem.setShipBeforeDate(item.getTimestamp(org.apache.ofbiz.persistence.entity.x.shipBeforeDate));
                cartItem.setShipAfterDate(item.getTimestamp(org.apache.ofbiz.persistence.entity.x.shipAfterDate));
                cartItem.setReserveAfterDate(item.getTimestamp(org.apache.ofbiz.persistence.entity.x.reserveAfterDate));
                cartItem.setShoppingList(item.getString(org.apache.ofbiz.persistence.entity.x.shoppingListId), item.getString(org.apache.ofbiz.persistence.entity.x.shoppingListItemSeqId));
                cartItem.setIsModifiedPrice("Y".equals(item.getString(org.apache.ofbiz.persistence.entity.x.isModifiedPrice)));
                cartItem.setName(item.getString(org.apache.ofbiz.persistence.entity.x.itemDescription));
                cartItem.setExternalId(item.getString(org.apache.ofbiz.persistence.entity.x.externalId));
                cartItem.setListPrice(item.getBigDecimal(org.apache.ofbiz.persistence.entity.x.unitListPrice));
                cartItem.setSupplierProductId(item.getString(org.apache.ofbiz.persistence.entity.x.supplierProductId));

                // load order item attributes
                List<GenericValue> orderItemAttributesList = null;
                try {
                    orderItemAttributesList = EntityQuery.use(delegator).from("OrderItemAttribute").where("orderId", orderId, "orderItemSeqId",
                            orderItemSeqId).queryList();
                    if (UtilValidate.isNotEmpty(orderItemAttributesList)) {
                        for (GenericValue orderItemAttr : orderItemAttributesList) {
                            String name = orderItemAttr.getString(org.apache.ofbiz.persistence.entity.x.attrName);
                            String value = orderItemAttr.getString(org.apache.ofbiz.persistence.entity.x.attrValue);
                            cartItem.setOrderItemAttribute(name, value);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // load order item contact mechs
                List<GenericValue> orderItemContactMechList = null;
                try {
                    orderItemContactMechList = EntityQuery.use(delegator).from("OrderItemContactMech").where("orderId", orderId, "orderItemSeqId",
                            orderItemSeqId).queryList();
                    if (UtilValidate.isNotEmpty(orderItemContactMechList)) {
                        for (GenericValue orderItemContactMech : orderItemContactMechList) {
                            String contactMechPurposeTypeId = orderItemContactMech.getString(org.apache.ofbiz.persistence.entity.x.contactMechPurposeTypeId);
                            String contactMechId = orderItemContactMech.getString(org.apache.ofbiz.persistence.entity.x.contactMechId);
                            cartItem.addContactMech(contactMechPurposeTypeId, contactMechId);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // set the PO number on the cart
                cart.setPoNumber(item.getString(org.apache.ofbiz.persistence.entity.x.correspondingPoId));

                // get all item adjustments EXCEPT tax and promo adjustments that will be recalculate
                List<GenericValue> itemAdjustments = orh.getOrderItemAdjustments(item);
                if (itemAdjustments != null) {
                    for (GenericValue itemAdjustment : itemAdjustments) {
                        if (!isTaxAdjustment(itemAdjustment) && !isPromoAdjustment(itemAdjustment)) {
                            cartItem.addAdjustment(itemAdjustment);
                        }
                    }
                }
            }

            // setup the OrderItemShipGroupAssoc records
            if (UtilValidate.isNotEmpty(orderItems)) {
                int itemIndex = 0;
                for (GenericValue item : orderItems) {
                    // if rejected or cancelled ignore, just like above otherwise all indexes will be off by one!
                    if ("ITEM_REJECTED".equals(item.getString(org.apache.ofbiz.persistence.entity.x.statusId)) || "ITEM_CANCELLED".equals(item.getString(org.apache.ofbiz.persistence.entity.x.statusId))) {
                        continue;
                    }

                    List<GenericValue> orderItemAdjustments = orh.getOrderItemAdjustments(item);
                    // set the item's ship group info
                    List<GenericValue> shipGroupAssocs = orh.getOrderItemShipGroupAssocs(item);
                    if (UtilValidate.isNotEmpty(shipGroupAssocs)) {
                        shipGroupAssocs = EntityUtil.orderBy(shipGroupAssocs, UtilMisc.toList("-shipGroupSeqId"));
                    }
                    for (int g = 0; g < shipGroupAssocs.size(); g++) {
                        GenericValue sgAssoc = shipGroupAssocs.get(g);
                        BigDecimal shipGroupQty = OrderReadHelper.getOrderItemShipGroupQuantity(sgAssoc);
                        if (shipGroupQty == null) {
                            shipGroupQty = BigDecimal.ZERO;
                        }

                        String cartShipGroupIndexStr = sgAssoc.getString(org.apache.ofbiz.persistence.entity.x.shipGroupSeqId);
                        int cartShipGroupIndex = cart.getShipInfoIndex(cartShipGroupIndexStr);
                        if (cartShipGroupIndex > 0) {
                            cart.positionItemToGroup(itemIndex, shipGroupQty, 0, cartShipGroupIndex, false);
                        }

                        // because the ship groups are setup before loading items, and the ShoppingCart.addItemToEnd
                        // method is called when loading items above and it calls ShoppingCart.setItemShipGroupQty,
                        // this may not be necessary here, so check it first as calling it here with 0 quantity and
                        // such ends up removing cart items from the group, which causes problems later with inventory
                        // reservation, tax calculation, etc.
                        ShoppingCart.CartShipInfo csi = cart.getShipInfo(cartShipGroupIndex);
                        ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                        if (cartItem == null || cartItem.getQuantity() == null
                                || BigDecimal.ZERO.equals(cartItem.getQuantity())
                                || shipGroupQty.equals(cartItem.getQuantity())) {
                            Debug.logInfo("In loadCartFromOrder not adding item [" + item.getString(org.apache.ofbiz.persistence.entity.x.orderItemSeqId)
                                    + "] to ship group with index [" + itemIndex + "]; group quantity is [" + shipGroupQty
                                    + "] item quantity is [" + (cartItem != null ? cartItem.getQuantity() : "no cart item")
                                    + "] cartShipGroupIndex is [" + cartShipGroupIndex + "], csi.shipItemInfo.size(): "
                                    + (cartShipGroupIndex < 0 ? 0 : csi.getShipItemInfo().size()), MODULE);
                        } else {
                            cart.setItemShipGroupQty(itemIndex, shipGroupQty, cartShipGroupIndex);
                        }

                        List<GenericValue> shipGroupItemAdjustments = EntityUtil.filterByAnd(orderItemAdjustments, UtilMisc.toMap("shipGroupSeqId",
                                cartShipGroupIndexStr));
                        if (cartItem == null || cartShipGroupIndex < 0) {
                            Debug.logWarning("In loadCartFromOrder could not find cart item for itemIndex=" + itemIndex + ", for orderId="
                                    + orderId, MODULE);
                        } else {
                            CartShipItemInfo cartShipItemInfo = csi.getShipItemInfo(cartItem);
                            if (cartShipItemInfo == null) {
                                Debug.logWarning("In loadCartFromOrder could not find CartShipItemInfo for itemIndex=" + itemIndex + ", for "
                                        + "orderId=" + orderId, MODULE);
                            } else {
                                List<GenericValue> itemTaxAdj = cartShipItemInfo.getItemTaxAdj();
                                for (GenericValue shipGroupItemAdjustment : shipGroupItemAdjustments) {
                                    if (isTaxAdjustment(shipGroupItemAdjustment)) {
                                        itemTaxAdj.add(shipGroupItemAdjustment);
                                    }
                                }
                            }
                        }
                    }
                    itemIndex++;
                }
            }

            // set the item seq in the cart
            if (nextItemSeq > 0) {
                try {
                    cart.setNextItemSeq(nextItemSeq + 1);
                } catch (GeneralException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        if (includePromoItems) {
            for (String productPromoCode : orh.getProductPromoCodesEntered()) {
                cart.addProductPromoCode(productPromoCode, dispatcher);
            }
            for (GenericValue productPromoUse : orh.getProductPromoUse()) {
                cart.addProductPromoUse(productPromoUse.getString(org.apache.ofbiz.persistence.entity.x.productPromoId), productPromoUse.getString(org.apache.ofbiz.persistence.entity.x.productPromoCodeId),
                        productPromoUse.getBigDecimal(org.apache.ofbiz.persistence.entity.x.totalDiscountAmount), productPromoUse.getBigDecimal(org.apache.ofbiz.persistence.entity.x.quantityLeftInActions),
                        new HashMap<ShoppingCartItem, BigDecimal>());
            }
        }

        List<GenericValue> adjustments = orh.getOrderHeaderAdjustments();
        // If applyQuoteAdjustments is set to false then standard cart adjustments are used.
        if (!adjustments.isEmpty()) {
            // The cart adjustments are added to the cart
            cart.getAdjustments().addAll(adjustments);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    public static Map<String, Object> loadCartFromQuote(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.userLogin);
        String quoteId = (String) context.get(org.apache.ofbiz.persistence.entity.x.quoteId);
        String applyQuoteAdjustmentsString = (String) context.get(org.apache.ofbiz.persistence.entity.x.applyQuoteAdjustments);
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        boolean applyQuoteAdjustments = applyQuoteAdjustmentsString == null || "true".equals(applyQuoteAdjustmentsString);

        // get the quote header
        GenericValue quote = null;
        try {
            quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial require cart info
        String productStoreId = quote.getString(org.apache.ofbiz.persistence.entity.x.productStoreId);
        String currency = quote.getString(org.apache.ofbiz.persistence.entity.x.currencyUomId);

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currency);
        // set shopping cart type
        if ("PURCHASE_QUOTE".equals(quote.getString(org.apache.ofbiz.persistence.entity.x.quoteTypeId))) {
            cart.setOrderType("PURCHASE_ORDER");
            cart.setBillFromVendorPartyId(quote.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        cart.setQuoteId(quoteId);
        cart.setOrderName(quote.getString(org.apache.ofbiz.persistence.entity.x.quoteName));
        cart.setChannelType(quote.getString(org.apache.ofbiz.persistence.entity.x.salesChannelEnumId));

        List<GenericValue> quoteItems = null;
        List<GenericValue> quoteAdjs = null;
        List<GenericValue> quoteRoles = null;
        List<GenericValue> quoteAttributes = null;
        List<GenericValue> quoteTerms = null;
        try {
            quoteItems = quote.getRelated(org.apache.ofbiz.persistence.entity.x.QuoteItem, null, UtilMisc.toList("quoteItemSeqId"), false);
            quoteAdjs = quote.getRelated(org.apache.ofbiz.persistence.entity.x.QuoteAdjustment, null, null, false);
            quoteRoles = quote.getRelated(org.apache.ofbiz.persistence.entity.x.QuoteRole, null, null, false);
            quoteAttributes = quote.getRelated(org.apache.ofbiz.persistence.entity.x.QuoteAttribute, null, null, false);
            quoteTerms = quote.getRelated(org.apache.ofbiz.persistence.entity.x.QuoteTerm, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        // set the role information
        cart.setOrderPartyId(quote.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        if (UtilValidate.isNotEmpty(quoteRoles)) {
            for (GenericValue quoteRole : quoteRoles) {
                String quoteRoleTypeId = quoteRole.getString(org.apache.ofbiz.persistence.entity.x.roleTypeId);
                String quoteRolePartyId = quoteRole.getString(org.apache.ofbiz.persistence.entity.x.partyId);
                if ("PLACING_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setPlacingCustomerPartyId(quoteRolePartyId);
                } else if ("BILL_TO_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setBillToCustomerPartyId(quoteRolePartyId);
                } else if ("SHIP_TO_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setShipToCustomerPartyId(quoteRolePartyId);
                } else if ("END_USER_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setEndUserCustomerPartyId(quoteRolePartyId);
                } else if ("BILL_FROM_VENDOR".equals(quoteRoleTypeId)) {
                    cart.setBillFromVendorPartyId(quoteRolePartyId);
                } else {
                    cart.addAdditionalPartyRole(quoteRolePartyId, quoteRoleTypeId);
                }
            }
        }

        // set the order term
        if (UtilValidate.isNotEmpty(quoteTerms)) {
            // create order term from quote term
            for (GenericValue quoteTerm : quoteTerms) {
                BigDecimal termValue = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.termValue))) {
                    termValue = new BigDecimal(quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.termValue));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.termDays))) {
                    termDays = Long.parseLong(quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.termDays).trim());
                }
                String orderItemSeqId = quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId);
                cart.addOrderTerm(quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.termTypeId), orderItemSeqId, termValue, termDays, quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.textValue),
                        quoteTerm.getString(org.apache.ofbiz.persistence.entity.x.description));
            }
        }

        // set the attribute information
        if (UtilValidate.isNotEmpty(quoteAttributes)) {
            for (GenericValue quoteAttribute : quoteAttributes) {
                cart.setOrderAttribute(quoteAttribute.getString(org.apache.ofbiz.persistence.entity.x.attrName), quoteAttribute.getString(org.apache.ofbiz.persistence.entity.x.attrValue));
            }
        }

        // Convert the quote adjustment to order header adjustments and
        // put them in a map: the key/values pairs are quoteItemSeqId/List of adjs
        Map<String, List<GenericValue>> orderAdjsMap = new HashMap<>();
        for (GenericValue quoteAdj : quoteAdjs) {
            List<GenericValue> orderAdjs = orderAdjsMap.get(UtilValidate.isNotEmpty(quoteAdj.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId)) ? quoteAdj.getString(
                    org.apache.ofbiz.persistence.entity.x.quoteItemSeqId) : quoteId);
            if (orderAdjs == null) {
                orderAdjs = new LinkedList<>();
                orderAdjsMap.put(UtilValidate.isNotEmpty(quoteAdj.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId)) ? quoteAdj.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId) : quoteId,
                        orderAdjs);
            }
            // convert quote adjustments to order adjustments
            GenericValue orderAdj = delegator.makeValue("OrderAdjustment");
            orderAdj.put("orderAdjustmentId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.quoteAdjustmentId));
            orderAdj.put("orderAdjustmentTypeId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.quoteAdjustmentTypeId));
            orderAdj.put("orderItemSeqId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId));
            orderAdj.put("comments", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.comments));
            orderAdj.put("description", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.description));
            orderAdj.put("amount", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.amount));
            orderAdj.put("productPromoId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.productPromoId));
            orderAdj.put("productPromoRuleId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.productPromoRuleId));
            orderAdj.put("productPromoActionSeqId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.productPromoActionSeqId));
            orderAdj.put("productFeatureId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.productFeatureId));
            orderAdj.put("correspondingProductId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.correspondingProductId));
            orderAdj.put("sourceReferenceId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.sourceReferenceId));
            orderAdj.put("sourcePercentage", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.sourcePercentage));
            orderAdj.put("customerReferenceId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.customerReferenceId));
            orderAdj.put("primaryGeoId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.primaryGeoId));
            orderAdj.put("secondaryGeoId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.secondaryGeoId));
            orderAdj.put("exemptAmount", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.exemptAmount));
            orderAdj.put("taxAuthGeoId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.taxAuthGeoId));
            orderAdj.put("taxAuthPartyId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.taxAuthPartyId));
            orderAdj.put("overrideGlAccountId", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.overrideGlAccountId));
            orderAdj.put("includeInTax", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.includeInTax));
            orderAdj.put("includeInShipping", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.includeInShipping));
            orderAdj.put("createdDate", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.createdDate));
            orderAdj.put("createdByUserLogin", quoteAdj.get(org.apache.ofbiz.persistence.entity.x.createdByUserLogin));
            orderAdjs.add(orderAdj);
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(quoteItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue quoteItem : quoteItems) {
                // get the next item sequence id
                String orderItemSeqId = quoteItem.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId);
                Matcher pmatcher = pattern.matcher(orderItemSeqId);
                orderItemSeqId = pmatcher.replaceAll("");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                boolean isPromo = quoteItem.get(org.apache.ofbiz.persistence.entity.x.isPromo) != null && "Y".equals(quoteItem.getString(org.apache.ofbiz.persistence.entity.x.isPromo));
                if (isPromo && !applyQuoteAdjustments) {
                    // do not include PROMO items
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = quoteItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.selectedAmount);
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                BigDecimal quantity = quoteItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.quantity);
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                BigDecimal quoteUnitPrice = quoteItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.quoteUnitPrice);
                if (quoteUnitPrice == null) {
                    quoteUnitPrice = BigDecimal.ZERO;
                }
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    // If, in the quote, an amount is set, we need to
                    // pass to the cart the quoteUnitPrice/amount value.
                    quoteUnitPrice = quoteUnitPrice.divide(amount, GEN_ROUNDING);
                }

                //rental product data
                Timestamp reservStart = quoteItem.getTimestamp(org.apache.ofbiz.persistence.entity.x.reservStart);
                BigDecimal reservLength = quoteItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.reservLength);
                BigDecimal reservPersons = quoteItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.reservPersons);
                int itemIndex = -1;
                if (quoteItem.get(org.apache.ofbiz.persistence.entity.x.productId) == null) {
                    // non-product item
                    String desc = quoteItem.getString(org.apache.ofbiz.persistence.entity.x.comments);
                    try {
                        // note that passing in null for itemGroupNumber as there is no real grouping concept in the quotes right now
                        itemIndex = cart.addNonProductItem(null, desc, null, null, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String productId = quoteItem.getString(org.apache.ofbiz.persistence.entity.x.productId);
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(quoteItem.getString(org.apache.ofbiz.persistence.entity.x.configId))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, quoteItem.getString(org.apache.ofbiz.persistence.entity.x.configId),
                                productId, productStoreId, null, null, currency, locale, userLogin);
                    }
                    try {
                        itemIndex = cart.addItemToEnd(productId, amount, quantity, quoteUnitPrice, reservStart, reservLength, reservPersons, null,
                                null, null, null, null, configWrapper, null, dispatcher, !applyQuoteAdjustments,
                                quoteUnitPrice.compareTo(BigDecimal.ZERO) == 0, Boolean.FALSE, Boolean.FALSE);

                    } catch (ItemNotFoundException | CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setOrderItemSeqId(orderItemSeqId);
                // attach additional item information
                cartItem.setItemComment(quoteItem.getString(org.apache.ofbiz.persistence.entity.x.comments));
                cartItem.setQuoteId(quoteItem.getString(org.apache.ofbiz.persistence.entity.x.quoteId));
                cartItem.setQuoteItemSeqId(quoteItem.getString(org.apache.ofbiz.persistence.entity.x.quoteItemSeqId));
                cartItem.setIsPromo(isPromo);
            }

        }

        // If applyQuoteAdjustments is set to false then standard cart adjustments are used.
        if (applyQuoteAdjustments) {
            // The cart adjustments, derived from quote adjustments, are added to the cart

            // Tax adjustments should be added to the shipping group and shipping group item info
            // Other adjustments like promotional price should be added to the cart independent of
            // the ship group.
            // We're creating the cart right now using data from the quote, so there cannot yet be more than one ship group.

            List<GenericValue> cartAdjs = cart.getAdjustments();
            CartShipInfo shipInfo = cart.getShipInfo(0);

            List<GenericValue> adjs = orderAdjsMap.get(quoteId);

            if (adjs != null) {
                for (GenericValue adj : adjs) {
                    if (isTaxAdjustment(adj)) {
                        shipInfo.addShipTaxAdj(adj);
                    } else {
                        cartAdjs.add(adj);
                    }
                }
            }

            // The cart item adjustments, derived from quote item adjustments, are added to the cart
            if (quoteItems != null) {
                for (ShoppingCartItem item : cart) {
                    String orderItemSeqId = item.getOrderItemSeqId();
                    if (orderItemSeqId != null) {
                        adjs = orderAdjsMap.get(orderItemSeqId);
                    } else {
                        adjs = null;
                    }
                    if (adjs != null) {
                        for (GenericValue adj : adjs) {
                            if (isTaxAdjustment(adj)) {
                                CartShipItemInfo csii = shipInfo.getShipItemInfo(item);

                                if (csii.getItemTaxAdj() == null) {
                                    shipInfo.setItemInfo(item, UtilMisc.toList(adj));
                                } else {
                                    csii.getItemTaxAdj().add(adj);
                                }
                            } else {
                                item.addAdjustment(adj);
                            }
                        }
                    }
                }
            }
        }

        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq + 1);
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    private static boolean isTaxAdjustment(GenericValue cartAdj) {
        String adjType = cartAdj.getString(org.apache.ofbiz.persistence.entity.x.orderAdjustmentTypeId);
        return "SALES_TAX".equals(adjType) || "VAT_TAX".equals(adjType) || "VAT_PRICE_CORRECT".equals(adjType);
    }

    private static boolean isPromoAdjustment(GenericValue cartAdj) {
        String adjType = cartAdj.getString(org.apache.ofbiz.persistence.entity.x.orderAdjustmentTypeId);
        return "PROMOTION_ADJUSTMENT".equals(adjType);
    }

    public static Map<String, Object> loadCartFromShoppingList(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(org.apache.ofbiz.persistence.entity.x.userLogin);
        String shoppingListId = (String) context.get(org.apache.ofbiz.persistence.entity.x.shoppingListId);
        String orderPartyId = (String) context.get(org.apache.ofbiz.persistence.entity.x.orderPartyId);
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);

        // get the shopping list header
        GenericValue shoppingList = null;
        try {
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial required cart info
        String productStoreId = shoppingList.getString(org.apache.ofbiz.persistence.entity.x.productStoreId);
        String currency = shoppingList.getString(org.apache.ofbiz.persistence.entity.x.currencyUom);
        // If no currency has been set in the ShoppingList, use the ProductStore default currency
        if (currency == null) {
            try {
                GenericValue productStore = shoppingList.getRelatedOne(org.apache.ofbiz.persistence.entity.x.ProductStore, false);
                if (productStore != null) {
                    currency = productStore.getString(org.apache.ofbiz.persistence.entity.x.defaultCurrencyUomId);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        // If we still have no currency, use the default from general.properties.  Failing that, use USD
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currency);

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the role information
        if (UtilValidate.isNotEmpty(orderPartyId)) {
            cart.setOrderPartyId(orderPartyId);
        } else {
            cart.setOrderPartyId(shoppingList.getString(org.apache.ofbiz.persistence.entity.x.partyId));
        }

        List<GenericValue> shoppingListItems = null;
        try {
            shoppingListItems = shoppingList.getRelated(org.apache.ofbiz.persistence.entity.x.ShoppingListItem, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(shoppingListItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue shoppingListItem : shoppingListItems) {
                // get the next item sequence id
                String orderItemSeqId = shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.shoppingListItemSeqId);
                Matcher pmatcher = pattern.matcher(orderItemSeqId);
                orderItemSeqId = pmatcher.replaceAll("");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                BigDecimal modifiedPrice = shoppingListItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.modifiedPrice);
                BigDecimal quantity = shoppingListItem.getBigDecimal(org.apache.ofbiz.persistence.entity.x.quantity);
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                int itemIndex = -1;
                if (shoppingListItem.get(org.apache.ofbiz.persistence.entity.x.productId) != null) {
                    // product item
                    String productId = shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.productId);
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.configId))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.configId),
                                productId, productStoreId, null, null, currency, locale, userLogin);
                    }
                    try {
                        itemIndex = cart.addItemToEnd(productId, null, quantity, null, null, null, null, null, configWrapper, dispatcher,
                                Boolean.TRUE, Boolean.TRUE);
                    } catch (ItemNotFoundException | CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    // set the modified price
                    if (modifiedPrice != null && modifiedPrice.doubleValue() != 0) {
                        ShoppingCartItem item = cart.findCartItem(itemIndex);
                        if (item != null) {
                            item.setIsModifiedPrice(true);
                            item.setBasePrice(modifiedPrice);
                        }
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setOrderItemSeqId(orderItemSeqId);
                // attach additional item information
                cartItem.setShoppingList(shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.shoppingListId), shoppingListItem.getString(org.apache.ofbiz.persistence.entity.x.shoppingListItemSeqId));
            }

        }

        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq + 1);
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    public static Map<String, Object> getShoppingCartData(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get(org.apache.ofbiz.persistence.entity.x.locale);
        ShoppingCart shoppingCart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        if (shoppingCart != null) {
            String isoCode = shoppingCart.getCurrency();
            result.put("totalQuantity", shoppingCart.getTotalQuantity());
            result.put("currencyIsoCode", isoCode);
            result.put("subTotal", shoppingCart.getSubTotal());
            result.put("subTotalCurrencyFormatted", org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getSubTotal(), isoCode,
                    locale));
            result.put("totalShipping", shoppingCart.getTotalShipping());
            result.put("totalShippingCurrencyFormatted", org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getTotalShipping(),
                    isoCode, locale));
            result.put("totalSalesTax", shoppingCart.getTotalSalesTax());
            result.put("totalSalesTaxCurrencyFormatted", org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getTotalSalesTax(),
                    isoCode, locale));
            result.put("displayGrandTotal", shoppingCart.getDisplayGrandTotal());
            result.put("displayGrandTotalCurrencyFormatted",
                    org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getDisplayGrandTotal(), isoCode, locale));
            BigDecimal orderAdjustmentsTotal =
                    OrderReadHelper.calcOrderAdjustments(OrderReadHelper.getOrderHeaderAdjustments(shoppingCart.getAdjustments(), null),
                            shoppingCart.getSubTotal(), true, true, true);
            result.put("displayOrderAdjustmentsTotalCurrencyFormatted",
                    org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(orderAdjustmentsTotal, isoCode, locale));
            Map<String, Object> cartItemData = new HashMap<>();
            for (ShoppingCartItem cartLine : shoppingCart) {
                int cartLineIndex = shoppingCart.getItemIndex(cartLine);
                cartItemData.put("displayItemQty_" + cartLineIndex, cartLine.getQuantity());
                cartItemData.put("displayItemPrice_" + cartLineIndex,
                        org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getDisplayPrice(), isoCode, locale));
                cartItemData.put("displayItemSubTotal_" + cartLineIndex, cartLine.getDisplayItemSubTotal());
                cartItemData.put("displayItemSubTotalCurrencyFormatted_" + cartLineIndex,
                        org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getDisplayItemSubTotal(), isoCode, locale));
                cartItemData.put("displayItemAdjustment_" + cartLineIndex,
                        org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getOtherAdjustments(), isoCode, locale));
            }
            result.put("cartItemData", cartItemData);
        }
        return result;
    }

    public static Map<String, Object> getShoppingCartItemIndex(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        ShoppingCart shoppingCart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        String productId = (String) context.get(org.apache.ofbiz.persistence.entity.x.productId);
        if (shoppingCart != null && UtilValidate.isNotEmpty(shoppingCart.items())) {
            List<ShoppingCartItem> items = shoppingCart.findAllCartItems(productId);
            if (!items.isEmpty()) {
                ShoppingCartItem item = items.get(0);
                int itemIndex = shoppingCart.getItemIndex(item);
                result.put("itemIndex", String.valueOf(itemIndex));
            }
        }
        return result;
    }

    public static Map<String, Object> resetShipGroupItems(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        ShoppingCart cart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        for (ShoppingCartItem item : cart) {
            cart.clearItemShipInfo(item);
            cart.setItemShipGroupQty(item, item.getQuantity(), 0);
        }
        return result;
    }

    public static Map<String, Object> prepareVendorShipGroups(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        ShoppingCart cart = (ShoppingCart) context.get(org.apache.ofbiz.persistence.entity.x.shoppingCart);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Map<String, Object> resp = dispatcher.runSync("resetShipGroupItems", context);
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        Map<String, Object> vendorMap = new HashMap<>();
        for (ShoppingCartItem item : cart) {
            GenericValue vendorProduct = null;
            String productId = item.getParentProductId();
            if (productId == null) {
                productId = item.getProductId();
            }
            int index = 0;
            try {
                vendorProduct = EntityQuery.use(delegator).from("VendorProduct").where("productId", productId, "productStoreGroupId", "_NA_")
                        .queryFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e.toString(), MODULE);
            }

            if (UtilValidate.isEmpty(vendorProduct)) {
                if (vendorMap.containsKey("_NA_")) {
                    index = (Integer) vendorMap.get("_NA_");
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                } else {
                    index = cart.addShipInfo();
                    vendorMap.put("_NA_", index);

                    ShoppingCart.CartShipInfo info = cart.getShipInfo(index);
                    info.setVendorPartyId("_NA_");
                    info.setShipGroupSeqId(UtilFormatOut.formatPaddedNumber(index, 5));
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                }
            }
            if (vendorProduct != null) {
                String vendorPartyId = vendorProduct.getString(org.apache.ofbiz.persistence.entity.x.vendorPartyId);
                if (vendorMap.containsKey(vendorPartyId)) {
                    index = (Integer) vendorMap.get(vendorPartyId);
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                } else {
                    index = cart.addShipInfo();
                    vendorMap.put(vendorPartyId, index);

                    ShoppingCart.CartShipInfo info = cart.getShipInfo(index);
                    info.setVendorPartyId(vendorPartyId);
                    info.setShipGroupSeqId(UtilFormatOut.formatPaddedNumber(index, 5));
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                }
            }
        }
        return result;
    }
}
