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
@Entity(name = "PRODUCT_GEO")
@Table(name = "PRODUCT_GEO")
public class ProductGeoEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "PRODUCT_GEO_ENUM_ID")
    private String productGeoEnumId;

    @Column(name = "DESCRIPTION")
    private String description;
}
