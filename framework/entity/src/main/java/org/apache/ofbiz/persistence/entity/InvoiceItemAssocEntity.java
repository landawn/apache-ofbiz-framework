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
@Entity(name = "INVOICE_ITEM_ASSOC")
@Table(name = "INVOICE_ITEM_ASSOC")
public class InvoiceItemAssocEntity {
    @Id
    @Column(name = "INVOICE_ID_FROM")
    private String invoiceIdFrom;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID_FROM")
    private String invoiceItemSeqIdFrom;

    @Id
    @Column(name = "INVOICE_ID_TO")
    private String invoiceIdTo;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID_TO")
    private String invoiceItemSeqIdTo;

    @Id
    @Column(name = "INVOICE_ITEM_ASSOC_TYPE_ID")
    private String invoiceItemAssocTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
