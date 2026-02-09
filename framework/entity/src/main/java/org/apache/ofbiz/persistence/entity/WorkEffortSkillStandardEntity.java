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
@Entity(name = "WORK_EFFORT_SKILL_STANDARD")
@Table(name = "WORK_EFFORT_SKILL_STANDARD")
public class WorkEffortSkillStandardEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "SKILL_TYPE_ID")
    private String skillTypeId;

    @Column(name = "ESTIMATED_NUM_PEOPLE")
    private Double estimatedNumPeople;

    @Column(name = "ESTIMATED_DURATION")
    private Double estimatedDuration;

    @Column(name = "ESTIMATED_COST")
    private BigDecimal estimatedCost;
}
