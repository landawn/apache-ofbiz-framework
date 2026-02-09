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
@Entity(name = "ALLOCATION_PLAN_TYPE")
@Table(name = "ALLOCATION_PLAN_TYPE")
public class AllocationPlanTypeEntity {
    @Id
    @Column(name = "PLAN_TYPE_ID")
    private String planTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "HAS_TABLE")
    private String hasTable;
}
