package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "AGREEMENT_ITEM")
@Table(name = "AGREEMENT_ITEM")
public class AgreementItemEntity {
    @Id
    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Id
    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Column(name = "AGREEMENT_ITEM_TYPE_ID")
    private String agreementItemTypeId;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "AGREEMENT_TEXT")
    private String agreementText;

    @Column(name = "AGREEMENT_IMAGE")
    private byte[] agreementImage;
}
