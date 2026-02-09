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
@Entity(name = "WORK_EFFORT_DELIVERABLE_PROD")
@Table(name = "WORK_EFFORT_DELIVERABLE_PROD")
public class WorkEffortDeliverableProdEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "DELIVERABLE_ID")
    private String deliverableId;
}
