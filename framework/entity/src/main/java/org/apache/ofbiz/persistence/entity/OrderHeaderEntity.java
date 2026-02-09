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
@Entity(name = "ORDER_HEADER")
@Table(name = "ORDER_HEADER")
public class OrderHeaderEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_TYPE_ID")
    private String orderTypeId;

    @Column(name = "ORDER_NAME")
    private String orderName;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "SALES_CHANNEL_ENUM_ID")
    private String salesChannelEnumId;

    @Column(name = "ORDER_DATE")
    private Timestamp orderDate;

    @Column(name = "PRIORITY")
    private String priority;

    @Column(name = "ENTRY_DATE")
    private Timestamp entryDate;

    @Column(name = "PICK_SHEET_PRINTED_DATE")
    private Timestamp pickSheetPrintedDate;

    @Column(name = "VISIT_ID")
    private String visitId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "FIRST_ATTEMPT_ORDER_ID")
    private String firstAttemptOrderId;

    @Column(name = "CURRENCY_UOM")
    private String currencyUom;

    @Column(name = "SYNC_STATUS_ID")
    private String syncStatusId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "ORIGIN_FACILITY_ID")
    private String originFacilityId;

    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Column(name = "TERMINAL_ID")
    private String terminalId;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;

    @Column(name = "AUTO_ORDER_SHOPPING_LIST_ID")
    private String autoOrderShoppingListId;

    @Column(name = "NEEDS_INVENTORY_ISSUANCE")
    private String needsInventoryIssuance;

    @Column(name = "IS_RUSH_ORDER")
    private String isRushOrder;

    @Column(name = "INTERNAL_CODE")
    private String internalCode;

    @Column(name = "REMAINING_SUB_TOTAL")
    private BigDecimal remainingSubTotal;

    @Column(name = "GRAND_TOTAL")
    private BigDecimal grandTotal;

    @Column(name = "IS_VIEWED")
    private String isViewed;

    @Column(name = "INVOICE_PER_SHIPMENT")
    private String invoicePerShipment;
}
