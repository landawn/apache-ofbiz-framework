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
@Entity(name = "ORDER_ITEM_BILLING")
@Table(name = "ORDER_ITEM_BILLING")
public class OrderItemBillingEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "ITEM_ISSUANCE_ID")
    private String itemIssuanceId;

    @Column(name = "SHIPMENT_RECEIPT_ID")
    private String shipmentReceiptId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
