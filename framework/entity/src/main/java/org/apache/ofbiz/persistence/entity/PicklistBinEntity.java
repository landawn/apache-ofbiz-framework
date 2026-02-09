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
@Entity(name = "PICKLIST_BIN")
@Table(name = "PICKLIST_BIN")
public class PicklistBinEntity {
    @Id
    @Column(name = "PICKLIST_BIN_ID")
    private String picklistBinId;

    @Column(name = "PICKLIST_ID")
    private String picklistId;

    @Column(name = "BIN_LOCATION_NUMBER")
    private BigDecimal binLocationNumber;

    @Column(name = "PRIMARY_ORDER_ID")
    private String primaryOrderId;

    @Column(name = "PRIMARY_SHIP_GROUP_SEQ_ID")
    private String primaryShipGroupSeqId;
}
