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
@Entity(name = "RECURRENCE_RULE")
@Table(name = "RECURRENCE_RULE")
public class RecurrenceRuleEntity {
    @Id
    @Column(name = "RECURRENCE_RULE_ID")
    private String recurrenceRuleId;

    @Column(name = "FREQUENCY")
    private String frequency;

    @Column(name = "UNTIL_DATE_TIME")
    private Timestamp untilDateTime;

    @Column(name = "COUNT_NUMBER")
    private BigDecimal countNumber;

    @Column(name = "INTERVAL_NUMBER")
    private BigDecimal intervalNumber;

    @Column(name = "BY_SECOND_LIST")
    private String bySecondList;

    @Column(name = "BY_MINUTE_LIST")
    private String byMinuteList;

    @Column(name = "BY_HOUR_LIST")
    private String byHourList;

    @Column(name = "BY_DAY_LIST")
    private String byDayList;

    @Column(name = "BY_MONTH_DAY_LIST")
    private String byMonthDayList;

    @Column(name = "BY_YEAR_DAY_LIST")
    private String byYearDayList;

    @Column(name = "BY_WEEK_NO_LIST")
    private String byWeekNoList;

    @Column(name = "BY_MONTH_LIST")
    private String byMonthList;

    @Column(name = "BY_SET_POS_LIST")
    private String bySetPosList;

    @Column(name = "WEEK_START")
    private String weekStart;

    @Column(name = "X_NAME")
    private String xName;
}
