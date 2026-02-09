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
@Entity(name = "EMPL_POSITION_TYPE_CLASS")
@Table(name = "EMPL_POSITION_TYPE_CLASS")
public class EmplPositionTypeClassEntity {
    @Id
    @Column(name = "EMPL_POSITION_TYPE_ID")
    private String emplPositionTypeId;

    @Id
    @Column(name = "EMPL_POSITION_CLASS_TYPE_ID")
    private String emplPositionClassTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "STANDARD_HOURS_PER_WEEK")
    private Double standardHoursPerWeek;
}
