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
@Entity(name = "SURVEY_QUESTION")
@Table(name = "SURVEY_QUESTION")
public class SurveyQuestionEntity {
    @Id
    @Column(name = "SURVEY_QUESTION_ID")
    private String surveyQuestionId;

    @Column(name = "SURVEY_QUESTION_CATEGORY_ID")
    private String surveyQuestionCategoryId;

    @Column(name = "SURVEY_QUESTION_TYPE_ID")
    private String surveyQuestionTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "QUESTION")
    private String question;

    @Column(name = "HINT")
    private String hint;

    @Column(name = "ENUM_TYPE_ID")
    private String enumTypeId;

    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "FORMAT_STRING")
    private String formatString;
}
