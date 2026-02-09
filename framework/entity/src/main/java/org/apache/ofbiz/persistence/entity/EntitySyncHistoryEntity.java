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
@Entity(name = "ENTITY_SYNC_HISTORY")
@Table(name = "ENTITY_SYNC_HISTORY")
public class EntitySyncHistoryEntity {
    @Id
    @Column(name = "ENTITY_SYNC_ID")
    private String entitySyncId;

    @Id
    @Column(name = "START_DATE")
    private Timestamp startDate;

    @Column(name = "RUN_STATUS_ID")
    private String runStatusId;

    @Column(name = "BEGINNING_SYNCH_TIME")
    private Timestamp beginningSynchTime;

    @Column(name = "LAST_SUCCESSFUL_SYNCH_TIME")
    private Timestamp lastSuccessfulSynchTime;

    @Column(name = "LAST_CANDIDATE_END_TIME")
    private Timestamp lastCandidateEndTime;

    @Column(name = "LAST_SPLIT_START_TIME")
    private BigDecimal lastSplitStartTime;

    @Column(name = "TO_CREATE_INSERTED")
    private BigDecimal toCreateInserted;

    @Column(name = "TO_CREATE_UPDATED")
    private BigDecimal toCreateUpdated;

    @Column(name = "TO_CREATE_NOT_UPDATED")
    private BigDecimal toCreateNotUpdated;

    @Column(name = "TO_STORE_INSERTED")
    private BigDecimal toStoreInserted;

    @Column(name = "TO_STORE_UPDATED")
    private BigDecimal toStoreUpdated;

    @Column(name = "TO_STORE_NOT_UPDATED")
    private BigDecimal toStoreNotUpdated;

    @Column(name = "TO_REMOVE_DELETED")
    private BigDecimal toRemoveDeleted;

    @Column(name = "TO_REMOVE_ALREADY_DELETED")
    private BigDecimal toRemoveAlreadyDeleted;

    @Column(name = "TOTAL_ROWS_EXPORTED")
    private BigDecimal totalRowsExported;

    @Column(name = "TOTAL_ROWS_TO_CREATE")
    private BigDecimal totalRowsToCreate;

    @Column(name = "TOTAL_ROWS_TO_STORE")
    private BigDecimal totalRowsToStore;

    @Column(name = "TOTAL_ROWS_TO_REMOVE")
    private BigDecimal totalRowsToRemove;

    @Column(name = "TOTAL_SPLITS")
    private BigDecimal totalSplits;

    @Column(name = "TOTAL_STORE_CALLS")
    private BigDecimal totalStoreCalls;

    @Column(name = "RUNNING_TIME_MILLIS")
    private BigDecimal runningTimeMillis;

    @Column(name = "PER_SPLIT_MIN_MILLIS")
    private BigDecimal perSplitMinMillis;

    @Column(name = "PER_SPLIT_MAX_MILLIS")
    private BigDecimal perSplitMaxMillis;

    @Column(name = "PER_SPLIT_MIN_ITEMS")
    private BigDecimal perSplitMinItems;

    @Column(name = "PER_SPLIT_MAX_ITEMS")
    private BigDecimal perSplitMaxItems;
}
