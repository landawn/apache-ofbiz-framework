package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SHOPPING_LIST_ITEM_ATTRIBUTE")
@Table(name = "SHOPPING_LIST_ITEM_ATTRIBUTE")
public class ShoppingListItemAttributeEntity {
    @Id
    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Id
    @Column(name = "SHOPPING_LIST_ITEM_SEQ_ID")
    private String shoppingListItemSeqId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;
}
