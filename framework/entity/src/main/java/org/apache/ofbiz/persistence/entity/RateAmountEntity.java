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
@Entity(name = "RATE_AMOUNT")
@Table(name = "RATE_AMOUNT")
public class RateAmountEntity {
    @Id
    @Column(name = "RATE_TYPE_ID")
    private String rateTypeId;

    @Id
    @Column(name = "RATE_CURRENCY_UOM_ID")
    private String rateCurrencyUomId;

    @Id
    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "EMPL_POSITION_TYPE_ID")
    private String emplPositionTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "RATE_AMOUNT")
    private BigDecimal rateAmount;
}
