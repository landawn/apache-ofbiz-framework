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
@Entity(name = "EMPLOYMENT_APP")
@Table(name = "EMPLOYMENT_APP")
public class EmploymentAppEntity {
    @Id
    @Column(name = "APPLICATION_ID")
    private String applicationId;

    @Column(name = "EMPL_POSITION_ID")
    private String emplPositionId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "EMPLOYMENT_APP_SOURCE_TYPE_ID")
    private String employmentAppSourceTypeId;

    @Column(name = "APPLYING_PARTY_ID")
    private String applyingPartyId;

    @Column(name = "REFERRED_BY_PARTY_ID")
    private String referredByPartyId;

    @Column(name = "APPLICATION_DATE")
    private Timestamp applicationDate;

    @Column(name = "APPROVER_PARTY_ID")
    private String approverPartyId;

    @Column(name = "JOB_REQUISITION_ID")
    private String jobRequisitionId;
}
