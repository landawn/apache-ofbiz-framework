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
@Entity(name = "PRODUCT_PROMO_USE")
@Table(name = "PRODUCT_PROMO_USE")
public class ProductPromoUseEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "PROMO_SEQUENCE_ID")
    private String promoSequenceId;

    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Column(name = "PRODUCT_PROMO_CODE_ID")
    private String productPromoCodeId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "TOTAL_DISCOUNT_AMOUNT")
    private BigDecimal totalDiscountAmount;

    @Column(name = "QUANTITY_LEFT_IN_ACTIONS")
    private BigDecimal quantityLeftInActions;
}
