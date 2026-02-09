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
@Entity(name = "AGREEMENT_TERM")
@Table(name = "AGREEMENT_TERM")
public class AgreementTermEntity {
    @Id
    @Column(name = "AGREEMENT_TERM_ID")
    private String agreementTermId;

    @Column(name = "TERM_TYPE_ID")
    private String termTypeId;

    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Column(name = "INVOICE_ITEM_TYPE_ID")
    private String invoiceItemTypeId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "TERM_VALUE")
    private BigDecimal termValue;

    @Column(name = "TERM_DAYS")
    private BigDecimal termDays;

    @Column(name = "TEXT_VALUE")
    private String textValue;

    @Column(name = "MIN_QUANTITY")
    private Double minQuantity;

    @Column(name = "MAX_QUANTITY")
    private Double maxQuantity;

    @Column(name = "DESCRIPTION")
    private String description;
}
