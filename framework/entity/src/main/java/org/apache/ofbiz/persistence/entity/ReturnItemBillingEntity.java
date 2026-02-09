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
@Entity(name = "RETURN_ITEM_BILLING")
@Table(name = "RETURN_ITEM_BILLING")
public class ReturnItemBillingEntity {
    @Id
    @Column(name = "RETURN_ID")
    private String returnId;

    @Id
    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "SHIPMENT_RECEIPT_ID")
    private String shipmentReceiptId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
