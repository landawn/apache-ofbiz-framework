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
@Entity(name = "SURVEY_APPL_TYPE")
@Table(name = "SURVEY_APPL_TYPE")
public class SurveyApplTypeEntity {
    @Id
    @Column(name = "SURVEY_APPL_TYPE_ID")
    private String surveyApplTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
