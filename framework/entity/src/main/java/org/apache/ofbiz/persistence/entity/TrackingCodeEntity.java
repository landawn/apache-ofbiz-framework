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
@Entity(name = "TRACKING_CODE")
@Table(name = "TRACKING_CODE")
public class TrackingCodeEntity {
    @Id
    @Column(name = "TRACKING_CODE_ID")
    private String trackingCodeId;

    @Column(name = "TRACKING_CODE_TYPE_ID")
    private String trackingCodeTypeId;

    @Column(name = "MARKETING_CAMPAIGN_ID")
    private String marketingCampaignId;

    @Column(name = "REDIRECT_URL")
    private String redirectUrl;

    @Column(name = "OVERRIDE_LOGO")
    private String overrideLogo;

    @Column(name = "OVERRIDE_CSS")
    private String overrideCss;

    @Column(name = "PROD_CATALOG_ID")
    private String prodCatalogId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TRACKABLE_LIFETIME")
    private BigDecimal trackableLifetime;

    @Column(name = "BILLABLE_LIFETIME")
    private BigDecimal billableLifetime;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "SUBGROUP_ID")
    private String subgroupId;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
