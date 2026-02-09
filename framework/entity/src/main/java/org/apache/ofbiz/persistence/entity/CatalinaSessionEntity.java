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
@Entity(name = "CATALINA_SESSION")
@Table(name = "CATALINA_SESSION")
public class CatalinaSessionEntity {
    @Id
    @Column(name = "SESSION_ID")
    private String sessionId;

    @Column(name = "SESSION_SIZE")
    private BigDecimal sessionSize;

    @Column(name = "SESSION_INFO")
    private byte[] sessionInfo;

    @Column(name = "IS_VALID")
    private String isValid;

    @Column(name = "MAX_IDLE")
    private BigDecimal maxIdle;

    @Column(name = "LAST_ACCESSED")
    private BigDecimal lastAccessed;
}
