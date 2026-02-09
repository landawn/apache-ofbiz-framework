package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "SHOPPING_LIST")
@Table(name = "SHOPPING_LIST")
public class ShoppingListEntity {
    @Id
    @Column(name = "SHOPPING_LIST_ID")
    private String shoppingListId;

    @Column(name = "SHOPPING_LIST_TYPE_ID")
    private String shoppingListTypeId;

    @Column(name = "PARENT_SHOPPING_LIST_ID")
    private String parentShoppingListId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "VISITOR_ID")
    private String visitorId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "LIST_NAME")
    private String listName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_PUBLIC")
    private String isPublic;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    @Column(name = "CURRENCY_UOM")
    private String currencyUom;

    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "CARRIER_PARTY_ID")
    private String carrierPartyId;

    @Column(name = "CARRIER_ROLE_TYPE_ID")
    private String carrierRoleTypeId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;

    @Column(name = "LAST_ORDERED_DATE")
    private Timestamp lastOrderedDate;

    @Column(name = "LAST_ADMIN_MODIFIED")
    private Timestamp lastAdminModified;

    @Column(name = "PRODUCT_PROMO_CODE_ID")
    private String productPromoCodeId;
}
