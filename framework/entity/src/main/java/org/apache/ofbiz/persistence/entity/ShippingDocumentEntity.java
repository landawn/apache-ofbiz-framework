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
@Entity(name = "SHIPPING_DOCUMENT")
@Table(name = "SHIPPING_DOCUMENT")
public class ShippingDocumentEntity {
    @Id
    @Column(name = "DOCUMENT_ID")
    private String documentId;

    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "SHIPMENT_ITEM_SEQ_ID")
    private String shipmentItemSeqId;

    @Column(name = "SHIPMENT_PACKAGE_SEQ_ID")
    private String shipmentPackageSeqId;

    @Column(name = "DESCRIPTION")
    private String description;
}
