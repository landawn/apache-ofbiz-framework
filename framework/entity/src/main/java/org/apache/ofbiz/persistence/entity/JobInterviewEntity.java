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
@Entity(name = "JOB_INTERVIEW")
@Table(name = "JOB_INTERVIEW")
public class JobInterviewEntity {
    @Id
    @Column(name = "JOB_INTERVIEW_ID")
    private String jobInterviewId;

    @Column(name = "JOB_INTERVIEWEE_PARTY_ID")
    private String jobIntervieweePartyId;

    @Column(name = "JOB_REQUISITION_ID")
    private String jobRequisitionId;

    @Column(name = "JOB_INTERVIEWER_PARTY_ID")
    private String jobInterviewerPartyId;

    @Column(name = "JOB_INTERVIEW_TYPE_ID")
    private String jobInterviewTypeId;

    @Column(name = "GRADE_SECURED_ENUM_ID")
    private String gradeSecuredEnumId;

    @Column(name = "JOB_INTERVIEW_RESULT")
    private String jobInterviewResult;

    @Column(name = "JOB_INTERVIEW_DATE")
    private Date jobInterviewDate;
}
