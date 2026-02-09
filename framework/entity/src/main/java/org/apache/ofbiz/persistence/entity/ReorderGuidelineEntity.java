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
@Entity(name = "REORDER_GUIDELINE")
@Table(name = "REORDER_GUIDELINE")
public class ReorderGuidelineEntity {
    @Id
    @Column(name = "REORDER_GUIDELINE_ID")
    private String reorderGuidelineId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "REORDER_QUANTITY")
    private BigDecimal reorderQuantity;

    @Column(name = "REORDER_LEVEL")
    private BigDecimal reorderLevel;
}
