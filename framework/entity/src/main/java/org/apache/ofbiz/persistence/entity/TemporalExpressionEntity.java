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
@Entity(name = "TEMPORAL_EXPRESSION")
@Table(name = "TEMPORAL_EXPRESSION")
public class TemporalExpressionEntity {
    @Id
    @Column(name = "TEMP_EXPR_ID")
    private String tempExprId;

    @Column(name = "TEMP_EXPR_TYPE_ID")
    private String tempExprTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DATE1")
    private Timestamp date1;

    @Column(name = "DATE2")
    private Timestamp date2;

    @Column(name = "INTEGER1")
    private BigDecimal integer1;

    @Column(name = "INTEGER2")
    private BigDecimal integer2;

    @Column(name = "STRING1")
    private String string1;

    @Column(name = "STRING2")
    private String string2;
}
