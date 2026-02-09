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
@Entity(name = "TAX_AUTHORITY_CATEGORY")
@Table(name = "TAX_AUTHORITY_CATEGORY")
public class TaxAuthorityCategoryEntity {
    @Id
    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Id
    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Id
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;
}
