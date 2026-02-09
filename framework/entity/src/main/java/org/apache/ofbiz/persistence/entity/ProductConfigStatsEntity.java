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
@Entity(name = "PRODUCT_CONFIG_STATS")
@Table(name = "PRODUCT_CONFIG_STATS")
public class ProductConfigStatsEntity {
    @Id
    @Column(name = "CONFIG_ID")
    private String configId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "NUM_OF_CONFS")
    private BigDecimal numOfConfs;

    @Column(name = "CONFIG_TYPE_ID")
    private String configTypeId;
}
