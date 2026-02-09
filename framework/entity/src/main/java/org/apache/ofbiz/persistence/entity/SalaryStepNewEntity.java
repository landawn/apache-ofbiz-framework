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
@Entity(name = "SALARY_STEP_NEW")
@Table(name = "SALARY_STEP_NEW")
public class SalaryStepNewEntity {
    @Id
    @Column(name = "SALARY_STEP_SEQ_ID")
    private String salaryStepSeqId;

    @Id
    @Column(name = "PAY_GRADE_ID")
    private String payGradeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "DATE_MODIFIED")
    private Timestamp dateModified;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
