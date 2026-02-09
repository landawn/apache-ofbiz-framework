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
@Entity(name = "RETURN_ITEM_SHIPMENT")
@Table(name = "RETURN_ITEM_SHIPMENT")
public class ReturnItemShipmentEntity {
    @Id
    @Column(name = "RETURN_ID")
    private String returnId;

    @Id
    @Column(name = "RETURN_ITEM_SEQ_ID")
    private String returnItemSeqId;

    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Id
    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;
}
