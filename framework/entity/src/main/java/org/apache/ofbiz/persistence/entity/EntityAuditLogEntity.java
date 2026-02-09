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
@Entity(name = "ENTITY_AUDIT_LOG")
@Table(name = "ENTITY_AUDIT_LOG")
public class EntityAuditLogEntity {
    @Id
    @Column(name = "AUDIT_HISTORY_SEQ_ID")
    private String auditHistorySeqId;

    @Column(name = "CHANGED_ENTITY_NAME")
    private String changedEntityName;

    @Column(name = "CHANGED_FIELD_NAME")
    private String changedFieldName;

    @Column(name = "PK_COMBINED_VALUE_TEXT")
    private String pkCombinedValueText;

    @Column(name = "OLD_VALUE_TEXT")
    private String oldValueText;

    @Column(name = "NEW_VALUE_TEXT")
    private String newValueText;

    @Column(name = "CHANGED_DATE")
    private Timestamp changedDate;

    @Column(name = "CHANGED_BY_INFO")
    private String changedByInfo;

    @Column(name = "CHANGED_SESSION_INFO")
    private String changedSessionInfo;
}
