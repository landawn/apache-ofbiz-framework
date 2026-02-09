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
@Entity(name = "WORK_ORDER_ITEM_FULFILLMENT")
@Table(name = "WORK_ORDER_ITEM_FULFILLMENT")
public class WorkOrderItemFulfillmentEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;
}
