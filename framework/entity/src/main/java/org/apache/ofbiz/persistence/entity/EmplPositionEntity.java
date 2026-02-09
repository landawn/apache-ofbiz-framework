package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "EMPL_POSITION")
@Table(name = "EMPL_POSITION")
public class EmplPositionEntity {
    @Id
    @Column(name = "EMPL_POSITION_ID")
    private String emplPositionId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Column(name = "EMPL_POSITION_TYPE_ID")
    private String emplPositionTypeId;

    @Column(name = "ESTIMATED_FROM_DATE")
    private Timestamp estimatedFromDate;

    @Column(name = "ESTIMATED_THRU_DATE")
    private Timestamp estimatedThruDate;

    @Column(name = "SALARY_FLAG")
    private String salaryFlag;

    @Column(name = "EXEMPT_FLAG")
    private String exemptFlag;

    @Column(name = "FULLTIME_FLAG")
    private String fulltimeFlag;

    @Column(name = "TEMPORARY_FLAG")
    private String temporaryFlag;

    @Column(name = "ACTUAL_FROM_DATE")
    private Timestamp actualFromDate;

    @Column(name = "ACTUAL_THRU_DATE")
    private Timestamp actualThruDate;
}
