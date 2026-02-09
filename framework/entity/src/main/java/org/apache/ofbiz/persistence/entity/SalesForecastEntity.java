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
@Entity(name = "SALES_FORECAST")
@Table(name = "SALES_FORECAST")
public class SalesForecastEntity {
    @Id
    @Column(name = "SALES_FORECAST_ID")
    private String salesForecastId;

    @Column(name = "PARENT_SALES_FORECAST_ID")
    private String parentSalesForecastId;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "INTERNAL_PARTY_ID")
    private String internalPartyId;

    @Column(name = "CUSTOM_TIME_PERIOD_ID")
    private String customTimePeriodId;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "QUOTA_AMOUNT")
    private BigDecimal quotaAmount;

    @Column(name = "FORECAST_AMOUNT")
    private BigDecimal forecastAmount;

    @Column(name = "BEST_CASE_AMOUNT")
    private BigDecimal bestCaseAmount;

    @Column(name = "CLOSED_AMOUNT")
    private BigDecimal closedAmount;

    @Column(name = "PERCENT_OF_QUOTA_FORECAST")
    private BigDecimal percentOfQuotaForecast;

    @Column(name = "PERCENT_OF_QUOTA_CLOSED")
    private BigDecimal percentOfQuotaClosed;

    @Column(name = "PIPELINE_AMOUNT")
    private BigDecimal pipelineAmount;

    @Column(name = "CREATED_BY_USER_LOGIN_ID")
    private String createdByUserLoginId;

    @Column(name = "MODIFIED_BY_USER_LOGIN_ID")
    private String modifiedByUserLoginId;
}
