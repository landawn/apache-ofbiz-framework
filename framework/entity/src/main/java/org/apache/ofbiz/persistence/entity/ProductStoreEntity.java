package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT_STORE")
@Table(name = "PRODUCT_STORE")
public class ProductStoreEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "PRIMARY_STORE_GROUP_ID")
    private String primaryStoreGroupId;

    @Column(name = "STORE_NAME")
    private String storeName;

    @Column(name = "COMPANY_NAME")
    private String companyName;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "SUBTITLE")
    private String subtitle;

    @Column(name = "PAY_TO_PARTY_ID")
    private String payToPartyId;

    @Column(name = "DAYS_TO_CANCEL_NON_PAY")
    private BigDecimal daysToCancelNonPay;

    @Column(name = "MANUAL_AUTH_IS_CAPTURE")
    private String manualAuthIsCapture;

    @Column(name = "PRORATE_SHIPPING")
    private String prorateShipping;

    @Column(name = "PRORATE_TAXES")
    private String prorateTaxes;

    @Column(name = "VIEW_CART_ON_ADD")
    private String viewCartOnAdd;

    @Column(name = "AUTO_SAVE_CART")
    private String autoSaveCart;

    @Column(name = "AUTO_APPROVE_REVIEWS")
    private String autoApproveReviews;

    @Column(name = "IS_DEMO_STORE")
    private String isDemoStore;

    @Column(name = "IS_IMMEDIATELY_FULFILLED")
    private String isImmediatelyFulfilled;

    @Column(name = "INVENTORY_FACILITY_ID")
    private String inventoryFacilityId;

    @Column(name = "ONE_INVENTORY_FACILITY")
    private String oneInventoryFacility;

    @Column(name = "CHECK_INVENTORY")
    private String checkInventory;

    @Column(name = "RESERVE_INVENTORY")
    private String reserveInventory;

    @Column(name = "RESERVE_ORDER_ENUM_ID")
    private String reserveOrderEnumId;

    @Column(name = "REQUIRE_INVENTORY")
    private String requireInventory;

    @Column(name = "BALANCE_RES_ON_ORDER_CREATION")
    private String balanceResOnOrderCreation;

    @Column(name = "REQUIREMENT_METHOD_ENUM_ID")
    private String requirementMethodEnumId;

    @Column(name = "ORDER_NUMBER_PREFIX")
    private String orderNumberPrefix;

    @Column(name = "DEFAULT_LOCALE_STRING")
    private String defaultLocaleString;

    @Column(name = "DEFAULT_CURRENCY_UOM_ID")
    private String defaultCurrencyUomId;

    @Column(name = "DEFAULT_TIME_ZONE_STRING")
    private String defaultTimeZoneString;

    @Column(name = "DEFAULT_SALES_CHANNEL_ENUM_ID")
    private String defaultSalesChannelEnumId;

    @Column(name = "ALLOW_PASSWORD")
    private String allowPassword;

    @Column(name = "DEFAULT_PASSWORD")
    private String defaultPassword;

    @Column(name = "EXPLODE_ORDER_ITEMS")
    private String explodeOrderItems;

    @Column(name = "CHECK_GC_BALANCE")
    private String checkGcBalance;

    @Column(name = "RETRY_FAILED_AUTHS")
    private String retryFailedAuths;

    @Column(name = "HEADER_APPROVED_STATUS")
    private String headerApprovedStatus;

    @Column(name = "ITEM_APPROVED_STATUS")
    private String itemApprovedStatus;

    @Column(name = "DIGITAL_ITEM_APPROVED_STATUS")
    private String digitalItemApprovedStatus;

    @Column(name = "HEADER_DECLINED_STATUS")
    private String headerDeclinedStatus;

    @Column(name = "ITEM_DECLINED_STATUS")
    private String itemDeclinedStatus;

    @Column(name = "HEADER_CANCEL_STATUS")
    private String headerCancelStatus;

    @Column(name = "ITEM_CANCEL_STATUS")
    private String itemCancelStatus;

    @Column(name = "AUTH_DECLINED_MESSAGE")
    private String authDeclinedMessage;

    @Column(name = "AUTH_FRAUD_MESSAGE")
    private String authFraudMessage;

    @Column(name = "AUTH_ERROR_MESSAGE")
    private String authErrorMessage;

    @Column(name = "VISUAL_THEME_ID")
    private String visualThemeId;

    @Column(name = "STORE_CREDIT_ACCOUNT_ENUM_ID")
    private String storeCreditAccountEnumId;

    @Column(name = "USE_PRIMARY_EMAIL_USERNAME")
    private String usePrimaryEmailUsername;

    @Column(name = "REQUIRE_CUSTOMER_ROLE")
    private String requireCustomerRole;

    @Column(name = "AUTO_INVOICE_DIGITAL_ITEMS")
    private String autoInvoiceDigitalItems;

    @Column(name = "REQ_SHIP_ADDR_FOR_DIG_ITEMS")
    private String reqShipAddrForDigItems;

    @Column(name = "SHOW_CHECKOUT_GIFT_OPTIONS")
    private String showCheckoutGiftOptions;

    @Column(name = "SELECT_PAYMENT_TYPE_PER_ITEM")
    private String selectPaymentTypePerItem;

    @Column(name = "SHOW_PRICES_WITH_VAT_TAX")
    private String showPricesWithVatTax;

    @Column(name = "SHOW_TAX_IS_EXEMPT")
    private String showTaxIsExempt;

    @Column(name = "VAT_TAX_AUTH_GEO_ID")
    private String vatTaxAuthGeoId;

    @Column(name = "VAT_TAX_AUTH_PARTY_ID")
    private String vatTaxAuthPartyId;

    @Column(name = "ENABLE_AUTO_SUGGESTION_LIST")
    private String enableAutoSuggestionList;

    @Column(name = "ENABLE_DIG_PROD_UPLOAD")
    private String enableDigProdUpload;

    @Column(name = "PROD_SEARCH_EXCLUDE_VARIANTS")
    private String prodSearchExcludeVariants;

    @Column(name = "DIG_PROD_UPLOAD_CATEGORY_ID")
    private String digProdUploadCategoryId;

    @Column(name = "AUTO_ORDER_CC_TRY_EXP")
    private String autoOrderCcTryExp;

    @Column(name = "AUTO_ORDER_CC_TRY_OTHER_CARDS")
    private String autoOrderCcTryOtherCards;

    @Column(name = "AUTO_ORDER_CC_TRY_LATER_NSF")
    private String autoOrderCcTryLaterNsf;

    @Column(name = "AUTO_ORDER_CC_TRY_LATER_MAX")
    private BigDecimal autoOrderCcTryLaterMax;

    @Column(name = "STORE_CREDIT_VALID_DAYS")
    private BigDecimal storeCreditValidDays;

    @Column(name = "AUTO_APPROVE_INVOICE")
    private String autoApproveInvoice;

    @Column(name = "AUTO_APPROVE_ORDER")
    private String autoApproveOrder;

    @Column(name = "SHIP_IF_CAPTURE_FAILS")
    private String shipIfCaptureFails;

    @Column(name = "SET_OWNER_UPON_ISSUANCE")
    private String setOwnerUponIssuance;

    @Column(name = "REQ_RETURN_INVENTORY_RECEIVE")
    private String reqReturnInventoryReceive;

    @Column(name = "ADD_TO_CART_REMOVE_INCOMPAT")
    private String addToCartRemoveIncompat;

    @Column(name = "ADD_TO_CART_REPLACE_UPSELL")
    private String addToCartReplaceUpsell;

    @Column(name = "SPLIT_PAY_PREF_PER_SHP_GRP")
    private String splitPayPrefPerShpGrp;

    @Column(name = "MANAGED_BY_LOT")
    private String managedByLot;

    @Column(name = "SHOW_OUT_OF_STOCK_PRODUCTS")
    private String showOutOfStockProducts;

    @Column(name = "ORDER_DECIMAL_QUANTITY")
    private String orderDecimalQuantity;

    @Column(name = "ALLOW_COMMENT")
    private String allowComment;

    @Column(name = "ALLOCATE_INVENTORY")
    private String allocateInventory;
}
