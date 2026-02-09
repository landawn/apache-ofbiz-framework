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

import org.apache.ofbiz.persistence.entity.x;
/**
 * Shopping Cart Services
 */
public class ShoppingCartServices {

    private static final MathContext GEN_ROUNDING = new MathContext(10);
    private static final String MODULE = ShoppingCartServices.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";

    public static Map<String, Object> assignItemShipGroup(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get(x.shoppingCart);
        Integer fromGroupIndex = (Integer) context.get(x.fromGroupIndex);
        Integer toGroupIndex = (Integer) context.get(x.toGroupIndex);
        Integer itemIndex = (Integer) context.get(x.itemIndex);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        Boolean clearEmptyGroups = (Boolean) context.get(x.clearEmptyGroups);

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
        ShoppingCart cart = (ShoppingCart) context.get(x.shoppingCart);
        Integer groupIndex = (Integer) context.get(x.groupIndex);
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        String shipmentMethodString = (String) context.get(x.shipmentMethodString);
        String shippingInstructions = (String) context.get(x.shippingInstructions);
        String giftMessage = (String) context.get(x.giftMessage);
        Boolean maySplit = (Boolean) context.get(x.maySplit);
        Boolean isGift = (Boolean) context.get(x.isGift);
        Locale locale = (Locale) context.get(x.locale);

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
        Locale locale = (Locale) context.get(x.locale);

        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderServiceNotYetImplemented", locale));
    }

