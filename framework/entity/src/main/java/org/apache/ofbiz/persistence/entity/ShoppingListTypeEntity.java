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
@Entity(name = "SHOPPING_LIST_TYPE")
@Table(name = "SHOPPING_LIST_TYPE")
public class ShoppingListTypeEntity {
    @Id
    @Column(name = "SHOPPING_LIST_TYPE_ID")
    private String shoppingListTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
