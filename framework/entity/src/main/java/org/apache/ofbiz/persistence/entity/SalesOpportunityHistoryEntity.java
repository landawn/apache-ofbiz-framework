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
@Entity(name = "SALES_OPPORTUNITY_HISTORY")
@Table(name = "SALES_OPPORTUNITY_HISTORY")
public class SalesOpportunityHistoryEntity {
    @Id
    @Column(name = "SALES_OPPORTUNITY_HISTORY_ID")
    private String salesOpportunityHistoryId;

    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "NEXT_STEP")
    private String nextStep;

    @Column(name = "ESTIMATED_AMOUNT")
    private BigDecimal estimatedAmount;

    @Column(name = "ESTIMATED_PROBABILITY")
    private BigDecimal estimatedProbability;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "ESTIMATED_CLOSE_DATE")
    private Timestamp estimatedCloseDate;

    @Column(name = "OPPORTUNITY_STAGE_ID")
    private String opportunityStageId;

    @Column(name = "CHANGE_NOTE")
    private String changeNote;

    @Column(name = "MODIFIED_BY_USER_LOGIN")
    private String modifiedByUserLogin;

    @Column(name = "MODIFIED_TIMESTAMP")
    private Timestamp modifiedTimestamp;
}
