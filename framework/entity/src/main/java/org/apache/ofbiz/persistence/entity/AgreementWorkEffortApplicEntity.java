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
@Entity(name = "AGREEMENT_WORK_EFFORT_APPLIC")
@Table(name = "AGREEMENT_WORK_EFFORT_APPLIC")
public class AgreementWorkEffortApplicEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Id
    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;
}
