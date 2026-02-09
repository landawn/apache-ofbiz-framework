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
@Entity(name = "GEO_POINT")
@Table(name = "GEO_POINT")
public class GeoPointEntity {
    @Id
    @Column(name = "GEO_POINT_ID")
    private String geoPointId;

    @Column(name = "GEO_POINT_TYPE_ENUM_ID")
    private String geoPointTypeEnumId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "LATITUDE")
    private BigDecimal latitude;

    @Column(name = "LONGITUDE")
    private BigDecimal longitude;

    @Column(name = "ELEVATION")
    private BigDecimal elevation;

    @Column(name = "ELEVATION_UOM_ID")
    private String elevationUomId;

    @Column(name = "INFORMATION")
    private String information;
}
