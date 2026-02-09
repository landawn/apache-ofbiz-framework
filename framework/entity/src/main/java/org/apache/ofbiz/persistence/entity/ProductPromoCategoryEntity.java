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
@Entity(name = "PRODUCT_PROMO_CATEGORY")
@Table(name = "PRODUCT_PROMO_CATEGORY")
public class ProductPromoCategoryEntity {
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
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Id
    @Column(name = "AND_GROUP_ID")
    private String andGroupId;

    @Column(name = "PRODUCT_PROMO_APPL_ENUM_ID")
    private String productPromoApplEnumId;

    @Column(name = "INCLUDE_SUB_CATEGORIES")
    private String includeSubCategories;
}
