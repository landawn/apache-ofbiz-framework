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
@Entity(name = "CONTENT_APPROVAL")
@Table(name = "CONTENT_APPROVAL")
public class ContentApprovalEntity {
    @Id
    @Column(name = "CONTENT_APPROVAL_ID")
    private String contentApprovalId;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "CONTENT_REVISION_SEQ_ID")
    private String contentRevisionSeqId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "APPROVAL_STATUS_ID")
    private String approvalStatusId;

    @Column(name = "APPROVAL_DATE")
    private Timestamp approvalDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "COMMENTS")
    private String comments;
}
