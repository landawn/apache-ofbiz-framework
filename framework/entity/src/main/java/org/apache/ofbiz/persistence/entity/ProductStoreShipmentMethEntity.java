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
@Entity(name = "PRODUCT_STORE_SHIPMENT_METH")
@Table(name = "PRODUCT_STORE_SHIPMENT_METH")
public class ProductStoreShipmentMethEntity {
    @Id
    @Column(name = "PRODUCT_STORE_SHIP_METH_ID")
    private String productStoreShipMethId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "COMPANY_PARTY_ID")
    private String companyPartyId;

    @Column(name = "MIN_WEIGHT")
    private BigDecimal minWeight;

    @Column(name = "MAX_WEIGHT")
    private BigDecimal maxWeight;

    @Column(name = "MIN_SIZE")
    private BigDecimal minSize;

    @Column(name = "MAX_SIZE")
    private BigDecimal maxSize;

    @Column(name = "MIN_TOTAL")
    private BigDecimal minTotal;

    @Column(name = "MAX_TOTAL")
    private BigDecimal maxTotal;

    @Column(name = "ALLOW_USPS_ADDR")
    private String allowUspsAddr;

    @Column(name = "REQUIRE_USPS_ADDR")
    private String requireUspsAddr;

    @Column(name = "ALLOW_COMPANY_ADDR")
    private String allowCompanyAddr;

    @Column(name = "REQUIRE_COMPANY_ADDR")
    private String requireCompanyAddr;

    @Column(name = "INCLUDE_NO_CHARGE_ITEMS")
    private String includeNoChargeItems;

    @Column(name = "INCLUDE_FEATURE_GROUP")
    private String includeFeatureGroup;

    @Column(name = "EXCLUDE_FEATURE_GROUP")
    private String excludeFeatureGroup;

    @Column(name = "INCLUDE_GEO_ID")
    private String includeGeoId;

    @Column(name = "EXCLUDE_GEO_ID")
    private String excludeGeoId;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "CONFIG_PROPS")
    private String configProps;

    @Column(name = "SHIPMENT_CUSTOM_METHOD_ID")
    private String shipmentCustomMethodId;

    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "SEQUENCE_NUMBER")
    private BigDecimal sequenceNumber;

    @Column(name = "ALLOWANCE_PERCENT")
    private BigDecimal allowancePercent;

    @Column(name = "MINIMUM_PRICE")
    private BigDecimal minimumPrice;
}
