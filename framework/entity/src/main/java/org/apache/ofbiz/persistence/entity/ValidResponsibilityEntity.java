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
@Entity(name = "VALID_RESPONSIBILITY")
@Table(name = "VALID_RESPONSIBILITY")
public class ValidResponsibilityEntity {
    @Id
    @Column(name = "EMPL_POSITION_TYPE_ID")
    private String emplPositionTypeId;

    @Id
    @Column(name = "RESPONSIBILITY_TYPE_ID")
    private String responsibilityTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "COMMENTS")
    private String comments;
}
