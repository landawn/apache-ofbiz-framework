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
@Entity(name = "ORDER_ITEM_PRICE_INFO")
@Table(name = "ORDER_ITEM_PRICE_INFO")
public class OrderItemPriceInfoEntity {
    @Id
    @Column(name = "ORDER_ITEM_PRICE_INFO_ID")
    private String orderItemPriceInfoId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "PRODUCT_PRICE_RULE_ID")
    private String productPriceRuleId;

    @Column(name = "PRODUCT_PRICE_ACTION_SEQ_ID")
    private String productPriceActionSeqId;

    @Column(name = "MODIFY_AMOUNT")
    private BigDecimal modifyAmount;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "RATE_CODE")
    private String rateCode;
}
