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
@Entity(name = "PRODUCT_CONFIG_OPTION")
@Table(name = "PRODUCT_CONFIG_OPTION")
public class ProductConfigOptionEntity {
    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Id
    @Column(name = "CONFIG_OPTION_ID")
    private String configOptionId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "CONFIG_OPTION_NAME")
    private String configOptionName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
