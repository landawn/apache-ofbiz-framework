package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ENTITY_SYNC")
@Table(name = "ENTITY_SYNC")
public class EntitySyncEntity {
    @Id
    @Column(name = "ENTITY_SYNC_ID")
    private String entitySyncId;

    @Column(name = "RUN_STATUS_ID")
    private String runStatusId;

    @Column(name = "LAST_SUCCESSFUL_SYNCH_TIME")
    private Timestamp lastSuccessfulSynchTime;

    @Column(name = "LAST_HISTORY_START_DATE")
    private Timestamp lastHistoryStartDate;

    @Column(name = "PRE_OFFLINE_SYNCH_TIME")
    private Timestamp preOfflineSynchTime;

    @Column(name = "OFFLINE_SYNC_SPLIT_MILLIS")
    private BigDecimal offlineSyncSplitMillis;

    @Column(name = "SYNC_SPLIT_MILLIS")
    private BigDecimal syncSplitMillis;

    @Column(name = "SYNC_END_BUFFER_MILLIS")
    private BigDecimal syncEndBufferMillis;

    @Column(name = "MAX_RUNNING_NO_UPDATE_MILLIS")
    private BigDecimal maxRunningNoUpdateMillis;

    @Column(name = "TARGET_SERVICE_NAME")
    private String targetServiceName;

    @Column(name = "TARGET_DELEGATOR_NAME")
    private String targetDelegatorName;

    @Column(name = "KEEP_REMOVE_INFO_HOURS")
    private Double keepRemoveInfoHours;

    @Column(name = "FOR_PULL_ONLY")
    private String forPullOnly;

    @Column(name = "FOR_PUSH_ONLY")
    private String forPushOnly;
}
