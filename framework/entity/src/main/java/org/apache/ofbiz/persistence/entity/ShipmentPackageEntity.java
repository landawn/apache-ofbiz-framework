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
@Entity(name = "SHIPMENT_PACKAGE")
@Table(name = "SHIPMENT_PACKAGE")
public class ShipmentPackageEntity {
    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Id
    @Column(name = "SHIPMENT_PACKAGE_SEQ_ID")
    private String shipmentPackageSeqId;

    @Column(name = "SHIPMENT_BOX_TYPE_ID")
    private String shipmentBoxTypeId;

    @Column(name = "DATE_CREATED")
    private Timestamp dateCreated;

    @Column(name = "BOX_LENGTH")
    private BigDecimal boxLength;

    @Column(name = "BOX_HEIGHT")
    private BigDecimal boxHeight;

    @Column(name = "BOX_WIDTH")
    private BigDecimal boxWidth;

    @Column(name = "DIMENSION_UOM_ID")
    private String dimensionUomId;

    @Column(name = "WEIGHT")
    private BigDecimal weight;

    @Column(name = "WEIGHT_UOM_ID")
    private String weightUomId;

    @Column(name = "INSURED_VALUE")
    private BigDecimal insuredValue;
}
