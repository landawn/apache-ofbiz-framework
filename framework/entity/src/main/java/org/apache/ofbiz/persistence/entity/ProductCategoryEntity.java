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
@Entity(name = "PRODUCT_CATEGORY")
@Table(name = "PRODUCT_CATEGORY")
public class ProductCategoryEntity {
    @Id
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "PRODUCT_CATEGORY_TYPE_ID")
    private String productCategoryTypeId;

    @Column(name = "PRIMARY_PARENT_CATEGORY_ID")
    private String primaryParentCategoryId;

    @Column(name = "CATEGORY_NAME")
    private String categoryName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LONG_DESCRIPTION")
    private String longDescription;

    @Column(name = "CATEGORY_IMAGE_URL")
    private String categoryImageUrl;

    @Column(name = "LINK_ONE_IMAGE_URL")
    private String linkOneImageUrl;

    @Column(name = "LINK_TWO_IMAGE_URL")
    private String linkTwoImageUrl;

    @Column(name = "DETAIL_SCREEN")
    private String detailScreen;

    @Column(name = "SHOW_IN_SELECT")
    private String showInSelect;
}
