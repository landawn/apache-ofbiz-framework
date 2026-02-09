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
@Entity(name = "BUDGET_ITEM_ATTRIBUTE")
@Table(name = "BUDGET_ITEM_ATTRIBUTE")
public class BudgetItemAttributeEntity {
    @Id
    @Column(name = "BUDGET_ID")
    private String budgetId;

    @Id
    @Column(name = "BUDGET_ITEM_SEQ_ID")
    private String budgetItemSeqId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;

    @Column(name = "ATTR_DESCRIPTION")
    private String attrDescription;
}
