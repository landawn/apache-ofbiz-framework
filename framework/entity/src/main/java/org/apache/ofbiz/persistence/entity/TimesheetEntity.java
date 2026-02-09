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
@Entity(name = "TIMESHEET")
@Table(name = "TIMESHEET")
public class TimesheetEntity {
    @Id
    @Column(name = "TIMESHEET_ID")
    private String timesheetId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "CLIENT_PARTY_ID")
    private String clientPartyId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "APPROVED_BY_USER_LOGIN_ID")
    private String approvedByUserLoginId;

    @Column(name = "COMMENTS")
    private String comments;
}
