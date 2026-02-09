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
@Entity(name = "FIXED_ASSET_MAINT")
@Table(name = "FIXED_ASSET_MAINT")
public class FixedAssetMaintEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "MAINT_HIST_SEQ_ID")
    private String maintHistSeqId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PRODUCT_MAINT_TYPE_ID")
    private String productMaintTypeId;

    @Column(name = "PRODUCT_MAINT_SEQ_ID")
    private String productMaintSeqId;

    @Column(name = "SCHEDULE_WORK_EFFORT_ID")
    private String scheduleWorkEffortId;

    @Column(name = "INTERVAL_QUANTITY")
    private BigDecimal intervalQuantity;

    @Column(name = "INTERVAL_UOM_ID")
    private String intervalUomId;

    @Column(name = "INTERVAL_METER_TYPE_ID")
    private String intervalMeterTypeId;

    @Column(name = "PURCHASE_ORDER_ID")
    private String purchaseOrderId;
}
