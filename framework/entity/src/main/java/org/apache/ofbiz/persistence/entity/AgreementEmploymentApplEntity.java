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
@Entity(name = "AGREEMENT_EMPLOYMENT_APPL")
@Table(name = "AGREEMENT_EMPLOYMENT_APPL")
public class AgreementEmploymentApplEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Id
    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Id
    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Id
    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Id
    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Id
    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "AGREEMENT_DATE")
    private Timestamp agreementDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
