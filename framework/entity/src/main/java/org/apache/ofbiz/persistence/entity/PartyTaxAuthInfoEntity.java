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
@Entity(name = "PARTY_TAX_AUTH_INFO")
@Table(name = "PARTY_TAX_AUTH_INFO")
public class PartyTaxAuthInfoEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Id
    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PARTY_TAX_ID")
    private String partyTaxId;

    @Column(name = "IS_EXEMPT")
    private String isExempt;

    @Column(name = "IS_NEXUS")
    private String isNexus;
}
