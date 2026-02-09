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
@Entity(name = "AGREEMENT_PROMO_APPL")
@Table(name = "AGREEMENT_PROMO_APPL")
public class AgreementPromoApplEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Id
    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
