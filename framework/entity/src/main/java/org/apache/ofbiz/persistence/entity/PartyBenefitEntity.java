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
@Entity(name = "PARTY_BENEFIT")
@Table(name = "PARTY_BENEFIT")
public class PartyBenefitEntity {
    @Id
    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Id
    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Id
    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Id
    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Id
    @Column(name = "BENEFIT_TYPE_ID")
    private String benefitTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Column(name = "COST")
    private BigDecimal cost;

    @Column(name = "ACTUAL_EMPLOYER_PAID_PERCENT")
    private Double actualEmployerPaidPercent;

    @Column(name = "AVAILABLE_TIME")
    private BigDecimal availableTime;
}
