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
@Entity(name = "MARKETING_CAMPAIGN")
@Table(name = "MARKETING_CAMPAIGN")
public class MarketingCampaignEntity {
    @Id
    @Column(name = "MARKETING_CAMPAIGN_ID")
    private String marketingCampaignId;

    @Column(name = "PARENT_CAMPAIGN_ID")
    private String parentCampaignId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CAMPAIGN_NAME")
    private String campaignName;

    @Column(name = "CAMPAIGN_SUMMARY")
    private String campaignSummary;

    @Column(name = "BUDGETED_COST")
    private BigDecimal budgetedCost;

    @Column(name = "ACTUAL_COST")
    private BigDecimal actualCost;

    @Column(name = "ESTIMATED_COST")
    private BigDecimal estimatedCost;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    @Column(name = "CONVERTED_LEADS")
    private String convertedLeads;

    @Column(name = "EXPECTED_RESPONSE_PERCENT")
    private Double expectedResponsePercent;

    @Column(name = "EXPECTED_REVENUE")
    private BigDecimal expectedRevenue;

    @Column(name = "NUM_SENT")
    private BigDecimal numSent;

    @Column(name = "START_DATE")
    private Timestamp startDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
