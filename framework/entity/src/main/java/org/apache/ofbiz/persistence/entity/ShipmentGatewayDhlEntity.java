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
@Entity(name = "SHIPMENT_GATEWAY_DHL")
@Table(name = "SHIPMENT_GATEWAY_DHL")
public class ShipmentGatewayDhlEntity {
    @Id
    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "CONNECT_URL")
    private String connectUrl;

    @Column(name = "CONNECT_TIMEOUT")
    private BigDecimal connectTimeout;

    @Column(name = "HEAD_VERSION")
    private String headVersion;

    @Column(name = "HEAD_ACTION")
    private String headAction;

    @Column(name = "ACCESS_USER_ID")
    private String accessUserId;

    @Column(name = "ACCESS_PASSWORD")
    private String accessPassword;

    @Column(name = "ACCESS_ACCOUNT_NBR")
    private String accessAccountNbr;

    @Column(name = "ACCESS_SHIPPING_KEY")
    private String accessShippingKey;

    @Column(name = "LABEL_IMAGE_FORMAT")
    private String labelImageFormat;

    @Column(name = "RATE_ESTIMATE_TEMPLATE")
    private String rateEstimateTemplate;
}
