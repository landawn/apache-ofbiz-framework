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
@Entity(name = "BUDGET_REVISION")
@Table(name = "BUDGET_REVISION")
public class BudgetRevisionEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "REVISION_SEQ_ID")
    private String revisionSeqId;

    @Column(name = "DATE_REVISED")
    private Timestamp dateRevised;
}
