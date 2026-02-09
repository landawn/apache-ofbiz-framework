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
@Entity(name = "FACILITY")
@Table(name = "FACILITY")
public class FacilityEntity {
    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "FACILITY_TYPE_ID")
    private String facilityTypeId;

    @Column(name = "PARENT_FACILITY_ID")
    private String parentFacilityId;

    @Column(name = "OWNER_PARTY_ID")
    private String ownerPartyId;

    @Column(name = "DEFAULT_INVENTORY_ITEM_TYPE_ID")
    private String defaultInventoryItemTypeId;

    @Column(name = "FACILITY_NAME")
    private String facilityName;

    @Column(name = "PRIMARY_FACILITY_GROUP_ID")
    private String primaryFacilityGroupId;

    @Column(name = "FACILITY_SIZE")
    private BigDecimal facilitySize;

    @Column(name = "FACILITY_SIZE_UOM_ID")
    private String facilitySizeUomId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "DEFAULT_DAYS_TO_SHIP")
    private BigDecimal defaultDaysToShip;

    @Column(name = "OPENED_DATE")
    private Timestamp openedDate;

    @Column(name = "CLOSED_DATE")
    private Timestamp closedDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_DIMENSION_UOM_ID")
    private String defaultDimensionUomId;

    @Column(name = "DEFAULT_WEIGHT_UOM_ID")
    private String defaultWeightUomId;

    @Column(name = "GEO_POINT_ID")
    private String geoPointId;

    @Column(name = "FACILITY_LEVEL")
    private BigDecimal facilityLevel;
}