    public static Map<String, Object> setOtherOptions(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get(x.shoppingCart);
        String orderAdditionalEmails = (String) context.get(x.orderAdditionalEmails);
        String correspondingPoId = (String) context.get(x.correspondingPoId);

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

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderId = (String) context.get(x.orderId);
        Boolean skipInventoryChecks = (Boolean) context.get(x.skipInventoryChecks);
        Boolean skipProductChecks = (Boolean) context.get(x.skipProductChecks);
        boolean includePromoItems = Boolean.TRUE.equals(context.get(x.includePromoItems));
        Locale locale = (Locale) context.get(x.locale);
        //FIXME: deepak:Personally I don't like the idea of passing flag but for orderItem quantity calculation we need this flag.
        String createAsNewOrder = (String) context.get(x.createAsNewOrder);
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
            orderTerms = orderHeader.getRelated(x.OrderTerm, null, null, false);
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
        cart.setChannelType(orderHeader.getString(x.salesChannelEnumId));
        cart.setInternalCode(orderHeader.getString(x.internalCode));
        if ("Y".equals(createAsNewOrder)) {
            cart.setOrderDate(UtilDateTime.nowTimestamp());
        } else {
            cart.setOrderDate(orderHeader.getTimestamp(x.orderDate));
        }
        cart.setOrderId(orderHeader.getString(x.orderId));
        cart.setOrderName(orderHeader.getString(x.orderName));
        cart.setOrderStatusId(orderHeader.getString(x.statusId));
        cart.setOrderStatusString(currentStatusString);
        cart.setFacilityId(orderHeader.getString(x.originFacilityId));
        cart.setAgreementId(orderHeader.getString(x.agreementId));

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the role information
        GenericValue placingParty = orh.getPlacingParty();
        if (placingParty != null) {
            cart.setPlacingCustomerPartyId(placingParty.getString(x.partyId));
        }

        GenericValue billFromParty = orh.getBillFromParty();
        if (billFromParty != null) {
            cart.setBillFromVendorPartyId(billFromParty.getString(x.partyId));
        }

        GenericValue billToParty = orh.getBillToParty();
        if (billToParty != null) {
            cart.setBillToCustomerPartyId(billToParty.getString(x.partyId));
        }

        GenericValue shipToParty = orh.getShipToParty();
        if (shipToParty != null) {
            cart.setShipToCustomerPartyId(shipToParty.getString(x.partyId));
        }

        GenericValue endUserParty = orh.getEndUserParty();
        if (endUserParty != null) {
            cart.setEndUserCustomerPartyId(endUserParty.getString(x.partyId));
            cart.setOrderPartyId(endUserParty.getString(x.partyId));
        }

        // load order attributes
        List<GenericValue> orderAttributesList = null;
        try {
            orderAttributesList = EntityQuery.use(delegator).from("OrderAttribute").where("orderId", orderId).queryList();
            if (UtilValidate.isNotEmpty(orderAttributesList)) {
                for (GenericValue orderAttr : orderAttributesList) {
                    String name = orderAttr.getString(x.attrName);
                    String value = orderAttr.getString(x.attrValue);
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
                String paymentId = opp.getString(x.paymentMethodId);
                if (paymentId == null) {
                    paymentId = opp.getString(x.paymentMethodTypeId);
                }
                BigDecimal maxAmount = opp.getBigDecimal(x.maxAmount);
                String overflow = opp.getString(x.overflowFlag);

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
                    cpi.setFinAccountId(opp.getString(x.finAccountId));
                }
                // set the billing account and amount
                cart.setBillingAccount(orderHeader.getString(x.billingAccountId), orh.getBillingAccountMaxAmount());
            }
        } else {
            Debug.logInfo("No payment preferences found for order #" + orderId, MODULE);
        }
        // set the order term
        if (UtilValidate.isNotEmpty(orderTerms)) {
            for (GenericValue orderTerm : orderTerms) {
                BigDecimal termValue = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(orderTerm.getString(x.termValue))) {
                    termValue = new BigDecimal(orderTerm.getString(x.termValue));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(orderTerm.getString(x.termDays))) {
                    termDays = Long.parseLong(orderTerm.getString(x.termDays).trim());
                }
                String orderItemSeqId = orderTerm.getString(x.orderItemSeqId);
                cart.addOrderTerm(orderTerm.getString(x.termTypeId), orderItemSeqId, termValue, termDays, orderTerm.getString(x.textValue),
                        orderTerm.getString(x.description));
            }
        }
        if (UtilValidate.isNotEmpty(orderContactMechs)) {
            for (GenericValue orderContactMech : orderContactMechs) {
                cart.addContactMechId(orderContactMech.getString(x.contactMechPurposeTypeId), orderContactMech.getString(x.contactMechId));
            }
        }
        List<GenericValue> orderItemShipGroupList = orh.getOrderItemShipGroups();
        for (GenericValue orderItemShipGroup : orderItemShipGroupList) {
            // should be sorted by shipGroupSeqId
            int groupIdx = Integer.parseInt(orderItemShipGroup.getString(x.shipGroupSeqId));
            CartShipInfo cartShipInfo = cart.getShipInfo(groupIdx - 1);
            if (cartShipInfo == null) {
                cartShipInfo = cart.getShipInfo(cart.addShipInfo());
            }
            cartShipInfo.setShipAfterDate(orderItemShipGroup.getTimestamp(x.shipAfterDate));
            cartShipInfo.setShipBeforeDate(orderItemShipGroup.getTimestamp(x.shipByDate));
            cartShipInfo.setShipmentMethodTypeId(orderItemShipGroup.getString(x.shipmentMethodTypeId));
            cartShipInfo.setCarrierPartyId(orderItemShipGroup.getString(x.carrierPartyId));
            cartShipInfo.setSupplierPartyId(orderItemShipGroup.getString(x.supplierPartyId));
            cartShipInfo.setMaySplit(orderItemShipGroup.getBoolean(x.maySplit));
            cartShipInfo.setGiftMessage(orderItemShipGroup.getString(x.giftMessage));
            cartShipInfo.setContactMechId(orderItemShipGroup.getString(x.contactMechId));
            cartShipInfo.setShippingInstructions(orderItemShipGroup.getString(x.shippingInstructions));
            cartShipInfo.setFacilityId(orderItemShipGroup.getString(x.facilityId));
            cartShipInfo.setVendorPartyId(orderItemShipGroup.getString(x.vendorPartyId));
            cartShipInfo.setShipGroupSeqId(orderItemShipGroup.getString(x.shipGroupSeqId));
            cartShipInfo.addShipTaxAdj(orh.getOrderHeaderAdjustmentsTax(orderItemShipGroup.getString(x.shipGroupSeqId)));
        }

