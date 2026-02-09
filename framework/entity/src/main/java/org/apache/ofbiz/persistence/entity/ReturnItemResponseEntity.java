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
@Entity(name = "RETURN_ITEM_RESPONSE")
@Table(name = "RETURN_ITEM_RESPONSE")
public class ReturnItemResponseEntity {
    @Id
    @Column(name = "RETURN_ITEM_RESPONSE_ID")
    private String returnItemResponseId;

    @Column(name = "ORDER_PAYMENT_PREFERENCE_ID")
    private String orderPaymentPreferenceId;

    @Column(name = "REPLACEMENT_ORDER_ID")
    private String replacementOrderId;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "FIN_ACCOUNT_TRANS_ID")
    private String finAccountTransId;

    @Column(name = "RESPONSE_AMOUNT")
    private BigDecimal responseAmount;

    @Column(name = "RESPONSE_DATE")
    private Timestamp responseDate;
}
