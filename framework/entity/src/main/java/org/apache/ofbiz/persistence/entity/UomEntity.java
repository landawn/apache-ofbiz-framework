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
@Entity(name = "UOM")
@Table(name = "UOM")
public class UomEntity {
    @Id
    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "UOM_TYPE_ID")
    private String uomTypeId;

    @Column(name = "ABBREVIATION")
    private String abbreviation;

    @Column(name = "NUMERIC_CODE")
    private BigDecimal numericCode;

    @Column(name = "DESCRIPTION")
    private String description;
}
