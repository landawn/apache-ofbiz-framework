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
@Entity(name = "GL_ACCOUNT_HISTORY")
@Table(name = "GL_ACCOUNT_HISTORY")
public class GlAccountHistoryEntity {
    @Id
    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Id
    @Column(name = "CUSTOM_TIME_PERIOD_ID")
    private String customTimePeriodId;

    @Column(name = "OPENING_BALANCE")
    private BigDecimal openingBalance;

    @Column(name = "POSTED_DEBITS")
    private BigDecimal postedDebits;

    @Column(name = "POSTED_CREDITS")
    private BigDecimal postedCredits;

    @Column(name = "ENDING_BALANCE")
    private BigDecimal endingBalance;
}
