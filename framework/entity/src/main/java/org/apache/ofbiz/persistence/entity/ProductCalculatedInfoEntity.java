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
@Entity(name = "PRODUCT_CALCULATED_INFO")
@Table(name = "PRODUCT_CALCULATED_INFO")
public class ProductCalculatedInfoEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "TOTAL_QUANTITY_ORDERED")
    private BigDecimal totalQuantityOrdered;

    @Column(name = "TOTAL_TIMES_VIEWED")
    private BigDecimal totalTimesViewed;

    @Column(name = "AVERAGE_CUSTOMER_RATING")
    private BigDecimal averageCustomerRating;
}
