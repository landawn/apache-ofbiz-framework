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
@Entity(name = "PRODUCT_FACILITY_LOCATION")
@Table(name = "PRODUCT_FACILITY_LOCATION")
public class ProductFacilityLocationEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Id
    @Column(name = "LOCATION_SEQ_ID")
    private String locationSeqId;

    @Column(name = "MINIMUM_STOCK")
    private BigDecimal minimumStock;

    @Column(name = "MOVE_QUANTITY")
    private BigDecimal moveQuantity;
}
