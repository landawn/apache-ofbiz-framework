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
@Entity(name = "TAX_AUTHORITY_RATE_PRODUCT")
@Table(name = "TAX_AUTHORITY_RATE_PRODUCT")
public class TaxAuthorityRateProductEntity {
    @Id
    @Column(name = "TAX_AUTHORITY_RATE_SEQ_ID")
    private String taxAuthorityRateSeqId;

    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Column(name = "TAX_AUTHORITY_RATE_TYPE_ID")
    private String taxAuthorityRateTypeId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "TITLE_TRANSFER_ENUM_ID")
    private String titleTransferEnumId;

    @Column(name = "MIN_ITEM_PRICE")
    private BigDecimal minItemPrice;

    @Column(name = "MIN_PURCHASE")
    private BigDecimal minPurchase;

    @Column(name = "TAX_SHIPPING")
    private String taxShipping;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_PROMOTIONS")
    private String taxPromotions;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_TAX_IN_SHIPPING_PRICE")
    private String isTaxInShippingPrice;
}
