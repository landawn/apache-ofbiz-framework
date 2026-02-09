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
@Entity(name = "BILLING_ACCOUNT")
@Table(name = "BILLING_ACCOUNT")
public class BillingAccountEntity {
    @Id
    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "ACCOUNT_LIMIT")
    private BigDecimal accountLimit;

    @Column(name = "ACCOUNT_CURRENCY_UOM_ID")
    private String accountCurrencyUomId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "EXTERNAL_ACCOUNT_ID")
    private String externalAccountId;
}
