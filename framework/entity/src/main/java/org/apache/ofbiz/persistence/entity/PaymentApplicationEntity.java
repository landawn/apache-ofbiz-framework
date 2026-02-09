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
@Entity(name = "PAYMENT_APPLICATION")
@Table(name = "PAYMENT_APPLICATION")
public class PaymentApplicationEntity {
    @Id
    @Column(name = "PAYMENT_APPLICATION_ID")
    private String paymentApplicationId;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "OVERRIDE_GL_ACCOUNT_ID")
    private String overrideGlAccountId;

    @Column(name = "TO_PAYMENT_ID")
    private String toPaymentId;

    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Column(name = "AMOUNT_APPLIED")
    private BigDecimal amountApplied;
}
