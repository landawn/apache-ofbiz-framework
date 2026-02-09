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
@Entity(name = "SHIPMENT_RECEIPT")
@Table(name = "SHIPMENT_RECEIPT")
public class ShipmentReceiptEntity {
    @Id
    @Column(name = "RECEIPT_ID")
    private String receiptId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "SHIPMENT_PACKAGE_SEQ_ID")
    private String shipmentPackageSeqId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "RETURN_ID")
    private String returnId;

    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Column(name = "REJECTION_ID")
    private String rejectionId;

    @Column(name = "RECEIVED_BY_USER_LOGIN_ID")
    private String receivedByUserLoginId;

    @Column(name = "DATETIME_RECEIVED")
    private Timestamp datetimeReceived;

    @Column(name = "ITEM_DESCRIPTION")
    private String itemDescription;

    @Column(name = "QUANTITY_ACCEPTED")
    private BigDecimal quantityAccepted;

    @Column(name = "QUANTITY_REJECTED")
    private BigDecimal quantityRejected;
}
