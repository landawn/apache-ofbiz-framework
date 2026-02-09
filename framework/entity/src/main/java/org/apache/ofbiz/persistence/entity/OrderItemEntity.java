package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDER_ITEM")
@Table(name = "ORDER_ITEM")
public class OrderItemEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "ORDER_ITEM_TYPE_ID")
    private String orderItemTypeId;

    @Column(name = "ORDER_ITEM_GROUP_SEQ_ID")
    private String orderItemGroupSeqId;

    @Column(name = "IS_ITEM_GROUP_PRIMARY")
    private String isItemGroupPrimary;

    @Column(name = "FROM_INVENTORY_ITEM_ID")
    private String fromInventoryItemId;

    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "SUPPLIER_PRODUCT_ID")
    private String supplierProductId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "PROD_CATALOG_ID")
    private String prodCatalogId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "IS_PROMO")
    private String isPromo;

    @Column(name = "QUOTE_ID")
    private String quoteId;

    @Column(name = "QUOTE_ITEM_SEQ_ID")
    private String quoteItemSeqId;

    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Column(name = "SHOPPING_LIST_ITEM_SEQ_ID")
    private String shoppingListItemSeqId;

    @Column(name = "SUBSCRIPTION_ID")
    private String subscriptionId;

    @Column(name = "DEPLOYMENT_ID")
    private String deploymentId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "CANCEL_QUANTITY")
    private BigDecimal cancelQuantity;

    @Column(name = "SELECTED_AMOUNT")
    private BigDecimal selectedAmount;

    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "UNIT_LIST_PRICE")
    private BigDecimal unitListPrice;

    @Column(name = "UNIT_AVERAGE_COST")
    private BigDecimal unitAverageCost;

    @Column(name = "UNIT_RECURRING_PRICE")
    private BigDecimal unitRecurringPrice;

    @Column(name = "DISCOUNT_RATE")
    private BigDecimal discountRate;

    @Column(name = "IS_MODIFIED_PRICE")
    private String isModifiedPrice;

    @Column(name = "RECURRING_FREQ_UOM_ID")
    private String recurringFreqUomId;

    @Column(name = "ITEM_DESCRIPTION")
    private String itemDescription;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "CORRESPONDING_PO_ID")
    private String correspondingPoId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "SYNC_STATUS_ID")
    private String syncStatusId;

    @Column(name = "ESTIMATED_SHIP_DATE")
    private Timestamp estimatedShipDate;

    @Column(name = "ESTIMATED_DELIVERY_DATE")
    private Timestamp estimatedDeliveryDate;

    @Column(name = "AUTO_CANCEL_DATE")
    private Timestamp autoCancelDate;

    @Column(name = "DONT_CANCEL_SET_DATE")
    private Timestamp dontCancelSetDate;

    @Column(name = "DONT_CANCEL_SET_USER_LOGIN")
    private String dontCancelSetUserLogin;

    @Column(name = "SHIP_BEFORE_DATE")
    private Timestamp shipBeforeDate;

    @Column(name = "SHIP_AFTER_DATE")
    private Timestamp shipAfterDate;

    @Column(name = "RESERVE_AFTER_DATE")
    private Timestamp reserveAfterDate;

    @Column(name = "CANCEL_BACK_ORDER_DATE")
    private Timestamp cancelBackOrderDate;

    @Column(name = "OVERRIDE_GL_ACCOUNT_ID")
    private String overrideGlAccountId;

    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;
}
