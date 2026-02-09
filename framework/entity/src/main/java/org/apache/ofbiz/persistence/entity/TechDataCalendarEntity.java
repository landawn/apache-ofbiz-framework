package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TECH_DATA_CALENDAR")
@Table(name = "TECH_DATA_CALENDAR")
public class TechDataCalendarEntity {
    @Id
    @Column(name = "CALENDAR_ID")
    private String calendarId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CALENDAR_WEEK_ID")
    private String calendarWeekId;
}
