package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT_PRICE_CHANGE")
@Table(name = "PRODUCT_PRICE_CHANGE")
public class ProductPriceChangeEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_CHANGE_ID")
    private String productPriceChangeId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_PRICE_TYPE_ID")
    private String productPriceTypeId;

    @Column(name = "PRODUCT_PRICE_PURPOSE_ID")
    private String productPricePurposeId;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "PRODUCT_STORE_GROUP_ID")
    private String productStoreGroupId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "OLD_PRICE")
    private BigDecimal oldPrice;

    @Column(name = "CHANGED_DATE")
    private Timestamp changedDate;

    @Column(name = "CHANGED_BY_USER_LOGIN")
    private String changedByUserLogin;
}
