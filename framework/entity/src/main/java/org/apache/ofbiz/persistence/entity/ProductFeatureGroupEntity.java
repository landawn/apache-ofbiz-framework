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
@Entity(name = "PRODUCT_FEATURE_GROUP")
@Table(name = "PRODUCT_FEATURE_GROUP")
public class ProductFeatureGroupEntity {
    @Id
    @Column(name = "PRODUCT_FEATURE_GROUP_ID")
    private String productFeatureGroupId;

    @Column(name = "DESCRIPTION")
    private String description;
}
