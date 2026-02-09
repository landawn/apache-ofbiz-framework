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
@Entity(name = "SHIPMENT_TIME_ESTIMATE")
@Table(name = "SHIPMENT_TIME_ESTIMATE")
public class ShipmentTimeEstimateEntity {
    @Id
    @Column(name = "SHIPMENT_METHOD_TYPE_ID")
    private String shipmentMethodTypeId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Id
    @Column(name = "GEO_ID_TO")
    private String geoIdTo;

    @Id
    @Column(name = "GEO_ID_FROM")
    private String geoIdFrom;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "LEAD_TIME")
    private BigDecimal leadTime;

    @Column(name = "LEAD_TIME_UOM_ID")
    private String leadTimeUomId;

    @Column(name = "SEQUENCE_NUMBER")
    private BigDecimal sequenceNumber;
}
