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
@Entity(name = "PAYMENT_GATEWAY_RESPONSE")
@Table(name = "PAYMENT_GATEWAY_RESPONSE")
public class PaymentGatewayResponseEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_RESPONSE_ID")
    private String paymentGatewayResponseId;

    @Column(name = "PAYMENT_SERVICE_TYPE_ENUM_ID")
    private String paymentServiceTypeEnumId;

    @Column(name = "ORDER_PAYMENT_PREFERENCE_ID")
    private String orderPaymentPreferenceId;

    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "TRANS_CODE_ENUM_ID")
    private String transCodeEnumId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "REFERENCE_NUM")
    private String referenceNum;

    @Column(name = "ALT_REFERENCE")
    private String altReference;

    @Column(name = "SUB_REFERENCE")
    private String subReference;

    @Column(name = "GATEWAY_CODE")
    private String gatewayCode;

    @Column(name = "GATEWAY_FLAG")
    private String gatewayFlag;

    @Column(name = "GATEWAY_AVS_RESULT")
    private String gatewayAvsResult;

    @Column(name = "GATEWAY_CV_RESULT")
    private String gatewayCvResult;

    @Column(name = "GATEWAY_SCORE_RESULT")
    private String gatewayScoreResult;

    @Column(name = "GATEWAY_MESSAGE")
    private String gatewayMessage;

    @Column(name = "TRANSACTION_DATE")
    private Timestamp transactionDate;

    @Column(name = "RESULT_DECLINED")
    private String resultDeclined;

    @Column(name = "RESULT_NSF")
    private String resultNsf;

    @Column(name = "RESULT_BAD_EXPIRE")
    private String resultBadExpire;

    @Column(name = "RESULT_BAD_CARD_NUMBER")
    private String resultBadCardNumber;
}
