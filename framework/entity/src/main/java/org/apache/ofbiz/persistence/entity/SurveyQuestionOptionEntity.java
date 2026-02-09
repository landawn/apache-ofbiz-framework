package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SURVEY_QUESTION_OPTION")
@Table(name = "SURVEY_QUESTION_OPTION")
public class SurveyQuestionOptionEntity {
    @Id
    @Column(name = "SURVEY_QUESTION_ID")
    private String surveyQuestionId;

    @Id
    @Column(name = "SURVEY_OPTION_SEQ_ID")
    private String surveyOptionSeqId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

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
}
