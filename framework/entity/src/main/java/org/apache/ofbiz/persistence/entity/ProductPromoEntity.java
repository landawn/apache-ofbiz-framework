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
@Entity(name = "PRODUCT_PROMO")
@Table(name = "PRODUCT_PROMO")
public class ProductPromoEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Column(name = "PROMO_NAME")
    private String promoName;

    @Column(name = "PROMO_TEXT")
    private String promoText;

    @Column(name = "USER_ENTERED")
    private String userEntered;

    @Column(name = "SHOW_TO_CUSTOMER")
    private String showToCustomer;

    @Column(name = "REQUIRE_CODE")
    private String requireCode;

    @Column(name = "USE_LIMIT_PER_ORDER")
    private BigDecimal useLimitPerOrder;

    @Column(name = "USE_LIMIT_PER_CUSTOMER")
    private BigDecimal useLimitPerCustomer;

    @Column(name = "USE_LIMIT_PER_PROMOTION")
    private BigDecimal useLimitPerPromotion;

    @Column(name = "BILLBACK_FACTOR")
    private BigDecimal billbackFactor;

    @Column(name = "OVERRIDE_ORG_PARTY_ID")
    private String overrideOrgPartyId;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
