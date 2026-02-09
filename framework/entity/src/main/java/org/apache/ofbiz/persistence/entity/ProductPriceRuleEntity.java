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
@Entity(name = "PRODUCT_PRICE_RULE")
@Table(name = "PRODUCT_PRICE_RULE")
public class ProductPriceRuleEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_RULE_ID")
    private String productPriceRuleId;

    @Column(name = "RULE_NAME")
    private String ruleName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_SALE")
    private String isSale;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
