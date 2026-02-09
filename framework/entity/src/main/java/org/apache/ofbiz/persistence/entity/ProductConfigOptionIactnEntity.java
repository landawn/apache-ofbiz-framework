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
@Entity(name = "PRODUCT_CONFIG_OPTION_IACTN")
@Table(name = "PRODUCT_CONFIG_OPTION_IACTN")
public class ProductConfigOptionIactnEntity {
    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Id
    @Column(name = "CONFIG_OPTION_ID")
    private String configOptionId;

    @Id
    @Column(name = "CONFIG_ITEM_ID_TO")
    private String configItemIdTo;

    @Id
    @Column(name = "CONFIG_OPTION_ID_TO")
    private String configOptionIdTo;

    @Id
    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "CONFIG_IACTN_TYPE_ID")
    private String configIactnTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
