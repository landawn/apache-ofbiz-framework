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
@Entity(name = "RECURRENCE_INFO")
@Table(name = "RECURRENCE_INFO")
public class RecurrenceInfoEntity {
    @Id
    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;

    @Column(name = "START_DATE_TIME")
    private Timestamp startDateTime;

    @Column(name = "EXCEPTION_DATE_TIMES")
    private String exceptionDateTimes;

    @Column(name = "RECURRENCE_DATE_TIMES")
    private String recurrenceDateTimes;

    @Column(name = "EXCEPTION_RULE_ID")
    private String exceptionRuleId;

    @Column(name = "RECURRENCE_RULE_ID")
    private String recurrenceRuleId;

    @Column(name = "RECURRENCE_COUNT")
    private BigDecimal recurrenceCount;
}
