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
@Entity(name = "WORK_EFFORT_EVENT_REMINDER")
@Table(name = "WORK_EFFORT_EVENT_REMINDER")
public class WorkEffortEventReminderEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "SEQUENCE_ID")
    private String sequenceId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "REMINDER_DATE_TIME")
    private Timestamp reminderDateTime;

    @Column(name = "REPEAT_COUNT")
    private BigDecimal repeatCount;

    @Column(name = "REPEAT_INTERVAL")
    private BigDecimal repeatInterval;

    @Column(name = "CURRENT_COUNT")
    private BigDecimal currentCount;

    @Column(name = "REMINDER_OFFSET")
    private BigDecimal reminderOffset;

    @Column(name = "LOCALE_ID")
    private String localeId;

    @Column(name = "TIME_ZONE_ID")
    private String timeZoneId;
}
