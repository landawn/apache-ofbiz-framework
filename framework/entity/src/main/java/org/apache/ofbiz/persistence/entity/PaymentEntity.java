package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PAYMENT")
@Table(name = "PAYMENT")
public class PaymentEntity {
    @Id
    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "PAYMENT_TYPE_ID")
    private String paymentTypeId;

    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "PAYMENT_GATEWAY_RESPONSE_ID")
    private String paymentGatewayResponseId;

    @Column(name = "PAYMENT_PREFERENCE_ID")
    private String paymentPreferenceId;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "EFFECTIVE_DATE")
    private Timestamp effectiveDate;

    @Column(name = "PAYMENT_REF_NUM")
    private String paymentRefNum;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "FIN_ACCOUNT_TRANS_ID")
    private String finAccountTransId;

    @Column(name = "OVERRIDE_GL_ACCOUNT_ID")
    private String overrideGlAccountId;

    @Column(name = "ACTUAL_CURRENCY_AMOUNT")
    private BigDecimal actualCurrencyAmount;

    @Column(name = "ACTUAL_CURRENCY_UOM_ID")
    private String actualCurrencyUomId;
}
