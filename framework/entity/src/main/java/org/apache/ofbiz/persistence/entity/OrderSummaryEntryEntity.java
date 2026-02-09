package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDER_SUMMARY_ENTRY")
@Table(name = "ORDER_SUMMARY_ENTRY")
public class OrderSummaryEntryEntity {
    @Id
    @Column(name = "ENTRY_DATE")
    private Date entryDate;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "TOTAL_QUANTITY")
    private BigDecimal totalQuantity;

    @Column(name = "GROSS_SALES")
    private BigDecimal grossSales;

    @Column(name = "PRODUCT_COST")
    private BigDecimal productCost;
}
