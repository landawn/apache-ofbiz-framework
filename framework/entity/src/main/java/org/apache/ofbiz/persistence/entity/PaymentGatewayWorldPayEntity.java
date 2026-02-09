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
@Entity(name = "PAYMENT_GATEWAY_WORLD_PAY")
@Table(name = "PAYMENT_GATEWAY_WORLD_PAY")
public class PaymentGatewayWorldPayEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "REDIRECT_URL")
    private String redirectUrl;

    @Column(name = "INST_ID")
    private String instId;

    @Column(name = "AUTH_MODE")
    private String authMode;

    @Column(name = "FIX_CONTACT")
    private String fixContact;

    @Column(name = "HIDE_CONTACT")
    private String hideContact;

    @Column(name = "HIDE_CURRENCY")
    private String hideCurrency;

    @Column(name = "LANG_ID")
    private String langId;

    @Column(name = "NO_LANGUAGE_MENU")
    private String noLanguageMenu;

    @Column(name = "WITH_DELIVERY")
    private String withDelivery;

    @Column(name = "TEST_MODE")
    private BigDecimal testMode;
}
