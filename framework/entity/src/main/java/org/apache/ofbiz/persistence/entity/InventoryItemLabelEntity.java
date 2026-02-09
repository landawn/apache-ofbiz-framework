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
@Entity(name = "INVENTORY_ITEM_LABEL")
@Table(name = "INVENTORY_ITEM_LABEL")
public class InventoryItemLabelEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_LABEL_ID")
    private String inventoryItemLabelId;

    @Column(name = "INVENTORY_ITEM_LABEL_TYPE_ID")
    private String inventoryItemLabelTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
