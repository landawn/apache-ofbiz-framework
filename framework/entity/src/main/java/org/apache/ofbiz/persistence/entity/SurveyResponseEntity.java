package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SURVEY_RESPONSE")
@Table(name = "SURVEY_RESPONSE")
public class SurveyResponseEntity {
    @Id
    @Column(name = "SURVEY_RESPONSE_ID")
    private String surveyResponseId;

    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "RESPONSE_DATE")
    private Timestamp responseDate;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "REFERENCE_ID")
    private String referenceId;

    @Column(name = "GENERAL_FEEDBACK")
    private String generalFeedback;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "STATUS_ID")
    private String statusId;
}
