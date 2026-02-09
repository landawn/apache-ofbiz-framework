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
@Entity(name = "PRODUCT_FEATURE_CATEGORY")
@Table(name = "PRODUCT_FEATURE_CATEGORY")
public class ProductFeatureCategoryEntity {
    @Id
    @Column(name = "PRODUCT_FEATURE_CATEGORY_ID")
    private String productFeatureCategoryId;

    @Column(name = "PARENT_CATEGORY_ID")
    private String parentCategoryId;

    @Column(name = "DESCRIPTION")
    private String description;
}
