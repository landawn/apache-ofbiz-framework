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
@Entity(name = "SURVEY_MULTI_RESP")
@Table(name = "SURVEY_MULTI_RESP")
public class SurveyMultiRespEntity {
    @Id
    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Id
    @Column(name = "SURVEY_MULTI_RESP_ID")
    private String surveyMultiRespId;

    @Column(name = "MULTI_RESP_TITLE")
    private String multiRespTitle;
}
