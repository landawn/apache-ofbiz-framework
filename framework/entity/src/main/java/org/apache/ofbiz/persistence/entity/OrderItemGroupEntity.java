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
@Entity(name = "ORDER_ITEM_GROUP")
@Table(name = "ORDER_ITEM_GROUP")
public class OrderItemGroupEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_GROUP_SEQ_ID")
    private String orderItemGroupSeqId;

    @Column(name = "PARENT_GROUP_SEQ_ID")
    private String parentGroupSeqId;

    @Column(name = "GROUP_NAME")
    private String groupName;
}
