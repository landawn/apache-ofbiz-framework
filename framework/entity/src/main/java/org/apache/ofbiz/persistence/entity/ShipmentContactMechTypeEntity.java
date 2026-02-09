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
@Entity(name = "SHIPMENT_CONTACT_MECH_TYPE")
@Table(name = "SHIPMENT_CONTACT_MECH_TYPE")
public class ShipmentContactMechTypeEntity {
    @Id
    @Column(name = "SHIPMENT_CONTACT_MECH_TYPE_ID")
    private String shipmentContactMechTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
