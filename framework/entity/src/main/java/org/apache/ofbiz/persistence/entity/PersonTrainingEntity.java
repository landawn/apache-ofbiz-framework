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
@Entity(name = "PERSON_TRAINING")
@Table(name = "PERSON_TRAINING")
public class PersonTrainingEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "TRAINING_REQUEST_ID")
    private String trainingRequestId;

    @Id
    @Column(name = "TRAINING_CLASS_TYPE_ID")
    private String trainingClassTypeId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "APPROVER_ID")
    private String approverId;

    @Column(name = "APPROVAL_STATUS")
    private String approvalStatus;

    @Column(name = "REASON")
    private String reason;
}
