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
@Entity(name = "APPLICATION_SANDBOX")
@Table(name = "APPLICATION_SANDBOX")
public class ApplicationSandboxEntity {
    @Id
    @Column(name = "APPLICATION_ID")
    private String applicationId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "RUNTIME_DATA_ID")
    private String runtimeDataId;
}
