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
@Entity(name = "PRODUCT_CONFIG")
@Table(name = "PRODUCT_CONFIG")
public class ProductConfigEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Id
    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LONG_DESCRIPTION")
    private String longDescription;

    @Column(name = "CONFIG_TYPE_ID")
    private String configTypeId;

    @Column(name = "DEFAULT_CONFIG_OPTION_ID")
    private String defaultConfigOptionId;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "IS_MANDATORY")
    private String isMandatory;
}
