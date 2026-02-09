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
@Entity(name = "FIXED_ASSET_STD_COST")
@Table(name = "FIXED_ASSET_STD_COST")
public class FixedAssetStdCostEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "FIXED_ASSET_STD_COST_TYPE_ID")
    private String fixedAssetStdCostTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "AMOUNT_UOM_ID")
    private String amountUomId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
