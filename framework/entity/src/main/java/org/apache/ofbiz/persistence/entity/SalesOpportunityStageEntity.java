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
@Entity(name = "SALES_OPPORTUNITY_STAGE")
@Table(name = "SALES_OPPORTUNITY_STAGE")
public class SalesOpportunityStageEntity {
    @Id
    @Column(name = "OPPORTUNITY_STAGE_ID")
    private String opportunityStageId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_PROBABILITY")
    private BigDecimal defaultProbability;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
