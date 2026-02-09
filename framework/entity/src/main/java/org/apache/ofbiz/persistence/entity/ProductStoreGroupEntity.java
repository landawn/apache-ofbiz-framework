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
@Entity(name = "PRODUCT_STORE_GROUP")
@Table(name = "PRODUCT_STORE_GROUP")
public class ProductStoreGroupEntity {
    @Id
    @Column(name = "PRODUCT_STORE_GROUP_ID")
    private String productStoreGroupId;

    @Column(name = "PRODUCT_STORE_GROUP_TYPE_ID")
    private String productStoreGroupTypeId;

    @Column(name = "PRIMARY_PARENT_GROUP_ID")
    private String primaryParentGroupId;

    @Column(name = "PRODUCT_STORE_GROUP_NAME")
    private String productStoreGroupName;

    @Column(name = "DESCRIPTION")
    private String description;
}
