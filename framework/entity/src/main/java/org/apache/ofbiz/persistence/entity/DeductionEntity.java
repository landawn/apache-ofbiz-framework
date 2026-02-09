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
@Entity(name = "DEDUCTION")
@Table(name = "DEDUCTION")
public class DeductionEntity {
    @Id
    @Column(name = "DEDUCTION_ID")
    private String deductionId;

    @Column(name = "DEDUCTION_TYPE_ID")
    private String deductionTypeId;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;
}
