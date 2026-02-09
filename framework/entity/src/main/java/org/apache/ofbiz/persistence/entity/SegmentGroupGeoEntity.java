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
@Entity(name = "SEGMENT_GROUP_GEO")
@Table(name = "SEGMENT_GROUP_GEO")
public class SegmentGroupGeoEntity {
    @Id
    @Column(name = "SEGMENT_GROUP_ID")
    private String segmentGroupId;

    @Id
    @Column(name = "GEO_ID")
    private String geoId;
}
