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
@Entity(name = "PRODUCT_CONFIG_PRODUCT")
@Table(name = "PRODUCT_CONFIG_PRODUCT")
public class ProductConfigProductEntity {
    @Id
    @Column(name = "CONFIG_ITEM_ID")
    private String configItemId;

    @Id
    @Column(name = "CONFIG_OPTION_ID")
    private String configOptionId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
