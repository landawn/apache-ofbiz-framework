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
@Entity(name = "PRODUCT_METER_TYPE")
@Table(name = "PRODUCT_METER_TYPE")
public class ProductMeterTypeEntity {
    @Id
    @Column(name = "PRODUCT_METER_TYPE_ID")
    private String productMeterTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_UOM_ID")
    private String defaultUomId;
}
