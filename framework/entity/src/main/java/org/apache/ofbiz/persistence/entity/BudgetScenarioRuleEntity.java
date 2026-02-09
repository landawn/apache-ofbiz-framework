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
@Entity(name = "BUDGET_SCENARIO_RULE")
@Table(name = "BUDGET_SCENARIO_RULE")
public class BudgetScenarioRuleEntity {
    @Id
    @Column(name = "BUDGET_SCENARIO_ID")
    private String budgetScenarioId;

    @Id
    @Column(name = "BUDGET_ITEM_TYPE_ID")
    private String budgetItemTypeId;

    @Column(name = "AMOUNT_CHANGE")
    private BigDecimal amountChange;

    @Column(name = "PERCENTAGE_CHANGE")
    private BigDecimal percentageChange;
}
