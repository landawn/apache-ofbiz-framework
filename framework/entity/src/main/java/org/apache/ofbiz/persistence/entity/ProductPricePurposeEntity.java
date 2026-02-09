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
@Entity(name = "PRODUCT_PRICE_PURPOSE")
@Table(name = "PRODUCT_PRICE_PURPOSE")
public class ProductPricePurposeEntity {
    @Id
    @Column(name = "PRODUCT_PRICE_PURPOSE_ID")
    private String productPricePurposeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
