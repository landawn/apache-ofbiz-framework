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
@Entity(name = "PAYMENT_GATEWAY_PAY_PAL")
@Table(name = "PAYMENT_GATEWAY_PAY_PAL")
public class PaymentGatewayPayPalEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "BUSINESS_EMAIL")
    private String businessEmail;

    @Column(name = "API_USER_NAME")
    private String apiUserName;

    @Column(name = "API_PASSWORD")
    private String apiPassword;

    @Column(name = "API_SIGNATURE")
    private String apiSignature;

    @Column(name = "API_ENVIRONMENT")
    private String apiEnvironment;

    @Column(name = "NOTIFY_URL")
    private String notifyUrl;

    @Column(name = "RETURN_URL")
    private String returnUrl;

    @Column(name = "CANCEL_RETURN_URL")
    private String cancelReturnUrl;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Column(name = "CONFIRM_TEMPLATE")
    private String confirmTemplate;

    @Column(name = "REDIRECT_URL")
    private String redirectUrl;

    @Column(name = "CONFIRM_URL")
    private String confirmUrl;

    @Column(name = "SHIPPING_CALLBACK_URL")
    private String shippingCallbackUrl;

    @Column(name = "REQUIRE_CONFIRMED_SHIPPING")
    private String requireConfirmedShipping;
}
