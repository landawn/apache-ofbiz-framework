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
@Entity(name = "BILLING_ACCOUNT_TERM")
@Table(name = "BILLING_ACCOUNT_TERM")
public class BillingAccountTermEntity {
    @Id
    @Column(name = "BILLING_ACCOUNT_TERM_ID")
    private String billingAccountTermId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "TERM_TYPE_ID")
    private String termTypeId;

    @Column(name = "TERM_VALUE")
    private BigDecimal termValue;

    @Column(name = "TERM_DAYS")
    private BigDecimal termDays;

    @Column(name = "UOM_ID")
    private String uomId;
}
