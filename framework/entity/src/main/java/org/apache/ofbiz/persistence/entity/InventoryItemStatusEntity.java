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
@Entity(name = "INVENTORY_ITEM_STATUS")
@Table(name = "INVENTORY_ITEM_STATUS")
public class InventoryItemStatusEntity {
    @Id
    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Id
    @Column(name = "STATUS_ID")
    private String statusId;

    @Id
    @Column(name = "STATUS_DATETIME")
    private Timestamp statusDatetime;

    @Column(name = "STATUS_END_DATETIME")
    private Timestamp statusEndDatetime;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;

    @Column(name = "OWNER_PARTY_ID")
    private String ownerPartyId;

    @Column(name = "PRODUCT_ID")
    private String productId;
}
