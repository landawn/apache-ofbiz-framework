package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TEMPORAL_EXPRESSION_ASSOC")
@Table(name = "TEMPORAL_EXPRESSION_ASSOC")
public class TemporalExpressionAssocEntity {
    @Id
    @Column(name = "FROM_TEMP_EXPR_ID")
    private String fromTempExprId;

    @Id
    @Column(name = "TO_TEMP_EXPR_ID")
    private String toTempExprId;

    @Column(name = "EXPR_ASSOC_TYPE")
    private String exprAssocType;
}
