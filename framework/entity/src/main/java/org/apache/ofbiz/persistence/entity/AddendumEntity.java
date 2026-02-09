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
@Entity(name = "ADDENDUM")
@Table(name = "ADDENDUM")
public class AddendumEntity {
    @Id
    @Column(name = "ADDENDUM_ID")
    private String addendumId;

    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Column(name = "ADDENDUM_CREATION_DATE")
    private Timestamp addendumCreationDate;

    @Column(name = "ADDENDUM_EFFECTIVE_DATE")
    private Timestamp addendumEffectiveDate;

    @Column(name = "ADDENDUM_TEXT")
    private String addendumText;
}
