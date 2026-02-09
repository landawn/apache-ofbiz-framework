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
@Entity(name = "PRODUCT_FACILITY")
@Table(name = "PRODUCT_FACILITY")
public class ProductFacilityEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "MINIMUM_STOCK")
    private BigDecimal minimumStock;

    @Column(name = "REORDER_QUANTITY")
    private BigDecimal reorderQuantity;

    @Column(name = "DAYS_TO_SHIP")
    private BigDecimal daysToShip;

    @Column(name = "REPLENISH_METHOD_ENUM_ID")
    private String replenishMethodEnumId;

    @Column(name = "LAST_INVENTORY_COUNT")
    private BigDecimal lastInventoryCount;

    @Column(name = "REQUIREMENT_METHOD_ENUM_ID")
    private String requirementMethodEnumId;
}
