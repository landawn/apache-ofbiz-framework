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
@Entity(name = "PAY_HISTORY")
@Table(name = "PAY_HISTORY")
public class PayHistoryEntity {
    @Id
    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Id
    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Id
    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Id
    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Id
    @Column(name = "EMPL_FROM_DATE")
    private Timestamp emplFromDate;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SALARY_STEP_SEQ_ID")
    private String salaryStepSeqId;

    @Column(name = "PAY_GRADE_ID")
    private String payGradeId;

    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "COMMENTS")
    private String comments;
}
