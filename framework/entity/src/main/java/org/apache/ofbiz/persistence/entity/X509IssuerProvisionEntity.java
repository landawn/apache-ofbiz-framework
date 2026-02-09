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
@Entity(name = "X509_ISSUER_PROVISION")
@Table(name = "X509_ISSUER_PROVISION")
public class X509IssuerProvisionEntity {
    @Id
    @Column(name = "CERT_PROVISION_ID")
    private String certProvisionId;

    @Column(name = "COMMON_NAME")
    private String commonName;

    @Column(name = "ORGANIZATIONAL_UNIT")
    private String organizationalUnit;

    @Column(name = "ORGANIZATION_NAME")
    private String organizationName;

    @Column(name = "CITY_LOCALITY")
    private String cityLocality;

    @Column(name = "STATE_PROVINCE")
    private String stateProvince;

    @Column(name = "COUNTRY")
    private String country;

    @Column(name = "SERIAL_NUMBER")
    private String serialNumber;
}
