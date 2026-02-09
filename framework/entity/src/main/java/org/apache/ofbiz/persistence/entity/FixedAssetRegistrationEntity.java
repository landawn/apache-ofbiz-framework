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
@Entity(name = "FIXED_ASSET_REGISTRATION")
@Table(name = "FIXED_ASSET_REGISTRATION")
public class FixedAssetRegistrationEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "REGISTRATION_DATE")
    private Timestamp registrationDate;

    @Column(name = "GOV_AGENCY_PARTY_ID")
    private String govAgencyPartyId;

    @Column(name = "REGISTRATION_NUMBER")
    private String registrationNumber;

    @Column(name = "LICENSE_NUMBER")
    private String licenseNumber;
}
