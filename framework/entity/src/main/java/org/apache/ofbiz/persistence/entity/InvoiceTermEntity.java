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
@Entity(name = "INVOICE_TERM")
@Table(name = "INVOICE_TERM")
public class InvoiceTermEntity {
    @Id
    @Column(name = "INVOICE_TERM_ID")
    private String invoiceTermId;

    @Column(name = "TERM_TYPE_ID")
    private String termTypeId;

    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "TERM_VALUE")
    private BigDecimal termValue;

    @Column(name = "TERM_DAYS")
    private BigDecimal termDays;

    @Column(name = "TEXT_VALUE")
    private String textValue;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "UOM_ID")
    private String uomId;
}
