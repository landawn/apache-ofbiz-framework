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
@Entity(name = "PRODUCT_FEATURE_APPL")
@Table(name = "PRODUCT_FEATURE_APPL")
public class ProductFeatureApplEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "PRODUCT_FEATURE_APPL_TYPE_ID")
    private String productFeatureApplTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "RECURRING_AMOUNT")
    private BigDecimal recurringAmount;
}
