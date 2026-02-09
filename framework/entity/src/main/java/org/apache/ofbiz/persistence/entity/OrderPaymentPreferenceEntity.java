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
@Entity(name = "ORDER_PAYMENT_PREFERENCE")
@Table(name = "ORDER_PAYMENT_PREFERENCE")
public class OrderPaymentPreferenceEntity {
    @Id
    @Column(name = "ORDER_PAYMENT_PREFERENCE_ID")
    private String orderPaymentPreferenceId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "PRODUCT_PRICE_PURPOSE_ID")
    private String productPricePurposeId;

    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Column(name = "SECURITY_CODE")
    private String securityCode;

    @Column(name = "TRACK2")
    private String track2;

    @Column(name = "PRESENT_FLAG")
    private String presentFlag;

    @Column(name = "SWIPED_FLAG")
    private String swipedFlag;

    @Column(name = "OVERFLOW_FLAG")
    private String overflowFlag;

    @Column(name = "MAX_AMOUNT")
    private BigDecimal maxAmount;

    @Column(name = "PROCESS_ATTEMPT")
    private BigDecimal processAttempt;

    @Column(name = "BILLING_POSTAL_CODE")
    private String billingPostalCode;

    @Column(name = "MANUAL_AUTH_CODE")
    private String manualAuthCode;

    @Column(name = "MANUAL_REF_NUM")
    private String manualRefNum;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "NEEDS_NSF_RETRY")
    private String needsNsfRetry;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
