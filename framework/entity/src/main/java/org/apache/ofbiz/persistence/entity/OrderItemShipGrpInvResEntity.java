package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDER_ITEM_SHIP_GRP_INV_RES")
@Table(name = "ORDER_ITEM_SHIP_GRP_INV_RES")
public class OrderItemShipGrpInvResEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "RESERVE_ORDER_ENUM_ID")
    private String reserveOrderEnumId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "QUANTITY_NOT_AVAILABLE")
    private BigDecimal quantityNotAvailable;

    @Column(name = "RESERVED_DATETIME")
    private Timestamp reservedDatetime;

    @Column(name = "CREATED_DATETIME")
    private Timestamp createdDatetime;

    @Column(name = "PROMISED_DATETIME")
    private Timestamp promisedDatetime;

    @Column(name = "CURRENT_PROMISED_DATE")
    private Timestamp currentPromisedDate;

    @Column(name = "PRIORITY")
    private String priority;

    @Column(name = "SEQUENCE_ID")
    private BigDecimal sequenceId;
}
