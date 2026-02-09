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
@Entity(name = "PRODUCT_PROMO_PRODUCT")
@Table(name = "PRODUCT_PROMO_PRODUCT")
public class ProductPromoProductEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Id
    @Column(name = "PRODUCT_PROMO_RULE_ID")
    private String productPromoRuleId;

    @Id
    @Column(name = "PRODUCT_PROMO_ACTION_SEQ_ID")
    private String productPromoActionSeqId;

    @Id
    @Column(name = "PRODUCT_PROMO_COND_SEQ_ID")
    private String productPromoCondSeqId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_PROMO_APPL_ENUM_ID")
    private String productPromoApplEnumId;
}
