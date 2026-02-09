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
@Entity(name = "WORK_EFFORT_FIXED_ASSET_ASSIGN")
@Table(name = "WORK_EFFORT_FIXED_ASSET_ASSIGN")
public class WorkEffortFixedAssetAssignEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "AVAILABILITY_STATUS_ID")
    private String availabilityStatusId;

    @Column(name = "ALLOCATED_COST")
    private BigDecimal allocatedCost;

    @Column(name = "COMMENTS")
    private String comments;
}
