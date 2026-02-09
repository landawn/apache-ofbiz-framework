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
@Entity(name = "BUDGET_SCENARIO_APPLICATION")
@Table(name = "BUDGET_SCENARIO_APPLICATION")
public class BudgetScenarioApplicationEntity {
    @Id
    @Column(name = "BUDGET_SCENARIO_APPLIC_ID")
    private String budgetScenarioApplicId;

    @Id
    @Column(name = "BUDGET_SCENARIO_ID")
    private String budgetScenarioId;

    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Column(name = "AMOUNT_CHANGE")
    private BigDecimal amountChange;

    @Column(name = "PERCENTAGE_CHANGE")
    private BigDecimal percentageChange;
}
