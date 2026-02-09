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
@Entity(name = "ORDER_ITEM_SHIP_GROUP")
@Table(name = "ORDER_ITEM_SHIP_GROUP")
public class OrderItemShipGroupEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "SHIP_GROUP_SEQ_ID")
    private String shipGroupSeqId;

    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Column(name = "SUPPLIER_PARTY_ID")
    private String supplierPartyId;

    @Column(name = "SUPPLIER_AGREEMENT_ID")
    private String supplierAgreementId;

    @Column(name = "VENDOR_PARTY_ID")
    private String vendorPartyId;

    @Column(name = "CARRIER_PARTY_ID")
    private String carrierPartyId;

    @Column(name = "CARRIER_ROLE_TYPE_ID")
    private String carrierRoleTypeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "TELECOM_CONTACT_MECH_ID")
    private String telecomContactMechId;

    @Column(name = "TRACKING_NUMBER")
    private String trackingNumber;

    @Column(name = "SHIPPING_INSTRUCTIONS")
    private String shippingInstructions;

    @Column(name = "MAY_SPLIT")
    private String maySplit;

    @Column(name = "GIFT_MESSAGE")
    private String giftMessage;

    @Column(name = "IS_GIFT")
    private String isGift;

    @Column(name = "SHIP_AFTER_DATE")
    private Timestamp shipAfterDate;

    @Column(name = "SHIP_BY_DATE")
    private Timestamp shipByDate;

    @Column(name = "ESTIMATED_SHIP_DATE")
    private Timestamp estimatedShipDate;

    @Column(name = "ESTIMATED_DELIVERY_DATE")
    private Timestamp estimatedDeliveryDate;
}
