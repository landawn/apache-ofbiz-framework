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
@Entity(name = "PRODUCT_ORDER_ITEM")
@Table(name = "PRODUCT_ORDER_ITEM")
public class ProductOrderItemEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "ENGAGEMENT_ID")
    private String engagementId;

    @Id
    @Column(name = "ENGAGEMENT_ITEM_SEQ_ID")
    private String engagementItemSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;
}
