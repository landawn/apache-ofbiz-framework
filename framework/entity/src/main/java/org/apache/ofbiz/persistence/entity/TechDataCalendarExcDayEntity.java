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
@Entity(name = "TECH_DATA_CALENDAR_EXC_DAY")
@Table(name = "TECH_DATA_CALENDAR_EXC_DAY")
public class TechDataCalendarExcDayEntity {
    @Id
    @Column(name = "CALENDAR_ID")
    private String calendarId;

    @Id
    @Column(name = "EXCEPTION_DATE_START_TIME")
    private Timestamp exceptionDateStartTime;

    @Column(name = "EXCEPTION_CAPACITY")
    private BigDecimal exceptionCapacity;

    @Column(name = "USED_CAPACITY")
    private BigDecimal usedCapacity;

    @Column(name = "DESCRIPTION")
    private String description;
}
