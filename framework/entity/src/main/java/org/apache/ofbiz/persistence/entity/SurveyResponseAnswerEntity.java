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
@Entity(name = "SURVEY_RESPONSE_ANSWER")
@Table(name = "SURVEY_RESPONSE_ANSWER")
public class SurveyResponseAnswerEntity {
    @Id
    @Column(name = "SURVEY_RESPONSE_ID")
    private String surveyResponseId;

    @Id
    @Column(name = "SURVEY_QUESTION_ID")
    private String surveyQuestionId;

    @Id
    @Column(name = "SURVEY_MULTI_RESP_COL_ID")
    private String surveyMultiRespColId;

    @Column(name = "SURVEY_MULTI_RESP_ID")
    private String surveyMultiRespId;

    @Column(name = "BOOLEAN_RESPONSE")
    private String booleanResponse;

    @Column(name = "CURRENCY_RESPONSE")
    private BigDecimal currencyResponse;

    @Column(name = "FLOAT_RESPONSE")
    private Double floatResponse;

    @Column(name = "NUMERIC_RESPONSE")
    private BigDecimal numericResponse;

    @Column(name = "TEXT_RESPONSE")
    private String textResponse;

    @Column(name = "SURVEY_OPTION_SEQ_ID")
    private String surveyOptionSeqId;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "ANSWERED_DATE")
    private Timestamp answeredDate;

    @Column(name = "AMOUNT_BASE")
    private BigDecimal amountBase;

    @Column(name = "AMOUNT_BASE_UOM_ID")
    private String amountBaseUomId;

    @Column(name = "WEIGHT_FACTOR")
    private Double weightFactor;

    @Column(name = "DURATION")
    private BigDecimal duration;

    @Column(name = "DURATION_UOM_ID")
    private String durationUomId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
