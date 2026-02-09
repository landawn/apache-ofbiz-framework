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
@Entity(name = "ORDER_ITEM_ASSOC")
@Table(name = "ORDER_ITEM_ASSOC")
public class OrderItemAssocEntity {
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
    @Column(name = "TO_ORDER_ID")
    private String toOrderId;

    @Id
    @Column(name = "TO_ORDER_ITEM_SEQ_ID")
    private String toOrderItemSeqId;

    @Id
    @Column(name = "TO_SHIP_GROUP_SEQ_ID")
    private String toShipGroupSeqId;

    @Id
    @Column(name = "ORDER_ITEM_ASSOC_TYPE_ID")
    private String orderItemAssocTypeId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;
}
