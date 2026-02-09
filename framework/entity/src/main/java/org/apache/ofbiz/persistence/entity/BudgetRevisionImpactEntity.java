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
@Entity(name = "BUDGET_REVISION_IMPACT")
@Table(name = "BUDGET_REVISION_IMPACT")
public class BudgetRevisionImpactEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Id
    @Column(name = "REVISION_SEQ_ID")
    private String revisionSeqId;

    @Column(name = "REVISED_AMOUNT")
    private BigDecimal revisedAmount;

    @Column(name = "ADD_DELETE_FLAG")
    private String addDeleteFlag;

    @Column(name = "REVISION_REASON")
    private String revisionReason;
}