        List<GenericValue> orderItems = orh.getOrderItems();
        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(orderItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue item : orderItems) {
                // get the next item sequence id
                String orderItemSeqId = item.getString(x.orderItemSeqId);
                Matcher pmatcher = pattern.matcher(orderItemSeqId);
                orderItemSeqId = pmatcher.replaceAll("");
                // get product Id
                String productId = item.getString(x.productId);
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
                if ("ITEM_REJECTED".equals(item.getString(x.statusId)) || "ITEM_CANCELLED".equals(item.getString(x.statusId))) {
                    continue;
                }
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    if ("DIGITAL_GOOD".equals(product.getString(x.productTypeId))) {
                        Map<String, Object> surveyResponseMap = new HashMap<>();
                        Map<String, Object> answers = new HashMap<>();
                        List<GenericValue> surveyResponseAndAnswers = EntityQuery.use(delegator).from("SurveyResponseAndAnswer").where("orderId",
                                orderId, "orderItemSeqId", orderItemSeqId).queryList();
                        if (UtilValidate.isNotEmpty(surveyResponseAndAnswers)) {
                            String surveyId = EntityUtil.getFirst(surveyResponseAndAnswers).getString("surveyId");
                            for (GenericValue surveyResponseAndAnswer : surveyResponseAndAnswers) {
                                answers.put((surveyResponseAndAnswer.get(x.surveyQuestionId).toString()),
                                        surveyResponseAndAnswer.get(x.textResponse));
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
                if (!includePromoItems && item.get(x.isPromo) != null && "Y".equals(item.getString(x.isPromo))) {
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = item.getBigDecimal(x.selectedAmount);
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                //BigDecimal quantity = item.getBigDecimal("quantity");
                BigDecimal quantity = BigDecimal.ZERO;
                if ("ITEM_COMPLETED".equals(item.getString(x.statusId)) && "N".equals(createAsNewOrder)) {
                    quantity = item.getBigDecimal(x.quantity);
                } else {
                    quantity = OrderReadHelper.getOrderItemQuantity(item);
                }
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }

                BigDecimal unitPrice = null;
                if ("Y".equals(item.getString(x.isModifiedPrice))) {
                    unitPrice = item.getBigDecimal(x.unitPrice);
                }

                int itemIndex = -1;
                if (item.get(x.productId) == null) {
                    // non-product item
                    String itemType = item.getString(x.orderItemTypeId);
                    String desc = item.getString(x.itemDescription);
                    try {
                        // TODO: passing in null now for itemGroupNumber, but should reproduce from OrderItemGroup records
                        itemIndex = cart.addNonProductItem(itemType, desc, null, unitPrice, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String prodCatalogId = item.getString(x.prodCatalogId);

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
                    if (workEffort != null && "ASSET_USAGE".equals(workEffort.getString(x.workEffortTypeId))) {
                        reservStart = workEffort.getTimestamp(x.estimatedStartDate);
                        reservLength = OrderReadHelper.getWorkEffortRentalLength(workEffort);
                        reservPersons = workEffort.getBigDecimal(x.reservPersons);
                        accommodationMapId = workEffort.getString(x.accommodationMapId);
                        accommodationSpotId = workEffort.getString(x.accommodationSpotId);

                    }    //end of rental data

                    //check for AGGREGATED products
                    ProductConfigWrapper configWrapper = null;
                    String configId = null;
                    try {
                        product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString(x.productTypeId),
                                "parentTypeId", "AGGREGATED")) {
                            GenericValue productAssoc = EntityQuery.use(delegator).from("ProductAssoc")
                                    .where("productAssocTypeId", "PRODUCT_CONF", "productIdTo", product.getString(x.productId))
                                    .filterByDate()
                                    .queryFirst();
                            if (productAssoc != null) {
                                productId = productAssoc.getString(x.productId);
                                configId = product.getString(x.configId);
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
                                item.getString(x.orderItemTypeId), dispatcher, null, unitPrice == null ? null : false, skipInventoryChecks,
                                skipProductChecks);
                    } catch (ItemNotFoundException | CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setIsPromo(item.get(x.isPromo) != null && "Y".equals(item.getString(x.isPromo)));
                cartItem.setOrderItemSeqId(item.getString(x.orderItemSeqId));

                try {
                    cartItem.setItemGroup(cart.addItemGroup(item.getRelatedOne(x.OrderItemGroup, true)));
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                // attach surveyResponseId for each item
                if (UtilValidate.isNotEmpty(surveyResponseResult)) {
                    cartItem.setAttribute("surveyResponses", UtilMisc.toList(surveyResponseResult.get("surveyResponseId")));
                }
                // attach addition item information
                cartItem.setStatusId(item.getString(x.statusId));
                cartItem.setItemType(item.getString(x.orderItemTypeId));
                cartItem.setItemComment(item.getString(x.comments));
                cartItem.setQuoteId(item.getString(x.quoteId));
                cartItem.setQuoteItemSeqId(item.getString(x.quoteItemSeqId));
                cartItem.setProductCategoryId(item.getString(x.productCategoryId));
                cartItem.setDesiredDeliveryDate(item.getTimestamp(x.estimatedDeliveryDate));
                cartItem.setShipBeforeDate(item.getTimestamp(x.shipBeforeDate));
                cartItem.setShipAfterDate(item.getTimestamp(x.shipAfterDate));
                cartItem.setReserveAfterDate(item.getTimestamp(x.reserveAfterDate));
                cartItem.setShoppingList(item.getString(x.shoppingListId), item.getString(x.shoppingListItemSeqId));
                cartItem.setIsModifiedPrice("Y".equals(item.getString(x.isModifiedPrice)));
                cartItem.setName(item.getString(x.itemDescription));
                cartItem.setExternalId(item.getString(x.externalId));
                cartItem.setListPrice(item.getBigDecimal(x.unitListPrice));
                cartItem.setSupplierProductId(item.getString(x.supplierProductId));

                // load order item attributes
                List<GenericValue> orderItemAttributesList = null;
                try {
                    orderItemAttributesList = EntityQuery.use(delegator).from("OrderItemAttribute").where("orderId", orderId, "orderItemSeqId",
                            orderItemSeqId).queryList();
                    if (UtilValidate.isNotEmpty(orderItemAttributesList)) {
                        for (GenericValue orderItemAttr : orderItemAttributesList) {
                            String name = orderItemAttr.getString(x.attrName);
                            String value = orderItemAttr.getString(x.attrValue);
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
                            String contactMechPurposeTypeId = orderItemContactMech.getString(x.contactMechPurposeTypeId);
                            String contactMechId = orderItemContactMech.getString(x.contactMechId);
                            cartItem.addContactMech(contactMechPurposeTypeId, contactMechId);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // set the PO number on the cart
                cart.setPoNumber(item.getString(x.correspondingPoId));

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
                    if ("ITEM_REJECTED".equals(item.getString(x.statusId)) || "ITEM_CANCELLED".equals(item.getString(x.statusId))) {
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

                        String cartShipGroupIndexStr = sgAssoc.getString(x.shipGroupSeqId);
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
                            Debug.logInfo("In loadCartFromOrder not adding item [" + item.getString(x.orderItemSeqId)
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
                cart.addProductPromoUse(productPromoUse.getString(x.productPromoId), productPromoUse.getString(x.productPromoCodeId),
                        productPromoUse.getBigDecimal(x.totalDiscountAmount), productPromoUse.getBigDecimal(x.quantityLeftInActions),
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

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String quoteId = (String) context.get(x.quoteId);
        String applyQuoteAdjustmentsString = (String) context.get(x.applyQuoteAdjustments);
        Locale locale = (Locale) context.get(x.locale);

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
        String productStoreId = quote.getString(x.productStoreId);
        String currency = quote.getString(x.currencyUomId);

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currency);
        // set shopping cart type
        if ("PURCHASE_QUOTE".equals(quote.getString(x.quoteTypeId))) {
            cart.setOrderType("PURCHASE_ORDER");
            cart.setBillFromVendorPartyId(quote.getString(x.partyId));
        }
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        cart.setQuoteId(quoteId);
        cart.setOrderName(quote.getString(x.quoteName));
        cart.setChannelType(quote.getString(x.salesChannelEnumId));

        List<GenericValue> quoteItems = null;
        List<GenericValue> quoteAdjs = null;
        List<GenericValue> quoteRoles = null;
        List<GenericValue> quoteAttributes = null;
        List<GenericValue> quoteTerms = null;
        try {
            quoteItems = quote.getRelated(x.QuoteItem, null, UtilMisc.toList("quoteItemSeqId"), false);
            quoteAdjs = quote.getRelated(x.QuoteAdjustment, null, null, false);
            quoteRoles = quote.getRelated(x.QuoteRole, null, null, false);
            quoteAttributes = quote.getRelated(x.QuoteAttribute, null, null, false);
            quoteTerms = quote.getRelated(x.QuoteTerm, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        // set the role information
        cart.setOrderPartyId(quote.getString(x.partyId));
        if (UtilValidate.isNotEmpty(quoteRoles)) {
            for (GenericValue quoteRole : quoteRoles) {
                String quoteRoleTypeId = quoteRole.getString(x.roleTypeId);
                String quoteRolePartyId = quoteRole.getString(x.partyId);
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
                if (UtilValidate.isNotEmpty(quoteTerm.getString(x.termValue))) {
                    termValue = new BigDecimal(quoteTerm.getString(x.termValue));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(quoteTerm.getString(x.termDays))) {
                    termDays = Long.parseLong(quoteTerm.getString(x.termDays).trim());
                }
                String orderItemSeqId = quoteTerm.getString(x.quoteItemSeqId);
                cart.addOrderTerm(quoteTerm.getString(x.termTypeId), orderItemSeqId, termValue, termDays, quoteTerm.getString(x.textValue),
                        quoteTerm.getString(x.description));
            }
        }

        // set the attribute information
        if (UtilValidate.isNotEmpty(quoteAttributes)) {
            for (GenericValue quoteAttribute : quoteAttributes) {
                cart.setOrderAttribute(quoteAttribute.getString(x.attrName), quoteAttribute.getString(x.attrValue));
            }
        }

        // Convert the quote adjustment to order header adjustments and
        // put them in a map: the key/values pairs are quoteItemSeqId/List of adjs
        Map<String, List<GenericValue>> orderAdjsMap = new HashMap<>();
        for (GenericValue quoteAdj : quoteAdjs) {
            List<GenericValue> orderAdjs = orderAdjsMap.get(UtilValidate.isNotEmpty(quoteAdj.getString(x.quoteItemSeqId)) ? quoteAdj.getString(
                    x.quoteItemSeqId) : quoteId);
            if (orderAdjs == null) {
                orderAdjs = new LinkedList<>();
                orderAdjsMap.put(UtilValidate.isNotEmpty(quoteAdj.getString(x.quoteItemSeqId)) ? quoteAdj.getString(x.quoteItemSeqId) : quoteId,
                        orderAdjs);
            }
            // convert quote adjustments to order adjustments
            GenericValue orderAdj = delegator.makeValue("OrderAdjustment");
            orderAdj.put("orderAdjustmentId", quoteAdj.get(x.quoteAdjustmentId));
            orderAdj.put("orderAdjustmentTypeId", quoteAdj.get(x.quoteAdjustmentTypeId));
            orderAdj.put("orderItemSeqId", quoteAdj.get(x.quoteItemSeqId));
            orderAdj.put("comments", quoteAdj.get(x.comments));
            orderAdj.put("description", quoteAdj.get(x.description));
            orderAdj.put("amount", quoteAdj.get(x.amount));
            orderAdj.put("productPromoId", quoteAdj.get(x.productPromoId));
            orderAdj.put("productPromoRuleId", quoteAdj.get(x.productPromoRuleId));
            orderAdj.put("productPromoActionSeqId", quoteAdj.get(x.productPromoActionSeqId));
            orderAdj.put("productFeatureId", quoteAdj.get(x.productFeatureId));
            orderAdj.put("correspondingProductId", quoteAdj.get(x.correspondingProductId));
            orderAdj.put("sourceReferenceId", quoteAdj.get(x.sourceReferenceId));
            orderAdj.put("sourcePercentage", quoteAdj.get(x.sourcePercentage));
            orderAdj.put("customerReferenceId", quoteAdj.get(x.customerReferenceId));
            orderAdj.put("primaryGeoId", quoteAdj.get(x.primaryGeoId));
            orderAdj.put("secondaryGeoId", quoteAdj.get(x.secondaryGeoId));
            orderAdj.put("exemptAmount", quoteAdj.get(x.exemptAmount));
            orderAdj.put("taxAuthGeoId", quoteAdj.get(x.taxAuthGeoId));
            orderAdj.put("taxAuthPartyId", quoteAdj.get(x.taxAuthPartyId));
            orderAdj.put("overrideGlAccountId", quoteAdj.get(x.overrideGlAccountId));
            orderAdj.put("includeInTax", quoteAdj.get(x.includeInTax));
            orderAdj.put("includeInShipping", quoteAdj.get(x.includeInShipping));
            orderAdj.put("createdDate", quoteAdj.get(x.createdDate));
            orderAdj.put("createdByUserLogin", quoteAdj.get(x.createdByUserLogin));
            orderAdjs.add(orderAdj);
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(quoteItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue quoteItem : quoteItems) {
                // get the next item sequence id
                String orderItemSeqId = quoteItem.getString(x.quoteItemSeqId);
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

                boolean isPromo = quoteItem.get(x.isPromo) != null && "Y".equals(quoteItem.getString(x.isPromo));
                if (isPromo && !applyQuoteAdjustments) {
                    // do not include PROMO items
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = quoteItem.getBigDecimal(x.selectedAmount);
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                BigDecimal quantity = quoteItem.getBigDecimal(x.quantity);
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                BigDecimal quoteUnitPrice = quoteItem.getBigDecimal(x.quoteUnitPrice);
                if (quoteUnitPrice == null) {
                    quoteUnitPrice = BigDecimal.ZERO;
                }
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    // If, in the quote, an amount is set, we need to
                    // pass to the cart the quoteUnitPrice/amount value.
                    quoteUnitPrice = quoteUnitPrice.divide(amount, GEN_ROUNDING);
                }

                //rental product data
                Timestamp reservStart = quoteItem.getTimestamp(x.reservStart);
                BigDecimal reservLength = quoteItem.getBigDecimal(x.reservLength);
                BigDecimal reservPersons = quoteItem.getBigDecimal(x.reservPersons);
                int itemIndex = -1;
                if (quoteItem.get(x.productId) == null) {
                    // non-product item
                    String desc = quoteItem.getString(x.comments);
                    try {
                        // note that passing in null for itemGroupNumber as there is no real grouping concept in the quotes right now
                        itemIndex = cart.addNonProductItem(null, desc, null, null, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String productId = quoteItem.getString(x.productId);
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(quoteItem.getString(x.configId))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, quoteItem.getString(x.configId),
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
                cartItem.setItemComment(quoteItem.getString(x.comments));
                cartItem.setQuoteId(quoteItem.getString(x.quoteId));
                cartItem.setQuoteItemSeqId(quoteItem.getString(x.quoteItemSeqId));
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
        String adjType = cartAdj.getString(x.orderAdjustmentTypeId);
        return "SALES_TAX".equals(adjType) || "VAT_TAX".equals(adjType) || "VAT_PRICE_CORRECT".equals(adjType);
    }

    private static boolean isPromoAdjustment(GenericValue cartAdj) {
        String adjType = cartAdj.getString(x.orderAdjustmentTypeId);
        return "PROMOTION_ADJUSTMENT".equals(adjType);
    }

    public static Map<String, Object> loadCartFromShoppingList(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shoppingListId = (String) context.get(x.shoppingListId);
        String orderPartyId = (String) context.get(x.orderPartyId);
        Locale locale = (Locale) context.get(x.locale);

        // get the shopping list header
        GenericValue shoppingList = null;
        try {
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial required cart info
        String productStoreId = shoppingList.getString(x.productStoreId);
        String currency = shoppingList.getString(x.currencyUom);
        // If no currency has been set in the ShoppingList, use the ProductStore default currency
        if (currency == null) {
            try {
                GenericValue productStore = shoppingList.getRelatedOne(x.ProductStore, false);
                if (productStore != null) {
                    currency = productStore.getString(x.defaultCurrencyUomId);
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
            cart.setOrderPartyId(shoppingList.getString(x.partyId));
        }

        List<GenericValue> shoppingListItems = null;
        try {
            shoppingListItems = shoppingList.getRelated(x.ShoppingListItem, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(shoppingListItems)) {
            Pattern pattern = Pattern.compile("\\P{Digit}");
            for (GenericValue shoppingListItem : shoppingListItems) {
                // get the next item sequence id
                String orderItemSeqId = shoppingListItem.getString(x.shoppingListItemSeqId);
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
                BigDecimal modifiedPrice = shoppingListItem.getBigDecimal(x.modifiedPrice);
                BigDecimal quantity = shoppingListItem.getBigDecimal(x.quantity);
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                int itemIndex = -1;
                if (shoppingListItem.get(x.productId) != null) {
                    // product item
                    String productId = shoppingListItem.getString(x.productId);
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(shoppingListItem.getString(x.configId))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, shoppingListItem.getString(x.configId),
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
                cartItem.setShoppingList(shoppingListItem.getString(x.shoppingListId), shoppingListItem.getString(x.shoppingListItemSeqId));
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
        Locale locale = (Locale) context.get(x.locale);
        ShoppingCart shoppingCart = (ShoppingCart) context.get(x.shoppingCart);
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
        ShoppingCart shoppingCart = (ShoppingCart) context.get(x.shoppingCart);
        String productId = (String) context.get(x.productId);
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
        ShoppingCart cart = (ShoppingCart) context.get(x.shoppingCart);
        for (ShoppingCartItem item : cart) {
            cart.clearItemShipInfo(item);
            cart.setItemShipGroupQty(item, item.getQuantity(), 0);
        }
        return result;
    }

    public static Map<String, Object> prepareVendorShipGroups(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        ShoppingCart cart = (ShoppingCart) context.get(x.shoppingCart);
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
                String vendorPartyId = vendorProduct.getString(x.vendorPartyId);
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
