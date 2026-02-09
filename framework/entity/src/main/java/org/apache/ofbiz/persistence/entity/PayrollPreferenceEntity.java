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
@Entity(name = "PAYROLL_PREFERENCE")
@Table(name = "PAYROLL_PREFERENCE")
public class PayrollPreferenceEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Id
    @Column(name = "PAYROLL_PREFERENCE_SEQ_ID")
    private String payrollPreferenceSeqId;

    @Column(name = "DEDUCTION_TYPE_ID")
    private String deductionTypeId;

    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PERCENTAGE")
    private Double percentage;

    @Column(name = "FLAT_AMOUNT")
    private BigDecimal flatAmount;

    @Column(name = "ROUTING_NUMBER")
    private String routingNumber;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "BANK_NAME")
    private String bankName;
}
