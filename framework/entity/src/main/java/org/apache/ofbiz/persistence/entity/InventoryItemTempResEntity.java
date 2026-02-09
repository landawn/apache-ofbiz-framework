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
@Entity(name = "INVENTORY_ITEM_TEMP_RES")
@Table(name = "INVENTORY_ITEM_TEMP_RES")
public class InventoryItemTempResEntity {
    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "RESERVED_DATE")
    private Timestamp reservedDate;
}
