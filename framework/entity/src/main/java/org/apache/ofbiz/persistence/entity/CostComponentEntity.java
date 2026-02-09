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
@Entity(name = "COST_COMPONENT")
@Table(name = "COST_COMPONENT")
public class CostComponentEntity {
    @Id
    @Column(name = "COST_COMPONENT_ID")
    private String costComponentId;

    @Column(name = "COST_COMPONENT_TYPE_ID")
    private String costComponentTypeId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "COST_COMPONENT_CALC_ID")
    private String costComponentCalcId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "COST")
    private BigDecimal cost;

    @Column(name = "COST_UOM_ID")
    private String costUomId;
}
