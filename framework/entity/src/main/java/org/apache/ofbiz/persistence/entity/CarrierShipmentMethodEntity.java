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
@Entity(name = "CARRIER_SHIPMENT_METHOD")
@Table(name = "CARRIER_SHIPMENT_METHOD")
public class CarrierShipmentMethodEntity {
    @Id
    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "SEQUENCE_NUMBER")
    private BigDecimal sequenceNumber;

    @Column(name = "CARRIER_SERVICE_CODE")
    private String carrierServiceCode;
}
