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
@Entity(name = "PRODUCT_MAINT")
@Table(name = "PRODUCT_MAINT")
public class ProductMaintEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PRODUCT_MAINT_SEQ_ID")
    private String productMaintSeqId;

    @Column(name = "PRODUCT_MAINT_TYPE_ID")
    private String productMaintTypeId;

    @Column(name = "MAINT_NAME")
    private String maintName;

    @Column(name = "MAINT_TEMPLATE_WORK_EFFORT_ID")
    private String maintTemplateWorkEffortId;

    @Column(name = "INTERVAL_QUANTITY")
    private BigDecimal intervalQuantity;

    @Column(name = "INTERVAL_UOM_ID")
    private String intervalUomId;

    @Column(name = "INTERVAL_METER_TYPE_ID")
    private String intervalMeterTypeId;

    @Column(name = "REPEAT_COUNT")
    private BigDecimal repeatCount;
}
