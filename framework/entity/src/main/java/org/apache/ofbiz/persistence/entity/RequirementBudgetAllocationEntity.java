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
@Entity(name = "REQUIREMENT_BUDGET_ALLOCATION")
@Table(name = "REQUIREMENT_BUDGET_ALLOCATION")
public class RequirementBudgetAllocationEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Id
    @Column(name = "REQUIREMENT_ID")
    private String requirementId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
