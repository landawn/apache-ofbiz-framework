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
@Entity(name = "VENDOR")
@Table(name = "VENDOR")
public class VendorEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "MANIFEST_COMPANY_NAME")
    private String manifestCompanyName;

    @Column(name = "MANIFEST_COMPANY_TITLE")
    private String manifestCompanyTitle;

    @Column(name = "MANIFEST_LOGO_URL")
    private String manifestLogoUrl;

    @Column(name = "MANIFEST_POLICIES")
    private String manifestPolicies;
}
