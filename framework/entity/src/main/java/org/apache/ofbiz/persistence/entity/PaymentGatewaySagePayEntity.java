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
@Entity(name = "PAYMENT_GATEWAY_SAGE_PAY")
@Table(name = "PAYMENT_GATEWAY_SAGE_PAY")
public class PaymentGatewaySagePayEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "VENDOR")
    private String vendor;

    @Column(name = "PRODUCTION_HOST")
    private String productionHost;

    @Column(name = "TESTING_HOST")
    private String testingHost;

    @Column(name = "SAGE_PAY_MODE")
    private String sagePayMode;

    @Column(name = "PROTOCOL_VERSION")
    private String protocolVersion;

    @Column(name = "AUTHENTICATION_TRANS_TYPE")
    private String authenticationTransType;

    @Column(name = "AUTHENTICATION_URL")
    private String authenticationUrl;

    @Column(name = "AUTHORISE_TRANS_TYPE")
    private String authoriseTransType;

    @Column(name = "AUTHORISE_URL")
    private String authoriseUrl;

    @Column(name = "RELEASE_TRANS_TYPE")
    private String releaseTransType;

    @Column(name = "RELEASE_URL")
    private String releaseUrl;

    @Column(name = "VOID_URL")
    private String voidUrl;

    @Column(name = "REFUND_URL")
    private String refundUrl;
}
