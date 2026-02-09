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
@Entity(name = "PICKLIST_ITEM")
@Table(name = "PICKLIST_ITEM")
public class PicklistItemEntity {
    @Id
    @Column(name = "PICKLIST_BIN_ID")
    private String picklistBinId;

    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "ITEM_STATUS_ID")
    private String itemStatusId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;
}
