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
@Entity(name = "SUBSCRIPTION")
@Table(name = "SUBSCRIPTION")
public class SubscriptionEntity {
    @Id
    @Column(name = "SUBSCRIPTION_ID")
    private String subscriptionId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SUBSCRIPTION_RESOURCE_ID")
    private String subscriptionResourceId;

    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "ORIGINATED_FROM_PARTY_ID")
    private String originatedFromPartyId;

    @Column(name = "ORIGINATED_FROM_ROLE_TYPE_ID")
    private String originatedFromRoleTypeId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "PARTY_NEED_ID")
    private String partyNeedId;

    @Column(name = "NEED_TYPE_ID")
    private String needTypeId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "SUBSCRIPTION_TYPE_ID")
    private String subscriptionTypeId;

    @Column(name = "EXTERNAL_SUBSCRIPTION_ID")
    private String externalSubscriptionId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PURCHASE_FROM_DATE")
    private Timestamp purchaseFromDate;

    @Column(name = "PURCHASE_THRU_DATE")
    private Timestamp purchaseThruDate;

    @Column(name = "MAX_LIFE_TIME")
    private BigDecimal maxLifeTime;

    @Column(name = "MAX_LIFE_TIME_UOM_ID")
    private String maxLifeTimeUomId;

    @Column(name = "AVAILABLE_TIME")
    private BigDecimal availableTime;

    @Column(name = "AVAILABLE_TIME_UOM_ID")
    private String availableTimeUomId;

    @Column(name = "USE_COUNT_LIMIT")
    private BigDecimal useCountLimit;

    @Column(name = "USE_TIME")
    private BigDecimal useTime;

    @Column(name = "USE_TIME_UOM_ID")
    private String useTimeUomId;

    @Column(name = "AUTOMATIC_EXTEND")
    private String automaticExtend;

    @Column(name = "CANCL_AUTM_EXT_TIME")
    private BigDecimal canclAutmExtTime;

    @Column(name = "CANCL_AUTM_EXT_TIME_UOM_ID")
    private String canclAutmExtTimeUomId;

    @Column(name = "GRACE_PERIOD_ON_EXPIRY")
    private BigDecimal gracePeriodOnExpiry;

    @Column(name = "GRACE_PERIOD_ON_EXPIRY_UOM_ID")
    private String gracePeriodOnExpiryUomId;

    @Column(name = "EXPIRATION_COMPLETED_DATE")
    private Timestamp expirationCompletedDate;
}
