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
@Entity(name = "PRODUCT_PROMO_ACTION")
@Table(name = "PRODUCT_PROMO_ACTION")
public class ProductPromoActionEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Id
    @Column(name = "PRODUCT_PROMO_RULE_ID")
    private String productPromoRuleId;

    @Id
    @Column(name = "PRODUCT_PROMO_ACTION_SEQ_ID")
    private String productPromoActionSeqId;

    @Column(name = "PRODUCT_PROMO_ACTION_ENUM_ID")
    private String productPromoActionEnumId;

    @Column(name = "CUSTOM_METHOD_ID")
    private String customMethodId;

    @Column(name = "ORDER_ADJUSTMENT_TYPE_ID")
    private String orderAdjustmentTypeId;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "USE_CART_QUANTITY")
    private String useCartQuantity;
}
