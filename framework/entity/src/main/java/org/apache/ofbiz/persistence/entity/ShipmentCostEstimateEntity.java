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
@Entity(name = "SHIPMENT_COST_ESTIMATE")
@Table(name = "SHIPMENT_COST_ESTIMATE")
public class ShipmentCostEstimateEntity {
    @Id
    @Column(name = "SHIPMENT_COST_ESTIMATE_ID")
    private String shipmentCostEstimateId;

    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "CARRIER_PARTY_ID")
    private String carrierPartyId;

    @Column(name = "CARRIER_ROLE_TYPE_ID")
    private String carrierRoleTypeId;

    @Column(name = "PRODUCT_STORE_SHIP_METH_ID")
    private String productStoreShipMethId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "GEO_ID_TO")
    private String geoIdTo;

    @Column(name = "GEO_ID_FROM")
    private String geoIdFrom;

    @Column(name = "WEIGHT_BREAK_ID")
    private String weightBreakId;

    @Column(name = "WEIGHT_UOM_ID")
    private String weightUomId;

    @Column(name = "WEIGHT_UNIT_PRICE")
    private BigDecimal weightUnitPrice;

    @Column(name = "QUANTITY_BREAK_ID")
    private String quantityBreakId;

    @Column(name = "QUANTITY_UOM_ID")
    private String quantityUomId;

    @Column(name = "QUANTITY_UNIT_PRICE")
    private BigDecimal quantityUnitPrice;

    @Column(name = "PRICE_BREAK_ID")
    private String priceBreakId;

    @Column(name = "PRICE_UOM_ID")
    private String priceUomId;

    @Column(name = "PRICE_UNIT_PRICE")
    private BigDecimal priceUnitPrice;

    @Column(name = "ORDER_FLAT_PRICE")
    private BigDecimal orderFlatPrice;

    @Column(name = "ORDER_PRICE_PERCENT")
    private BigDecimal orderPricePercent;

    @Column(name = "ORDER_ITEM_FLAT_PRICE")
    private BigDecimal orderItemFlatPrice;

    @Column(name = "SHIPPING_PRICE_PERCENT")
    private BigDecimal shippingPricePercent;

    @Column(name = "PRODUCT_FEATURE_GROUP_ID")
    private String productFeatureGroupId;

    @Column(name = "OVERSIZE_UNIT")
    private BigDecimal oversizeUnit;

    @Column(name = "OVERSIZE_PRICE")
    private BigDecimal oversizePrice;

    @Column(name = "FEATURE_PERCENT")
    private BigDecimal featurePercent;

    @Column(name = "FEATURE_PRICE")
    private BigDecimal featurePrice;
}
