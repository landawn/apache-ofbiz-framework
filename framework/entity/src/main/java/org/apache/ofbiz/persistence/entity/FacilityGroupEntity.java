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
@Entity(name = "FACILITY_GROUP")
@Table(name = "FACILITY_GROUP")
public class FacilityGroupEntity {
    @Id
    @Column(name = "FACILITY_GROUP_ID")
    private String facilityGroupId;

    @Column(name = "FACILITY_GROUP_TYPE_ID")
    private String facilityGroupTypeId;

    @Column(name = "PRIMARY_PARENT_GROUP_ID")
    private String primaryParentGroupId;

    @Column(name = "FACILITY_GROUP_NAME")
    private String facilityGroupName;

    @Column(name = "DESCRIPTION")
    private String description;
}
