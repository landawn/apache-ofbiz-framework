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
@Entity(name = "UNEMPLOYMENT_CLAIM")
@Table(name = "UNEMPLOYMENT_CLAIM")
public class UnemploymentClaimEntity {
    @Id
    @Column(name = "UNEMPLOYMENT_CLAIM_ID")
    private String unemploymentClaimId;

    @Column(name = "UNEMPLOYMENT_CLAIM_DATE")
    private Timestamp unemploymentClaimDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
