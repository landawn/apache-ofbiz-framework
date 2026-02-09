package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "FIXED_ASSET")
@Table(name = "FIXED_ASSET")
public class FixedAssetEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "FIXED_ASSET_TYPE_ID")
    private String fixedAssetTypeId;

    @Column(name = "PARENT_FIXED_ASSET_ID")
    private String parentFixedAssetId;

    @Column(name = "INSTANCE_OF_PRODUCT_ID")
    private String instanceOfProductId;

    @Column(name = "CLASS_ENUM_ID")
    private String classEnumId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "FIXED_ASSET_NAME")
    private String fixedAssetName;

    @Column(name = "ACQUIRE_ORDER_ID")
    private String acquireOrderId;

    @Column(name = "ACQUIRE_ORDER_ITEM_SEQ_ID")
    private String acquireOrderItemSeqId;

    @Column(name = "DATE_ACQUIRED")
    private Timestamp dateAcquired;

    @Column(name = "DATE_LAST_SERVICED")
    private Timestamp dateLastServiced;

    @Column(name = "DATE_NEXT_SERVICE")
    private Timestamp dateNextService;

    @Column(name = "EXPECTED_END_OF_LIFE")
    private Date expectedEndOfLife;

    @Column(name = "ACTUAL_END_OF_LIFE")
    private Date actualEndOfLife;

    @Column(name = "PRODUCTION_CAPACITY")
    private BigDecimal productionCapacity;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "CALENDAR_ID")
    private String calendarId;

    @Column(name = "SERIAL_NUMBER")
    private String serialNumber;

    @Column(name = "LOCATED_AT_FACILITY_ID")
    private String locatedAtFacilityId;

    @Column(name = "LOCATED_AT_LOCATION_SEQ_ID")
    private String locatedAtLocationSeqId;

    @Column(name = "SALVAGE_VALUE")
    private BigDecimal salvageValue;

    @Column(name = "DEPRECIATION")
    private BigDecimal depreciation;

    @Column(name = "PURCHASE_COST")
    private BigDecimal purchaseCost;

    @Column(name = "PURCHASE_COST_UOM_ID")
    private String purchaseCostUomId;
}
