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
@Entity(name = "PRODUCT_PROMO_CODE")
@Table(name = "PRODUCT_PROMO_CODE")
public class ProductPromoCodeEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_CODE_ID")
    private String productPromoCodeId;

    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Column(name = "USER_ENTERED")
    private String userEntered;

    @Column(name = "REQUIRE_EMAIL_OR_PARTY")
    private String requireEmailOrParty;

    @Column(name = "USE_LIMIT_PER_CODE")
    private BigDecimal useLimitPerCode;

    @Column(name = "USE_LIMIT_PER_CUSTOMER")
    private BigDecimal useLimitPerCustomer;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
