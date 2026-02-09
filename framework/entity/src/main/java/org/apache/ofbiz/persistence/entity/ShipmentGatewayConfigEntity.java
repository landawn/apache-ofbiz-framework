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
@Entity(name = "SHIPMENT_GATEWAY_CONFIG")
@Table(name = "SHIPMENT_GATEWAY_CONFIG")
public class ShipmentGatewayConfigEntity {
    @Id
    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "SHIPMENT_GATEWAY_CONF_TYPE_ID")
    private String shipmentGatewayConfTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
