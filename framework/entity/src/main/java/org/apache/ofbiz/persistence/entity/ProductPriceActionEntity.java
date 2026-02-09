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
@Entity(name = "PRODUCT_PRICE_ACTION")
@Table(name = "PRODUCT_PRICE_ACTION")
public class ProductPriceActionEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_RULE_ID")
    private String productPriceRuleId;

    @Id
    @Column(name = "PRODUCT_PRICE_ACTION_SEQ_ID")
    private String productPriceActionSeqId;

    @Column(name = "PRODUCT_PRICE_ACTION_TYPE_ID")
    private String productPriceActionTypeId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "RATE_CODE")
    private String rateCode;
}
