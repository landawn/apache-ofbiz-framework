package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT_MANUFACTURING_RULE")
@Table(name = "PRODUCT_MANUFACTURING_RULE")
public class ProductManufacturingRuleEntity {
    @Id
    @Column(name = "RULE_ID")
    private String ruleId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_ID_FOR")
    private String productIdFor;

    @Column(name = "PRODUCT_ID_IN")
    private String productIdIn;

    @Column(name = "RULE_SEQ_ID")
    private String ruleSeqId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "PRODUCT_ID_IN_SUBST")
    private String productIdInSubst;

    @Column(name = "PRODUCT_FEATURE")
    private String productFeature;

    @Column(name = "RULE_OPERATOR")
    private String ruleOperator;

    @Column(name = "QUANTITY")
    private Double quantity;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
