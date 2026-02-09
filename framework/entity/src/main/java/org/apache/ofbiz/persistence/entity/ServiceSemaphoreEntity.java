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
@Entity(name = "SERVICE_SEMAPHORE")
@Table(name = "SERVICE_SEMAPHORE")
public class ServiceSemaphoreEntity {
    @Id
    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "LOCKED_BY_INSTANCE_ID")
    private String lockedByInstanceId;

    @Column(name = "LOCK_THREAD")
    private String lockThread;

    @Column(name = "LOCK_TIME")
    private Timestamp lockTime;
}
