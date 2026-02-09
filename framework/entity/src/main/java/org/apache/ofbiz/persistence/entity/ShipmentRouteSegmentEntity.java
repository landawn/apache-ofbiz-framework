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
@Entity(name = "SHIPMENT_ROUTE_SEGMENT")
@Table(name = "SHIPMENT_ROUTE_SEGMENT")
public class ShipmentRouteSegmentEntity {
    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Id
    @Column(name = "SHIPMENT_ROUTE_SEGMENT_ID")
    private String shipmentRouteSegmentId;

    @Column(name = "DELIVERY_ID")
    private String deliveryId;

    @Column(name = "ORIGIN_FACILITY_ID")
    private String originFacilityId;

    @Column(name = "DEST_FACILITY_ID")
    private String destFacilityId;

    @Column(name = "ORIGIN_CONTACT_MECH_ID")
    private String originContactMechId;

    @Column(name = "ORIGIN_TELECOM_NUMBER_ID")
    private String originTelecomNumberId;

    @Column(name = "DEST_CONTACT_MECH_ID")
    private String destContactMechId;

    @Column(name = "DEST_TELECOM_NUMBER_ID")
    private String destTelecomNumberId;

    @Column(name = "CARRIER_PARTY_ID")
    private String carrierPartyId;

    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "CARRIER_SERVICE_STATUS_ID")
    private String carrierServiceStatusId;

    @Column(name = "CARRIER_DELIVERY_ZONE")
    private String carrierDeliveryZone;

    @Column(name = "CARRIER_RESTRICTION_CODES")
    private String carrierRestrictionCodes;

    @Column(name = "CARRIER_RESTRICTION_DESC")
    private String carrierRestrictionDesc;

    @Column(name = "BILLING_WEIGHT")
    private BigDecimal billingWeight;

    @Column(name = "BILLING_WEIGHT_UOM_ID")
    private String billingWeightUomId;

    @Column(name = "ACTUAL_TRANSPORT_COST")
    private BigDecimal actualTransportCost;

    @Column(name = "ACTUAL_SERVICE_COST")
    private BigDecimal actualServiceCost;

    @Column(name = "ACTUAL_OTHER_COST")
    private BigDecimal actualOtherCost;

    @Column(name = "ACTUAL_COST")
    private BigDecimal actualCost;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "ACTUAL_START_DATE")
    private Timestamp actualStartDate;

    @Column(name = "ACTUAL_ARRIVAL_DATE")
    private Timestamp actualArrivalDate;

    @Column(name = "ESTIMATED_START_DATE")
    private Timestamp estimatedStartDate;

    @Column(name = "ESTIMATED_ARRIVAL_DATE")
    private Timestamp estimatedArrivalDate;

    @Column(name = "TRACKING_ID_NUMBER")
    private String trackingIdNumber;

    @Column(name = "TRACKING_DIGEST")
    private String trackingDigest;

    @Column(name = "UPDATED_BY_USER_LOGIN_ID")
    private String updatedByUserLoginId;

    @Column(name = "LAST_UPDATED_DATE")
    private Timestamp lastUpdatedDate;

    @Column(name = "HOME_DELIVERY_TYPE")
    private String homeDeliveryType;

    @Column(name = "HOME_DELIVERY_DATE")
    private Timestamp homeDeliveryDate;

    @Column(name = "THIRD_PARTY_ACCOUNT_NUMBER")
    private String thirdPartyAccountNumber;

    @Column(name = "THIRD_PARTY_POSTAL_CODE")
    private String thirdPartyPostalCode;

    @Column(name = "THIRD_PARTY_COUNTRY_GEO_CODE")
    private String thirdPartyCountryGeoCode;

    @Column(name = "UPS_HIGH_VALUE_REPORT")
    private byte[] upsHighValueReport;
}
