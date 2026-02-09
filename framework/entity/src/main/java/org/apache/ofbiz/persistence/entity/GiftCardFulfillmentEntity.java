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
@Entity(name = "GIFT_CARD_FULFILLMENT")
@Table(name = "GIFT_CARD_FULFILLMENT")
public class GiftCardFulfillmentEntity {
    @Id
    @Column(name = "FULFILLMENT_ID")
    private String fulfillmentId;

    @Column(name = "TYPE_ENUM_ID")
    private String typeEnumId;

    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "SURVEY_RESPONSE_ID")
    private String surveyResponseId;

    @Column(name = "CARD_NUMBER")
    private String cardNumber;

    @Column(name = "PIN_NUMBER")
    private String pinNumber;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "RESPONSE_CODE")
    private String responseCode;

    @Column(name = "REFERENCE_NUM")
    private String referenceNum;

    @Column(name = "AUTH_CODE")
    private String authCode;

    @Column(name = "FULFILLMENT_DATE")
    private Timestamp fulfillmentDate;
}
