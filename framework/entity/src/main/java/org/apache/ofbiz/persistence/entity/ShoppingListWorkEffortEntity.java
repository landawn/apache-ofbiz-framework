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
@Entity(name = "SHOPPING_LIST_WORK_EFFORT")
@Table(name = "SHOPPING_LIST_WORK_EFFORT")
public class ShoppingListWorkEffortEntity {
    @Id
    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;
}
