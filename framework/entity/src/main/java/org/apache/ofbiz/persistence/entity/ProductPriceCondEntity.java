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
@Entity(name = "PRODUCT_PRICE_COND")
@Table(name = "PRODUCT_PRICE_COND")
public class ProductPriceCondEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_RULE_ID")
    private String productPriceRuleId;

    @Id
    @Column(name = "PRODUCT_PRICE_COND_SEQ_ID")
    private String productPriceCondSeqId;

    @Column(name = "INPUT_PARAM_ENUM_ID")
    private String inputParamEnumId;

    @Column(name = "OPERATOR_ENUM_ID")
    private String operatorEnumId;

    @Column(name = "COND_VALUE")
    private String condValue;
}
