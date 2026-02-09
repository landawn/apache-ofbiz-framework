package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "BUDGET")
@Table(name = "BUDGET")
public class BudgetEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Column(name = "BUDGET_TYPE_ID")
    private String budgetTypeId;

    @Column(name = "CUSTOM_TIME_PERIOD_ID")
    private String customTimePeriodId;

    @Column(name = "COMMENTS")
    private String comments;
}
