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
@Entity(name = "FIXED_ASSET_METER")
@Table(name = "FIXED_ASSET_METER")
public class FixedAssetMeterEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "PRODUCT_METER_TYPE_ID")
    private String productMeterTypeId;

    @Id
    @Column(name = "READING_DATE")
    private Timestamp readingDate;

    @Column(name = "METER_VALUE")
    private BigDecimal meterValue;

    @Column(name = "READING_REASON_ENUM_ID")
    private String readingReasonEnumId;

    @Column(name = "MAINT_HIST_SEQ_ID")
    private String maintHistSeqId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;
}
