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
@Entity(name = "PRODUCT_CONFIG_ITEM")
@Table(name = "PRODUCT_CONFIG_ITEM")
public class ProductConfigItemEntity {
    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Column(name = "CONFIG_ITEM_TYPE_ID")
    private String configItemTypeId;

    @Column(name = "CONFIG_ITEM_NAME")
    private String configItemName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LONG_DESCRIPTION")
    private String longDescription;

    @Column(name = "IMAGE_URL")
    private String imageUrl;
}
