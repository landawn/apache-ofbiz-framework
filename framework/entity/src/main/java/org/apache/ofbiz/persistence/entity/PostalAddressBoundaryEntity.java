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
@Entity(name = "POSTAL_ADDRESS_BOUNDARY")
@Table(name = "POSTAL_ADDRESS_BOUNDARY")
public class PostalAddressBoundaryEntity {
    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Id
    @Column(name = "GEO_ID")
    private String geoId;
}
