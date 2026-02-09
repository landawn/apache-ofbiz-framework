package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "POSTAL_ADDRESS")
@Table(name = "POSTAL_ADDRESS")
public class PostalAddressEntity {
    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "TO_NAME")
    private String toName;

    @Column(name = "ATTN_NAME")
    private String attnName;

    @Column(name = "ADDRESS1")
    private String address1;

    @Column(name = "ADDRESS2")
    private String address2;

    @Column(name = "HOUSE_NUMBER")
    private BigDecimal houseNumber;

    @Column(name = "HOUSE_NUMBER_EXT")
    private String houseNumberExt;

    @Column(name = "DIRECTIONS")
    private String directions;

    @Column(name = "CITY")
    private String city;

    @Column(name = "CITY_GEO_ID")
    private String cityGeoId;

    @Column(name = "POSTAL_CODE")
    private String postalCode;

    @Column(name = "POSTAL_CODE_EXT")
    private String postalCodeExt;

    @Column(name = "COUNTRY_GEO_ID")
    private String countryGeoId;

    @Column(name = "STATE_PROVINCE_GEO_ID")
    private String stateProvinceGeoId;

    @Column(name = "COUNTY_GEO_ID")
    private String countyGeoId;

    @Column(name = "MUNICIPALITY_GEO_ID")
    private String municipalityGeoId;

    @Column(name = "POSTAL_CODE_GEO_ID")
    private String postalCodeGeoId;

    @Column(name = "GEO_POINT_ID")
    private String geoPointId;
}
