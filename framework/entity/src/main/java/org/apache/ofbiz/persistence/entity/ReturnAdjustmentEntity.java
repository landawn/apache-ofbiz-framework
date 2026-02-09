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
@Entity(name = "RETURN_ADJUSTMENT")
@Table(name = "RETURN_ADJUSTMENT")
public class ReturnAdjustmentEntity {
    @Id
    @Column(name = "RETURN_ADJUSTMENT_ID")
    private String returnAdjustmentId;

    @Column(name = "RETURN_ADJUSTMENT_TYPE_ID")
    private String returnAdjustmentTypeId;

    @Column(name = "RETURN_ID")
    private String returnId;

    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "RETURN_TYPE_ID")
    private String returnTypeId;

    @Column(name = "ORDER_ADJUSTMENT_ID")
    private String orderAdjustmentId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PRODUCT_PROMO_ID")
    private String productPromoId;

    @Column(name = "PRODUCT_PROMO_RULE_ID")
    private String productPromoRuleId;

    @Column(name = "PRODUCT_PROMO_ACTION_SEQ_ID")
    private String productPromoActionSeqId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "CORRESPONDING_PRODUCT_ID")
    private String correspondingProductId;

    @Column(name = "TAX_AUTHORITY_RATE_SEQ_ID")
    private String taxAuthorityRateSeqId;

    @Column(name = "SOURCE_REFERENCE_ID")
    private String sourceReferenceId;

    @Column(name = "SOURCE_PERCENTAGE")
    private BigDecimal sourcePercentage;

    @Column(name = "CUSTOMER_REFERENCE_ID")
    private String customerReferenceId;

    @Column(name = "PRIMARY_GEO_ID")
    private String primaryGeoId;

    @Column(name = "SECONDARY_GEO_ID")
    private String secondaryGeoId;

    @Column(name = "EXEMPT_AMOUNT")
    private BigDecimal exemptAmount;

    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Column(name = "OVERRIDE_GL_ACCOUNT_ID")
    private String overrideGlAccountId;

    @Column(name = "INCLUDE_IN_TAX")
    private String includeInTax;

    @Column(name = "INCLUDE_IN_SHIPPING")
    private String includeInShipping;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
