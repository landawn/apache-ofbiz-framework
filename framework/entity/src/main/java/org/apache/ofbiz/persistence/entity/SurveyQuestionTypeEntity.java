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
@Entity(name = "SURVEY_QUESTION_TYPE")
@Table(name = "SURVEY_QUESTION_TYPE")
public class SurveyQuestionTypeEntity {
    @Id
    @Column(name = "SURVEY_QUESTION_TYPE_ID")
    private String surveyQuestionTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
