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
@Entity(name = "AGREEMENT")
@Table(name = "AGREEMENT")
public class AgreementEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Column(name = "AGREEMENT_TYPE_ID")
    private String agreementTypeId;

    @Column(name = "AGREEMENT_DATE")
    private Timestamp agreementDate;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TEXT_DATA")
    private String textData;

    @Column(name = "STATUS_ID")
    private String statusId;
}
