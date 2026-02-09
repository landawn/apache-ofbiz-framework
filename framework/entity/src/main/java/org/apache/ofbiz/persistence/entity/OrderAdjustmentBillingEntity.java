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
@Entity(name = "ORDER_ADJUSTMENT_BILLING")
@Table(name = "ORDER_ADJUSTMENT_BILLING")
public class OrderAdjustmentBillingEntity {
    @Id
    @Column(name = "ORDER_ADJUSTMENT_ID")
    private String orderAdjustmentId;

    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
