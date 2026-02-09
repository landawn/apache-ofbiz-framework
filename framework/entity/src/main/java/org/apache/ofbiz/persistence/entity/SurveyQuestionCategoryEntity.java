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
@Entity(name = "SURVEY_QUESTION_CATEGORY")
@Table(name = "SURVEY_QUESTION_CATEGORY")
public class SurveyQuestionCategoryEntity {
    @Id
    @Column(name = "SURVEY_QUESTION_CATEGORY_ID")
    private String surveyQuestionCategoryId;

    @Column(name = "PARENT_CATEGORY_ID")
    private String parentCategoryId;

    @Column(name = "DESCRIPTION")
    private String description;
}
