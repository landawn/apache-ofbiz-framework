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
@Entity(name = "PRODUCT_ASSOC")
@Table(name = "PRODUCT_ASSOC")
public class ProductAssocEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PRODUCT_ID_TO")
    private String productIdTo;

    @Id
    @Column(name = "PRODUCT_ASSOC_TYPE_ID")
    private String productAssocTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "SCRAP_FACTOR")
    private BigDecimal scrapFactor;

    @Column(name = "INSTRUCTION")
    private String instruction;

    @Column(name = "ROUTING_WORK_EFFORT_ID")
    private String routingWorkEffortId;

    @Column(name = "ESTIMATE_CALC_METHOD")
    private String estimateCalcMethod;

    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;
}
