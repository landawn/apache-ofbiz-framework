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
@Entity(name = "PRODUCT_FEATURE_IACTN")
@Table(name = "PRODUCT_FEATURE_IACTN")
public class ProductFeatureIactnEntity {
    @Id
    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Id
    @Column(name = "PRODUCT_FEATURE_ID_TO")
    private String productFeatureIdTo;

    @Column(name = "PRODUCT_FEATURE_IACTN_TYPE_ID")
    private String productFeatureIactnTypeId;

    @Column(name = "PRODUCT_ID")
    private String productId;
}
