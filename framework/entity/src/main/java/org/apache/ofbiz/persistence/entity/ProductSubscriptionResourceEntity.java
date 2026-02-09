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
@Entity(name = "PRODUCT_SUBSCRIPTION_RESOURCE")
@Table(name = "PRODUCT_SUBSCRIPTION_RESOURCE")
public class ProductSubscriptionResourceEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "SUBSCRIPTION_RESOURCE_ID")
    private String subscriptionResourceId;

    @Id
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

    @Column(name = "USE_ROLE_TYPE_ID")
    private String useRoleTypeId;

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
}
