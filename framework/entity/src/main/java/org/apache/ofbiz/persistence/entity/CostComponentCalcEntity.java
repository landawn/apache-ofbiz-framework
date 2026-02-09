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
@Entity(name = "COST_COMPONENT_CALC")
@Table(name = "COST_COMPONENT_CALC")
public class CostComponentCalcEntity {
    @Id
    @Column(name = "COST_COMPONENT_CALC_ID")
    private String costComponentCalcId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COST_GL_ACCOUNT_TYPE_ID")
    private String costGlAccountTypeId;

    @Column(name = "OFFSETTING_GL_ACCOUNT_TYPE_ID")
    private String offsettingGlAccountTypeId;

    @Column(name = "FIXED_COST")
    private BigDecimal fixedCost;

    @Column(name = "VARIABLE_COST")
    private BigDecimal variableCost;

    @Column(name = "PER_MILLI_SECOND")
    private BigDecimal perMilliSecond;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "COST_CUSTOM_METHOD_ID")
    private String costCustomMethodId;
}
