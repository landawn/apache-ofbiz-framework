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
@Entity(name = "PAY_PAL_PAYMENT_METHOD")
@Table(name = "PAY_PAL_PAYMENT_METHOD")
public class PayPalPaymentMethodEntity {
    @Id
    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "PAYER_ID")
    private String payerId;

    @Column(name = "EXPRESS_CHECKOUT_TOKEN")
    private String expressCheckoutToken;

    @Column(name = "PAYER_STATUS")
    private String payerStatus;

    @Column(name = "AVS_ADDR")
    private String avsAddr;

    @Column(name = "AVS_ZIP")
    private String avsZip;

    @Column(name = "CORRELATION_ID")
    private String correlationId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;
}
