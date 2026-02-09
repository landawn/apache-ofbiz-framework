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
@Entity(name = "PAYMENT_GATEWAY_ORBITAL")
@Table(name = "PAYMENT_GATEWAY_ORBITAL")
public class PaymentGatewayOrbitalEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "CONNECTION_PASSWORD")
    private String connectionPassword;

    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "ENGINE_CLASS")
    private String engineClass;

    @Column(name = "HOST_NAME")
    private String hostName;

    @Column(name = "PORT")
    private BigDecimal port;

    @Column(name = "HOST_NAME_FAILOVER")
    private String hostNameFailover;

    @Column(name = "PORT_FAILOVER")
    private BigDecimal portFailover;

    @Column(name = "CONNECTION_TIMEOUT_SECONDS")
    private BigDecimal connectionTimeoutSeconds;

    @Column(name = "READ_TIMEOUT_SECONDS")
    private BigDecimal readTimeoutSeconds;

    @Column(name = "AUTHORIZATION_U_R_I")
    private String authorizationURI;

    @Column(name = "SDK_VERSION")
    private String sdkVersion;

    @Column(name = "SSL_SOCKET_FACTORY")
    private String sslSocketFactory;

    @Column(name = "RESPONSE_TYPE")
    private String responseType;
}
