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
@Entity(name = "PRODUCT_PRICE")
@Table(name = "PRODUCT_PRICE")
public class ProductPriceEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PRODUCT_PRICE_TYPE_ID")
    private String productPriceTypeId;

    @Id
    @Column(name = "PRODUCT_PRICE_PURPOSE_ID")
    private String productPricePurposeId;

    @Id
    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Id
    @Column(name = "PRODUCT_STORE_GROUP_ID")
    private String productStoreGroupId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "TERM_UOM_ID")
    private String termUomId;

    @Column(name = "CUSTOM_PRICE_CALC_SERVICE")
    private String customPriceCalcService;

    @Column(name = "PRICE_WITHOUT_TAX")
    private BigDecimal priceWithoutTax;

    @Column(name = "PRICE_WITH_TAX")
    private BigDecimal priceWithTax;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Column(name = "TAX_IN_PRICE")
    private String taxInPrice;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
