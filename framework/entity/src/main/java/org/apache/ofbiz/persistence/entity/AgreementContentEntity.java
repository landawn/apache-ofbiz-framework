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
@Entity(name = "AGREEMENT_CONTENT")
@Table(name = "AGREEMENT_CONTENT")
public class AgreementContentEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Id
    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Id
    @Column(name = "AGREEMENT_CONTENT_TYPE_ID")
    private String agreementContentTypeId;

    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
