package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TECH_DATA_CALENDAR_WEEK")
@Table(name = "TECH_DATA_CALENDAR_WEEK")
public class TechDataCalendarWeekEntity {
    @Id
    @Column(name = "CALENDAR_WEEK_ID")
    private String calendarWeekId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "MONDAY_START_TIME")
    private Time mondayStartTime;

    @Column(name = "MONDAY_CAPACITY")
    private Double mondayCapacity;

    @Column(name = "TUESDAY_START_TIME")
    private Time tuesdayStartTime;

    @Column(name = "TUESDAY_CAPACITY")
    private Double tuesdayCapacity;

    @Column(name = "WEDNESDAY_START_TIME")
    private Time wednesdayStartTime;

    @Column(name = "WEDNESDAY_CAPACITY")
    private Double wednesdayCapacity;

    @Column(name = "THURSDAY_START_TIME")
    private Time thursdayStartTime;

    @Column(name = "THURSDAY_CAPACITY")
    private Double thursdayCapacity;

    @Column(name = "FRIDAY_START_TIME")
    private Time fridayStartTime;

    @Column(name = "FRIDAY_CAPACITY")
    private Double fridayCapacity;

    @Column(name = "SATURDAY_START_TIME")
    private Time saturdayStartTime;

    @Column(name = "SATURDAY_CAPACITY")
    private Double saturdayCapacity;

    @Column(name = "SUNDAY_START_TIME")
    private Time sundayStartTime;

    @Column(name = "SUNDAY_CAPACITY")
    private Double sundayCapacity;
}
