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
@Entity(name = "SHIPMENT_GATEWAY_USPS")
@Table(name = "SHIPMENT_GATEWAY_USPS")
public class ShipmentGatewayUspsEntity {
    @Id
    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "CONNECT_URL")
    private String connectUrl;

    @Column(name = "CONNECT_URL_LABELS")
    private String connectUrlLabels;

    @Column(name = "CONNECT_TIMEOUT")
    private BigDecimal connectTimeout;

    @Column(name = "ACCESS_USER_ID")
    private String accessUserId;

    @Column(name = "ACCESS_PASSWORD")
    private String accessPassword;

    @Column(name = "MAX_ESTIMATE_WEIGHT")
    private BigDecimal maxEstimateWeight;

    @Column(name = "TEST")
    private String test;
}
