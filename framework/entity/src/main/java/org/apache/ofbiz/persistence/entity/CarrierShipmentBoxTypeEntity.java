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
@Entity(name = "CARRIER_SHIPMENT_BOX_TYPE")
@Table(name = "CARRIER_SHIPMENT_BOX_TYPE")
public class CarrierShipmentBoxTypeEntity {
    @Id
    @Column(name = "SHIPMENT_BOX_TYPE_ID")
    private String shipmentBoxTypeId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "PACKAGING_TYPE_CODE")
    private String packagingTypeCode;

    @Column(name = "OVERSIZE_CODE")
    private String oversizeCode;
}
