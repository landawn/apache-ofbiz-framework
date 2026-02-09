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
@Entity(name = "WORK_EFFORT_TRANS_BOX")
@Table(name = "WORK_EFFORT_TRANS_BOX")
public class WorkEffortTransBoxEntity {
    @Id
    @Column(name = "PROCESS_WORK_EFFORT_ID")
    private String processWorkEffortId;

    @Id
    @Column(name = "TO_ACTIVITY_ID")
    private String toActivityId;

    @Id
    @Column(name = "TRANSITION_ID")
    private String transitionId;
}
