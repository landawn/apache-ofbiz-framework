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
@Entity(name = "FACILITY_LOCATION")
@Table(name = "FACILITY_LOCATION")
public class FacilityLocationEntity {
    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Id
    @Column(name = "LOCATION_SEQ_ID")
    private String locationSeqId;

    @Column(name = "LOCATION_TYPE_ENUM_ID")
    private String locationTypeEnumId;

    @Column(name = "AREA_ID")
    private String areaId;

    @Column(name = "AISLE_ID")
    private String aisleId;

    @Column(name = "SECTION_ID")
    private String sectionId;

    @Column(name = "LEVEL_ID")
    private String levelId;

    @Column(name = "POSITION_ID")
    private String positionId;

    @Column(name = "GEO_POINT_ID")
    private String geoPointId;
}
