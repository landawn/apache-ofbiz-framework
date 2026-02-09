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
@Entity(name = "SHIPMENT_BOX_TYPE")
@Table(name = "SHIPMENT_BOX_TYPE")
public class ShipmentBoxTypeEntity {
    @Id
    @Column(name = "SHIPMENT_BOX_TYPE_ID")
    private String shipmentBoxTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DIMENSION_UOM_ID")
    private String dimensionUomId;

    @Column(name = "BOX_LENGTH")
    private BigDecimal boxLength;

    @Column(name = "BOX_WIDTH")
    private BigDecimal boxWidth;

    @Column(name = "BOX_HEIGHT")
    private BigDecimal boxHeight;

    @Column(name = "WEIGHT_UOM_ID")
    private String weightUomId;

    @Column(name = "BOX_WEIGHT")
    private BigDecimal boxWeight;
}
