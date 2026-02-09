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
@Entity(name = "EMPL_LEAVE_REASON_TYPE")
@Table(name = "EMPL_LEAVE_REASON_TYPE")
public class EmplLeaveReasonTypeEntity {
    @Id
    @Column(name = "EMPL_LEAVE_REASON_TYPE_ID")
    private String emplLeaveReasonTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "DESCRIPTION")
    private String description;
}
