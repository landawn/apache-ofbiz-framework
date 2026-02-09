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
@Entity(name = "CONTAINER")
@Table(name = "CONTAINER")
public class ContainerEntity {
    @Id
    @Column(name = "CONTAINER_ID")
    private String containerId;

    @Column(name = "CONTAINER_TYPE_ID")
    private String containerTypeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "DESCRIPTION")
    private String description;
}
