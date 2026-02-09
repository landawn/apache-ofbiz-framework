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
@Entity(name = "PAYMENT_GATEWAY_EWAY")
@Table(name = "PAYMENT_GATEWAY_EWAY")
public class PaymentGatewayEwayEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "CUSTOMER_ID")
    private String customerId;

    @Column(name = "REFUND_PWD")
    private String refundPwd;

    @Column(name = "TEST_MODE")
    private String testMode;

    @Column(name = "ENABLE_CVN")
    private String enableCvn;

    @Column(name = "ENABLE_BEAGLE")
    private String enableBeagle;
}
