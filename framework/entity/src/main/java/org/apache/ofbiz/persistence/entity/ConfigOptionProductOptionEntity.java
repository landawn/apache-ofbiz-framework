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
@Entity(name = "CONFIG_OPTION_PRODUCT_OPTION")
@Table(name = "CONFIG_OPTION_PRODUCT_OPTION")
public class ConfigOptionProductOptionEntity {
    @Id
    @Column(name = "CONFIG_ID")
    private String configId;

    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Id
    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Id
    @Column(name = "CONFIG_OPTION_ID")
    private String configOptionId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_OPTION_ID")
    private String productOptionId;

    @Column(name = "DESCRIPTION")
    private String description;
}
