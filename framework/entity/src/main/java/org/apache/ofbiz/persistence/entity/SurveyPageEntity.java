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
@Entity(name = "SURVEY_PAGE")
@Table(name = "SURVEY_PAGE")
public class SurveyPageEntity {
    @Id
    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Id
    @Column(name = "SURVEY_PAGE_SEQ_ID")
    private String surveyPageSeqId;

    @Column(name = "PAGE_NAME")
    private String pageName;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
