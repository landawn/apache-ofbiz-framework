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
@Entity(name = "CUSTOM_TIME_PERIOD")
@Table(name = "CUSTOM_TIME_PERIOD")
public class CustomTimePeriodEntity {
    @Id
    @Column(name = "CUSTOM_TIME_PERIOD_ID")
    private String customTimePeriodId;

    @Column(name = "PARENT_PERIOD_ID")
    private String parentPeriodId;

    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Column(name = "PERIOD_NUM")
    private BigDecimal periodNum;

    @Column(name = "PERIOD_NAME")
    private String periodName;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "IS_CLOSED")
    private String isClosed;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;
}
