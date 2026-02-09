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
@Entity(name = "PRODUCT_PROMO_COND")
@Table(name = "PRODUCT_PROMO_COND")
public class ProductPromoCondEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Id
    @Column(name = "PRODUCT_PROMO_RULE_ID")
    private String productPromoRuleId;

    @Id
    @Column(name = "PRODUCT_PROMO_COND_SEQ_ID")
    private String productPromoCondSeqId;

    @Column(name = "CUSTOM_METHOD_ID")
    private String customMethodId;

    @Column(name = "INPUT_PARAM_ENUM_ID")
    private String inputParamEnumId;

    @Column(name = "OPERATOR_ENUM_ID")
    private String operatorEnumId;

    @Column(name = "COND_VALUE")
    private String condValue;

    @Column(name = "OTHER_VALUE")
    private String otherValue;
}
