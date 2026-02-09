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
@Entity(name = "ORDER_ITEM_SHIP_GROUP_ASSOC")
@Table(name = "ORDER_ITEM_SHIP_GROUP_ASSOC")
public class OrderItemShipGroupAssocEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "CANCEL_QUANTITY")
    private BigDecimal cancelQuantity;
}
