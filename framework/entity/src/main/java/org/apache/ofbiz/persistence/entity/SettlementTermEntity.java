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
@Entity(name = "SETTLEMENT_TERM")
@Table(name = "SETTLEMENT_TERM")
public class SettlementTermEntity {
    @Id
    @Column(name = "SETTLEMENT_TERM_ID")
    private String settlementTermId;

    @Column(name = "TERM_NAME")
    private String termName;

    @Column(name = "TERM_VALUE")
    private BigDecimal termValue;

    @Column(name = "UOM_ID")
    private String uomId;
}
