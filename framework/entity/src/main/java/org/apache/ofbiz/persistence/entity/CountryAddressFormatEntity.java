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
@Entity(name = "COUNTRY_ADDRESS_FORMAT")
@Table(name = "COUNTRY_ADDRESS_FORMAT")
public class CountryAddressFormatEntity {
    @Id
    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "GEO_ASSOC_TYPE_ID")
    private String geoAssocTypeId;

    @Column(name = "REQUIRE_STATE_PROVINCE_ID")
    private String requireStateProvinceId;

    @Column(name = "REQUIRE_POSTAL_CODE")
    private String requirePostalCode;

    @Column(name = "POSTAL_CODE_REGEX")
    private String postalCodeRegex;

    @Column(name = "HAS_POSTAL_CODE_EXT")
    private String hasPostalCodeExt;

    @Column(name = "REQUIRE_POSTAL_CODE_EXT")
    private String requirePostalCodeExt;

    @Column(name = "ADDRESS_FORMAT")
    private String addressFormat;
}
