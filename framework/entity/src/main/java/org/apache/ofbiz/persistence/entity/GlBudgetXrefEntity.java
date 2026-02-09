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
@Entity(name = "GL_BUDGET_XREF")
@Table(name = "GL_BUDGET_XREF")
public class GlBudgetXrefEntity {
    @Id
    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Id
    @Column(name = "BUDGET_ITEM_TYPE_ID")
    private String budgetItemTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "ALLOCATION_PERCENTAGE")
    private BigDecimal allocationPercentage;
}
