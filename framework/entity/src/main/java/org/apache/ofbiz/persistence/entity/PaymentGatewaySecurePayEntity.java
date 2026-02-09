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
@Entity(name = "PAYMENT_GATEWAY_SECURE_PAY")
@Table(name = "PAYMENT_GATEWAY_SECURE_PAY")
public class PaymentGatewaySecurePayEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "PWD")
    private String pwd;

    @Column(name = "SERVER_U_R_L")
    private String serverURL;

    @Column(name = "PROCESS_TIMEOUT")
    private BigDecimal processTimeout;

    @Column(name = "ENABLE_AMOUNT_ROUND")
    private String enableAmountRound;
}
