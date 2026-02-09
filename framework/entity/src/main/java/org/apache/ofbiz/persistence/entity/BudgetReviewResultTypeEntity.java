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
@Entity(name = "BUDGET_REVIEW_RESULT_TYPE")
@Table(name = "BUDGET_REVIEW_RESULT_TYPE")
public class BudgetReviewResultTypeEntity {
    @Id
    @Column(name = "BUDGET_REVIEW_RESULT_TYPE_ID")
    private String budgetReviewResultTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMMENTS")
    private String comments;
}
