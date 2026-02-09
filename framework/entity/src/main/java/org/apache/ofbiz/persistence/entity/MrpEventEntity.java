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
@Entity(name = "MRP_EVENT")
@Table(name = "MRP_EVENT")
public class MrpEventEntity {
    @Id
    @Column(name = "MRP_ID")
    private String mrpId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "EVENT_DATE")
    private Timestamp eventDate;

    @Id
    @Column(name = "MRP_EVENT_TYPE_ID")
    private String mrpEventTypeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "QUANTITY")
    private Double quantity;

    @Column(name = "EVENT_NAME")
    private String eventName;

    @Column(name = "IS_LATE")
    private String isLate;

    @Column(name = "FACILITY_ID_TO")
    private String facilityIdTo;
}
