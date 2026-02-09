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
@Entity(name = "QUOTE_COEFFICIENT")
@Table(name = "QUOTE_COEFFICIENT")
public class QuoteCoefficientEntity {
    @Id
    @Column(name = "QUOTE_ID")
    private String quoteId;

    @Id
    @Column(name = "COEFF_NAME")
    private String coeffName;

    @Column(name = "COEFF_VALUE")
    private BigDecimal coeffValue;
}
