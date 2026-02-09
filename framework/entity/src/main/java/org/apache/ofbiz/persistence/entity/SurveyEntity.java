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
@Entity(name = "SURVEY")
@Table(name = "SURVEY")
public class SurveyEntity {
    @Id
    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Column(name = "SURVEY_NAME")
    private String surveyName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "SUBMIT_CAPTION")
    private String submitCaption;

    @Column(name = "RESPONSE_SERVICE")
    private String responseService;

    @Column(name = "IS_ANONYMOUS")
    private String isAnonymous;

    @Column(name = "ALLOW_MULTIPLE")
    private String allowMultiple;

    @Column(name = "ALLOW_UPDATE")
    private String allowUpdate;

    @Column(name = "ACRO_FORM_CONTENT_ID")
    private String acroFormContentId;
}
