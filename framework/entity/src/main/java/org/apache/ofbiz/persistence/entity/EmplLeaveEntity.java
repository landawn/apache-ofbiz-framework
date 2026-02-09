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
@Entity(name = "EMPL_LEAVE")
@Table(name = "EMPL_LEAVE")
public class EmplLeaveEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "LEAVE_TYPE_ID")
    private String leaveTypeId;

    @Column(name = "EMPL_LEAVE_REASON_TYPE_ID")
    private String emplLeaveReasonTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "APPROVER_PARTY_ID")
    private String approverPartyId;

    @Column(name = "LEAVE_STATUS")
    private String leaveStatus;

    @Column(name = "DESCRIPTION")
    private String description;
}
