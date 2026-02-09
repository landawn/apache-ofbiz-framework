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
@Entity(name = "SHIPMENT")
@Table(name = "SHIPMENT")
public class ShipmentEntity {
    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "SHIPMENT_TYPE_ID")
    private String shipmentTypeId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PRIMARY_ORDER_ID")
    private String primaryOrderId;

    @Column(name = "PRIMARY_RETURN_ID")
    private String primaryReturnId;

    @Column(name = "PRIMARY_SHIP_GROUP_SEQ_ID")
    private String primaryShipGroupSeqId;

    @Column(name = "PICKLIST_BIN_ID")
    private String picklistBinId;

    @Column(name = "ESTIMATED_READY_DATE")
    private Timestamp estimatedReadyDate;

    @Column(name = "ESTIMATED_SHIP_DATE")
    private Timestamp estimatedShipDate;

    @Column(name = "ESTIMATED_SHIP_WORK_EFF_ID")
    private String estimatedShipWorkEffId;

    @Column(name = "ESTIMATED_ARRIVAL_DATE")
    private Timestamp estimatedArrivalDate;

    @Column(name = "ESTIMATED_ARRIVAL_WORK_EFF_ID")
    private String estimatedArrivalWorkEffId;

    @Column(name = "LATEST_CANCEL_DATE")
    private Timestamp latestCancelDate;

    @Column(name = "ESTIMATED_SHIP_COST")
    private BigDecimal estimatedShipCost;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "HANDLING_INSTRUCTIONS")
    private String handlingInstructions;

    @Column(name = "ORIGIN_FACILITY_ID")
    private String originFacilityId;

    @Column(name = "DESTINATION_FACILITY_ID")
    private String destinationFacilityId;

    @Column(name = "ORIGIN_CONTACT_MECH_ID")
    private String originContactMechId;

    @Column(name = "ORIGIN_TELECOM_NUMBER_ID")
    private String originTelecomNumberId;

    @Column(name = "DESTINATION_CONTACT_MECH_ID")
    private String destinationContactMechId;

    @Column(name = "DESTINATION_TELECOM_NUMBER_ID")
    private String destinationTelecomNumberId;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "ADDITIONAL_SHIPPING_CHARGE")
    private BigDecimal additionalShippingCharge;

    @Column(name = "ADDTL_SHIPPING_CHARGE_DESC")
    private String addtlShippingChargeDesc;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
