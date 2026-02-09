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
@Entity(name = "PRODUCT_MAINT_TYPE")
@Table(name = "PRODUCT_MAINT_TYPE")
public class ProductMaintTypeEntity {
    @Id
    @Column(name = "PRODUCT_MAINT_TYPE_ID")
    private String productMaintTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;
}
