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
@Entity(name = "INVENTORY_ITEM_VARIANCE")
@Table(name = "INVENTORY_ITEM_VARIANCE")
public class InventoryItemVarianceEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Id
    @Column(name = "PHYSICAL_INVENTORY_ID")
    private String physicalInventoryId;

    @Column(name = "VARIANCE_REASON_ID")
    private String varianceReasonId;

    @Column(name = "AVAILABLE_TO_PROMISE_VAR")
    private BigDecimal availableToPromiseVar;

    @Column(name = "QUANTITY_ON_HAND_VAR")
    private BigDecimal quantityOnHandVar;

    @Column(name = "COMMENTS")
    private String comments;
}
