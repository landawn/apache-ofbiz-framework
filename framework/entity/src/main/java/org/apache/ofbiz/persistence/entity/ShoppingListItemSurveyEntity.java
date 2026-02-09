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
@Entity(name = "SHOPPING_LIST_ITEM_SURVEY")
@Table(name = "SHOPPING_LIST_ITEM_SURVEY")
public class ShoppingListItemSurveyEntity {
    @Id
    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Id
    @Column(name = "SHOPPING_LIST_ITEM_SEQ_ID")
    private String shoppingListItemSeqId;

    @Id
    @Column(name = "SURVEY_RESPONSE_ID")
    private String surveyResponseId;
}
