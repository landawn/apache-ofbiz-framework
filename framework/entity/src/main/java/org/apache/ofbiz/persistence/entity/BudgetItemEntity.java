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
@Entity(name = "BUDGET_ITEM")
@Table(name = "BUDGET_ITEM")
public class BudgetItemEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Column(name = "BUDGET_ITEM_TYPE_ID")
    private String budgetItemTypeId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PURPOSE")
    private String purpose;

    @Column(name = "JUSTIFICATION")
    private String justification;
}
