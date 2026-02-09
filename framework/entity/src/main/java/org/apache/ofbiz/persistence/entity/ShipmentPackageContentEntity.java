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
@Entity(name = "SHIPMENT_PACKAGE_CONTENT")
@Table(name = "SHIPMENT_PACKAGE_CONTENT")
public class ShipmentPackageContentEntity {
    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Id
    @Column(name = "SHIPMENT_PACKAGE_SEQ_ID")
    private String shipmentPackageSeqId;

    @Id
    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "SUB_PRODUCT_ID")
    private String subProductId;

    @Column(name = "SUB_PRODUCT_QUANTITY")
    private BigDecimal subProductQuantity;
}
