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
@Entity(name = "SHIPMENT_GATEWAY_FEDEX")
@Table(name = "SHIPMENT_GATEWAY_FEDEX")
public class ShipmentGatewayFedexEntity {
    @Id
    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "CONNECT_URL")
    private String connectUrl;

    @Column(name = "CONNECT_SOAP_URL")
    private String connectSoapUrl;

    @Column(name = "CONNECT_TIMEOUT")
    private BigDecimal connectTimeout;

    @Column(name = "ACCESS_ACCOUNT_NBR")
    private String accessAccountNbr;

    @Column(name = "ACCESS_METER_NUMBER")
    private String accessMeterNumber;

    @Column(name = "ACCESS_USER_KEY")
    private String accessUserKey;

    @Column(name = "ACCESS_USER_PWD")
    private String accessUserPwd;

    @Column(name = "LABEL_IMAGE_TYPE")
    private String labelImageType;

    @Column(name = "DEFAULT_DROPOFF_TYPE")
    private String defaultDropoffType;

    @Column(name = "DEFAULT_PACKAGING_TYPE")
    private String defaultPackagingType;

    @Column(name = "TEMPLATE_SHIPMENT")
    private String templateShipment;

    @Column(name = "TEMPLATE_SUBSCRIPTION")
    private String templateSubscription;

    @Column(name = "RATE_ESTIMATE_TEMPLATE")
    private String rateEstimateTemplate;
}
