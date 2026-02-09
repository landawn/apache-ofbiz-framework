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
@Entity(name = "INVENTORY_ITEM")
@Table(name = "INVENTORY_ITEM")
public class InventoryItemEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "INVENTORY_ITEM_TYPE_ID")
    private String inventoryItemTypeId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "OWNER_PARTY_ID")
    private String ownerPartyId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "DATETIME_RECEIVED")
    private Timestamp datetimeReceived;

    @Column(name = "DATETIME_MANUFACTURED")
    private Timestamp datetimeManufactured;

    @Column(name = "EXPIRE_DATE")
    private Timestamp expireDate;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "CONTAINER_ID")
    private String containerId;

    @Column(name = "LOT_ID")
    private String lotId;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "BIN_NUMBER")
    private String binNumber;

    @Column(name = "LOCATION_SEQ_ID")
    private String locationSeqId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "QUANTITY_ON_HAND_TOTAL")
    private BigDecimal quantityOnHandTotal;

    @Column(name = "AVAILABLE_TO_PROMISE_TOTAL")
    private BigDecimal availableToPromiseTotal;

    @Column(name = "ACCOUNTING_QUANTITY_TOTAL")
    private BigDecimal accountingQuantityTotal;

    @Column(name = "SERIAL_NUMBER")
    private String serialNumber;

    @Column(name = "SOFT_IDENTIFIER")
    private String softIdentifier;

    @Column(name = "ACTIVATION_NUMBER")
    private String activationNumber;

    @Column(name = "ACTIVATION_VALID_THRU")
    private Timestamp activationValidThru;

    @Column(name = "UNIT_COST")
    private BigDecimal unitCost;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;
}
