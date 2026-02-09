package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TECH_DATA_CALENDAR_EXC_WEEK")
@Table(name = "TECH_DATA_CALENDAR_EXC_WEEK")
public class TechDataCalendarExcWeekEntity {
    @Id
    @Column(name = "CALENDAR_ID")
    private String calendarId;

    @Id
    @Column(name = "EXCEPTION_DATE_START")
    private Date exceptionDateStart;

    @Column(name = "CALENDAR_WEEK_ID")
    private String calendarWeekId;

    @Column(name = "DESCRIPTION")
    private String description;
}
