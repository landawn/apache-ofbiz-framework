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
@Entity(name = "PRODUCT_FEATURE")
@Table(name = "PRODUCT_FEATURE")
public class ProductFeatureEntity {
    @Id
    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "PRODUCT_FEATURE_TYPE_ID")
    private String productFeatureTypeId;

    @Column(name = "PRODUCT_FEATURE_CATEGORY_ID")
    private String productFeatureCategoryId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "NUMBER_SPECIFIED")
    private BigDecimal numberSpecified;

    @Column(name = "DEFAULT_AMOUNT")
    private BigDecimal defaultAmount;

    @Column(name = "DEFAULT_SEQUENCE_NUM")
    private BigDecimal defaultSequenceNum;

    @Column(name = "ABBREV")
    private String abbrev;

    @Column(name = "ID_CODE")
    private String idCode;
}
