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
@Entity(name = "SHOPPING_LIST_ITEM")
@Table(name = "SHOPPING_LIST_ITEM")
public class ShoppingListItemEntity {
    @Id
    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Id
    @Column(name = "SHOPPING_LIST_ITEM_SEQ_ID")
    private String shoppingListItemSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "MODIFIED_PRICE")
    private BigDecimal modifiedPrice;

    @Column(name = "RESERV_START")
    private Timestamp reservStart;

    @Column(name = "RESERV_LENGTH")
    private BigDecimal reservLength;

    @Column(name = "RESERV_PERSONS")
    private BigDecimal reservPersons;

    @Column(name = "QUANTITY_PURCHASED")
    private BigDecimal quantityPurchased;

    @Column(name = "CONFIG_ID")
    private String configId;
}
