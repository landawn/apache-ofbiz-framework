package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SURVEY_QUESTION_APPL")
@Table(name = "SURVEY_QUESTION_APPL")
public class SurveyQuestionApplEntity {
    @Id
    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Id
    @Column(name = "SURVEY_QUESTION_ID")
    private String surveyQuestionId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SURVEY_PAGE_SEQ_ID")
    private String surveyPageSeqId;

    @Column(name = "SURVEY_MULTI_RESP_ID")
    private String surveyMultiRespId;

    @Column(name = "SURVEY_MULTI_RESP_COL_ID")
    private String surveyMultiRespColId;

    @Column(name = "REQUIRED_FIELD")
    private String requiredField;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "EXTERNAL_FIELD_REF")
    private String externalFieldRef;

    @Column(name = "WITH_SURVEY_QUESTION_ID")
    private String withSurveyQuestionId;

    @Column(name = "WITH_SURVEY_OPTION_SEQ_ID")
    private String withSurveyOptionSeqId;
}
