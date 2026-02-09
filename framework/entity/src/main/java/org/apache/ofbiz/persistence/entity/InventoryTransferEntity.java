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
@Entity(name = "INVENTORY_TRANSFER")
@Table(name = "INVENTORY_TRANSFER")
public class InventoryTransferEntity {
    @Id
    @Column(name = "INVENTORY_TRANSFER_ID")
    private String inventoryTransferId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "LOCATION_SEQ_ID")
    private String locationSeqId;

    @Column(name = "CONTAINER_ID")
    private String containerId;

    @Column(name = "FACILITY_ID_TO")
    private String facilityIdTo;

    @Column(name = "LOCATION_SEQ_ID_TO")
    private String locationSeqIdTo;

    @Column(name = "CONTAINER_ID_TO")
    private String containerIdTo;

    @Column(name = "ITEM_ISSUANCE_ID")
    private String itemIssuanceId;

    @Column(name = "SEND_DATE")
    private Timestamp sendDate;

    @Column(name = "RECEIVE_DATE")
    private Timestamp receiveDate;

    @Column(name = "COMMENTS")
    private String comments;
}
