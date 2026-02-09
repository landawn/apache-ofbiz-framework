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
@Entity(name = "SERVER_HIT_BIN")
@Table(name = "SERVER_HIT_BIN")
public class ServerHitBinEntity {
    @Id
    @Column(name = "SERVER_HIT_BIN_ID")
    private String serverHitBinId;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "HIT_TYPE_ID")
    private String hitTypeId;

    @Column(name = "SERVER_IP_ADDRESS")
    private String serverIpAddress;

    @Column(name = "SERVER_HOST_NAME")
    private String serverHostName;

    @Column(name = "BIN_START_DATE_TIME")
    private Timestamp binStartDateTime;

    @Column(name = "BIN_END_DATE_TIME")
    private Timestamp binEndDateTime;

    @Column(name = "NUMBER_HITS")
    private BigDecimal numberHits;

    @Column(name = "TOTAL_TIME_MILLIS")
    private BigDecimal totalTimeMillis;

    @Column(name = "MIN_TIME_MILLIS")
    private BigDecimal minTimeMillis;

    @Column(name = "MAX_TIME_MILLIS")
    private BigDecimal maxTimeMillis;

    @Column(name = "INTERNAL_CONTENT_ID")
    private String internalContentId;
}
