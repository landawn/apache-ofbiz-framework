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
@Entity(name = "SURVEY_MULTI_RESP_COLUMN")
@Table(name = "SURVEY_MULTI_RESP_COLUMN")
public class SurveyMultiRespColumnEntity {
    @Id
    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Id
    @Column(name = "SURVEY_MULTI_RESP_ID")
    private String surveyMultiRespId;

    @Id
    @Column(name = "SURVEY_MULTI_RESP_COL_ID")
    private String surveyMultiRespColId;

    @Column(name = "COLUMN_TITLE")
    private String columnTitle;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
