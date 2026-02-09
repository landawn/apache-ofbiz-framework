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
@Entity(name = "PERIOD_TYPE")
@Table(name = "PERIOD_TYPE")
public class PeriodTypeEntity {
    @Id
    @Column(name = "PERIOD_TYPE_ID")
    private String periodTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PERIOD_LENGTH")
    private BigDecimal periodLength;

    @Column(name = "UOM_ID")
    private String uomId;
}
