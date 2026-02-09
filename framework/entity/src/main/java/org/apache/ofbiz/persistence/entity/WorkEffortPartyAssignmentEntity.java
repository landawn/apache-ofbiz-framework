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
@Entity(name = "WORK_EFFORT_PARTY_ASSIGNMENT")
@Table(name = "WORK_EFFORT_PARTY_ASSIGNMENT")
public class WorkEffortPartyAssignmentEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "ASSIGNED_BY_USER_LOGIN_ID")
    private String assignedByUserLoginId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "STATUS_DATE_TIME")
    private Timestamp statusDateTime;

    @Column(name = "EXPECTATION_ENUM_ID")
    private String expectationEnumId;

    @Column(name = "DELEGATE_REASON_ENUM_ID")
    private String delegateReasonEnumId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "MUST_RSVP")
    private String mustRsvp;

    @Column(name = "AVAILABILITY_STATUS_ID")
    private String availabilityStatusId;
}
