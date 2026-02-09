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
@Entity(name = "DELIVERY")
@Table(name = "DELIVERY")
public class DeliveryEntity {
    @Id
    @Column(name = "DELIVERY_ID")
    private String deliveryId;

    @Column(name = "ORIGIN_FACILITY_ID")
    private String originFacilityId;

    @Column(name = "DEST_FACILITY_ID")
    private String destFacilityId;

    @Column(name = "ACTUAL_START_DATE")
    private Timestamp actualStartDate;

    @Column(name = "ACTUAL_ARRIVAL_DATE")
    private Timestamp actualArrivalDate;

    @Column(name = "ESTIMATED_START_DATE")
    private Timestamp estimatedStartDate;

    @Column(name = "ESTIMATED_ARRIVAL_DATE")
    private Timestamp estimatedArrivalDate;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "START_MILEAGE")
    private BigDecimal startMileage;

    @Column(name = "END_MILEAGE")
    private BigDecimal endMileage;

    @Column(name = "FUEL_USED")
    private BigDecimal fuelUsed;
}
