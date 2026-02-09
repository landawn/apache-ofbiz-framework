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
@Entity(name = "SALES_OPPORTUNITY")
@Table(name = "SALES_OPPORTUNITY")
public class SalesOpportunityEntity {
    @Id
    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;

    @Column(name = "OPPORTUNITY_NAME")
    private String opportunityName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "NEXT_STEP")
    private String nextStep;

    @Column(name = "NEXT_STEP_DATE")
    private Timestamp nextStepDate;

    @Column(name = "ESTIMATED_AMOUNT")
    private BigDecimal estimatedAmount;

    @Column(name = "ESTIMATED_PROBABILITY")
    private BigDecimal estimatedProbability;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "MARKETING_CAMPAIGN_ID")
    private String marketingCampaignId;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "ESTIMATED_CLOSE_DATE")
    private Timestamp estimatedCloseDate;

    @Column(name = "OPPORTUNITY_STAGE_ID")
    private String opportunityStageId;

    @Column(name = "TYPE_ENUM_ID")
    private String typeEnumId;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;
}
