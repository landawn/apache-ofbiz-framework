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
@Entity(name = "UOM_CONVERSION_DATED")
@Table(name = "UOM_CONVERSION_DATED")
public class UomConversionDatedEntity {
    @Id
    @Column(name = "UOM_ID")
    private String uomId;

    @Id
    @Column(name = "UOM_ID_TO")
    private String uomIdTo;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "CONVERSION_FACTOR")
    private Double conversionFactor;

    @Column(name = "CUSTOM_METHOD_ID")
    private String customMethodId;

    @Column(name = "DECIMAL_SCALE")
    private BigDecimal decimalScale;

    @Column(name = "ROUNDING_MODE")
    private String roundingMode;

    @Column(name = "PURPOSE_ENUM_ID")
    private String purposeEnumId;
}
