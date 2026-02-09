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
@Entity(name = "INVENTORY_ITEM_DETAIL")
@Table(name = "INVENTORY_ITEM_DETAIL")
public class InventoryItemDetailEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Id
    @Column(name = "INVENTORY_ITEM_DETAIL_SEQ_ID")
    private String inventoryItemDetailSeqId;

    @Column(name = "EFFECTIVE_DATE")
    private Timestamp effectiveDate;

    @Column(name = "QUANTITY_ON_HAND_DIFF")
    private BigDecimal quantityOnHandDiff;

    @Column(name = "AVAILABLE_TO_PROMISE_DIFF")
    private BigDecimal availableToPromiseDiff;

    @Column(name = "ACCOUNTING_QUANTITY_DIFF")
    private BigDecimal accountingQuantityDiff;

    @Column(name = "UNIT_COST")
    private BigDecimal unitCost;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "RETURN_ID")
    private String returnId;

    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "MAINT_HIST_SEQ_ID")
    private String maintHistSeqId;

    @Column(name = "ITEM_ISSUANCE_ID")
    private String itemIssuanceId;

    @Column(name = "RECEIPT_ID")
    private String receiptId;

    @Column(name = "PHYSICAL_INVENTORY_ID")
    private String physicalInventoryId;

    @Column(name = "REASON_ENUM_ID")
    private String reasonEnumId;

    @Column(name = "DESCRIPTION")
    private String description;
}
