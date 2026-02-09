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
@Entity(name = "JOB_SANDBOX")
@Table(name = "JOB_SANDBOX")
public class JobSandboxEntity {
    @Id
    @Column(name = "JOB_ID")
    private String jobId;

    @Column(name = "JOB_NAME")
    private String jobName;

    @Column(name = "RUN_TIME")
    private Timestamp runTime;

    @Column(name = "RUN_TIME_EPOCH")
    private BigDecimal runTimeEpoch;

    @Column(name = "PRIORITY")
    private BigDecimal priority;

    @Column(name = "POOL_ID")
    private String poolId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PARENT_JOB_ID")
    private String parentJobId;

    @Column(name = "PREVIOUS_JOB_ID")
    private String previousJobId;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "LOADER_NAME")
    private String loaderName;

    @Column(name = "MAX_RETRY")
    private BigDecimal maxRetry;

    @Column(name = "CURRENT_RETRY_COUNT")
    private BigDecimal currentRetryCount;

    @Column(name = "AUTH_USER_LOGIN_ID")
    private String authUserLoginId;

    @Column(name = "RUN_AS_USER")
    private String runAsUser;

    @Column(name = "RUNTIME_DATA_ID")
    private String runtimeDataId;

    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;

    @Column(name = "TEMP_EXPR_ID")
    private String tempExprId;

    @Column(name = "CURRENT_RECURRENCE_COUNT")
    private BigDecimal currentRecurrenceCount;

    @Column(name = "MAX_RECURRENCE_COUNT")
    private BigDecimal maxRecurrenceCount;

    @Column(name = "RUN_BY_INSTANCE_ID")
    private String runByInstanceId;

    @Column(name = "START_DATE_TIME")
    private Timestamp startDateTime;

    @Column(name = "FINISH_DATE_TIME")
    private Timestamp finishDateTime;

    @Column(name = "CANCEL_DATE_TIME")
    private Timestamp cancelDateTime;

    @Column(name = "JOB_RESULT")
    private String jobResult;

    @Column(name = "RECURRENCE_TIME_ZONE")
    private String recurrenceTimeZone;
}
