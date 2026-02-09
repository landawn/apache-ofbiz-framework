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
@Entity(name = "PRODUCT_AVERAGE_COST")
@Table(name = "PRODUCT_AVERAGE_COST")
public class ProductAverageCostEntity {
    @Id
    @Column(name = "PRODUCT_AVERAGE_COST_TYPE_ID")
    private String productAverageCostTypeId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "AVERAGE_COST")
    private BigDecimal averageCost;
}
