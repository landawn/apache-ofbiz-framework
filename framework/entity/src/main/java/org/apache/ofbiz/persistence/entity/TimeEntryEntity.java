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
@Entity(name = "TIME_ENTRY")
@Table(name = "TIME_ENTRY")
public class TimeEntryEntity {
    @Id
    @Column(name = "TIME_ENTRY_ID")
    private String timeEntryId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "RATE_TYPE_ID")
    private String rateTypeId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "TIMESHEET_ID")
    private String timesheetId;

    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "HOURS")
    private Double hours;

    @Column(name = "COMMENTS")
    private String comments;
}
