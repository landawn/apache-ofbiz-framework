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
@Entity(name = "RETURN_ITEM")
@Table(name = "RETURN_ITEM")
public class ReturnItemEntity {
    @Id
    @Column(name = "RETURN_ID")
    private String returnId;

    @Id
    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Column(name = "RETURN_REASON_ID")
    private String returnReasonId;

    @Column(name = "RETURN_TYPE_ID")
    private String returnTypeId;

    @Column(name = "RETURN_ITEM_TYPE_ID")
    private String returnItemTypeId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "EXPECTED_ITEM_STATUS")
    private String expectedItemStatus;

    @Column(name = "RETURN_QUANTITY")
    private BigDecimal returnQuantity;

    @Column(name = "RECEIVED_QUANTITY")
    private BigDecimal receivedQuantity;

    @Column(name = "RETURN_PRICE")
    private BigDecimal returnPrice;

    @Column(name = "RETURN_ITEM_RESPONSE_ID")
    private String returnItemResponseId;
}
