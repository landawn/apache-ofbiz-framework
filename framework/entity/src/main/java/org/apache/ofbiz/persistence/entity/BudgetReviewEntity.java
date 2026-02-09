package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "BUDGET_REVIEW")
@Table(name = "BUDGET_REVIEW")
public class BudgetReviewEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "BUDGET_REVIEW_ID")
    private String budgetReviewId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "BUDGET_REVIEW_RESULT_TYPE_ID")
    private String budgetReviewResultTypeId;

    @Column(name = "REVIEW_DATE")
    private Timestamp reviewDate;
}
