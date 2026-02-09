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
@Entity(name = "SHIPMENT_METHOD_TYPE")
@Table(name = "SHIPMENT_METHOD_TYPE")
public class ShipmentMethodTypeEntity {
    @Id
    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
