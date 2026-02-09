package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT_FEATURE_CATEGORY_APPL")
@Table(name = "PRODUCT_FEATURE_CATEGORY_APPL")
public class ProductFeatureCategoryApplEntity {
    @Id
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Id
    @Column(name = "PRODUCT_FEATURE_CATEGORY_ID")
    private String productFeatureCategoryId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
