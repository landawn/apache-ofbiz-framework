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
@Entity(name = "TAX_AUTHORITY_ASSOC")
@Table(name = "TAX_AUTHORITY_ASSOC")
public class TaxAuthorityAssocEntity {
    @Id
    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Id
    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Id
    @Column(name = "TO_TAX_AUTH_GEO_ID")
    private String toTaxAuthGeoId;

    @Id
    @Column(name = "TO_TAX_AUTH_PARTY_ID")
    private String toTaxAuthPartyId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "TAX_AUTHORITY_ASSOC_TYPE_ID")
    private String taxAuthorityAssocTypeId;
}
