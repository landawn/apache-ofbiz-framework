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
@Entity(name = "WORK_EFFORT_GOOD_STANDARD")
@Table(name = "WORK_EFFORT_GOOD_STANDARD")
public class WorkEffortGoodStandardEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "WORK_EFFORT_GOOD_STD_TYPE_ID")
    private String workEffortGoodStdTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "ESTIMATED_QUANTITY")
    private Double estimatedQuantity;

    @Column(name = "ESTIMATED_COST")
    private BigDecimal estimatedCost;
}
