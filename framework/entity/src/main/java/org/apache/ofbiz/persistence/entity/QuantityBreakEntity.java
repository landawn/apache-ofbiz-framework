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
@Entity(name = "QUANTITY_BREAK")
@Table(name = "QUANTITY_BREAK")
public class QuantityBreakEntity {
    @Id
    @Column(name = "QUANTITY_BREAK_ID")
    private String quantityBreakId;

    @Column(name = "QUANTITY_BREAK_TYPE_ID")
    private String quantityBreakTypeId;

    @Column(name = "FROM_QUANTITY")
    private BigDecimal fromQuantity;

    @Column(name = "THRU_QUANTITY")
    private BigDecimal thruQuantity;
}
