package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "JOB_REQUISITION")
@Table(name = "JOB_REQUISITION")
public class JobRequisitionEntity {
    @Id
    @Column(name = "JOB_REQUISITION_ID")
    private String jobRequisitionId;

    @Column(name = "DURATION_MONTHS")
    private BigDecimal durationMonths;

    @Column(name = "AGE")
    private BigDecimal age;

    @Column(name = "GENDER")
    private String gender;

    @Column(name = "EXPERIENCE_MONTHS")
    private BigDecimal experienceMonths;

    @Column(name = "EXPERIENCE_YEARS")
    private BigDecimal experienceYears;

    @Column(name = "QUALIFICATION")
    private String qualification;

    @Column(name = "JOB_LOCATION")
    private String jobLocation;

    @Column(name = "SKILL_TYPE_ID")
    private String skillTypeId;

    @Column(name = "NO_OF_RESOURCES")
    private BigDecimal noOfResources;

    @Column(name = "JOB_POSTING_TYPE_ENUM_ID")
    private String jobPostingTypeEnumId;

    @Column(name = "JOB_REQUISITION_DATE")
    private Date jobRequisitionDate;

    @Column(name = "EXAM_TYPE_ENUM_ID")
    private String examTypeEnumId;

    @Column(name = "REQUIRED_ON_DATE")
    private Date requiredOnDate;
}
