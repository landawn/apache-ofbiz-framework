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
@Entity(name = "WORK_EFFORT_INVENTORY_ASSIGN")
@Table(name = "WORK_EFFORT_INVENTORY_ASSIGN")
public class WorkEffortInventoryAssignEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "QUANTITY")
    private Double quantity;
}
