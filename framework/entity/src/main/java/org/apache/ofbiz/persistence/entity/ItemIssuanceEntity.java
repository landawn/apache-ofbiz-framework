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
@Entity(name = "ITEM_ISSUANCE")
@Table(name = "ITEM_ISSUANCE")
public class ItemIssuanceEntity {
    @Id
    @Column(name = "ITEM_ISSUANCE_ID")
    private String itemIssuanceId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "MAINT_HIST_SEQ_ID")
    private String maintHistSeqId;

    @Column(name = "ISSUED_DATE_TIME")
    private Timestamp issuedDateTime;

    @Column(name = "ISSUED_BY_USER_LOGIN_ID")
    private String issuedByUserLoginId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "CANCEL_QUANTITY")
    private BigDecimal cancelQuantity;
}
