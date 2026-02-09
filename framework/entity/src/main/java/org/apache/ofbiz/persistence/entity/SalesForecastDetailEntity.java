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
@Entity(name = "SALES_FORECAST_DETAIL")
@Table(name = "SALES_FORECAST_DETAIL")
public class SalesForecastDetailEntity {
    @Id
    @Column(name = "SALES_FORECAST_ID")
    private String salesForecastId;

    @Id
    @Column(name = "SALES_FORECAST_DETAIL_ID")
    private String salesForecastDetailId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "QUANTITY_UOM_ID")
    private String quantityUomId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;
}
