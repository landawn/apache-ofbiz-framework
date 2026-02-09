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
@Entity(name = "INVENTORY_ITEM_LABEL_APPL")
@Table(name = "INVENTORY_ITEM_LABEL_APPL")
public class InventoryItemLabelApplEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Id
    @Column(name = "INVENTORY_ITEM_LABEL_TYPE_ID")
    private String inventoryItemLabelTypeId;

    @Column(name = "INVENTORY_ITEM_LABEL_ID")
    private String inventoryItemLabelId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
