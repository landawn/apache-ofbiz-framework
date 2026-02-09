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
@Entity(name = "PRODUCT_PROMO_RULE")
@Table(name = "PRODUCT_PROMO_RULE")
public class ProductPromoRuleEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Id
    @Column(name = "PRODUCT_PROMO_RULE_ID")
    private String productPromoRuleId;

    @Column(name = "RULE_NAME")
    private String ruleName;
}
