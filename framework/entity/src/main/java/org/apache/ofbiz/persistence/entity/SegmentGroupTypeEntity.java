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
@Entity(name = "SEGMENT_GROUP_TYPE")
@Table(name = "SEGMENT_GROUP_TYPE")
public class SegmentGroupTypeEntity {
    @Id
    @Column(name = "SEGMENT_GROUP_TYPE_ID")
    private String segmentGroupTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
