package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "JOB_INTERVIEW_TYPE")
@Table(name = "JOB_INTERVIEW_TYPE")
public class JobInterviewTypeEntity {
    @Id
    @Column(name = "JOB_INTERVIEW_TYPE_ID")
    private String jobInterviewTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
