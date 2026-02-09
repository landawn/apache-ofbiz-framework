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
@Entity(name = "TAX_AUTHORITY")
@Table(name = "TAX_AUTHORITY")
public class TaxAuthorityEntity {
    @Id
    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Id
    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Column(name = "REQUIRE_TAX_ID_FOR_EXEMPTION")
    private String requireTaxIdForExemption;

    @Column(name = "TAX_ID_FORMAT_PATTERN")
    private String taxIdFormatPattern;

    @Column(name = "INCLUDE_TAX_IN_PRICE")
    private String includeTaxInPrice;
}
