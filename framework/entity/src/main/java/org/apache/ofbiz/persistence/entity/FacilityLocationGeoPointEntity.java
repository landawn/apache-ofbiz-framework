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
@Entity(name = "FACILITY_LOCATION_GEO_POINT")
@Table(name = "FACILITY_LOCATION_GEO_POINT")
public class FacilityLocationGeoPointEntity {
    @Id
    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Id
    @Column(name = "LOCATION_SEQ_ID")
    private String locationSeqId;

    @Id
    @Column(name = "GEO_POINT_ID")
    private String geoPointId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
