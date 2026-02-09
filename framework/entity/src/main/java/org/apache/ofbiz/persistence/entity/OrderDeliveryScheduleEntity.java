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
@Entity(name = "ORDER_DELIVERY_SCHEDULE")
@Table(name = "ORDER_DELIVERY_SCHEDULE")
public class OrderDeliveryScheduleEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "ESTIMATED_READY_DATE")
    private Timestamp estimatedReadyDate;

    @Column(name = "CARTONS")
    private BigDecimal cartons;

    @Column(name = "SKIDS_PALLETS")
    private BigDecimal skidsPallets;

    @Column(name = "UNITS_PIECES")
    private BigDecimal unitsPieces;

    @Column(name = "TOTAL_CUBIC_SIZE")
    private BigDecimal totalCubicSize;

    @Column(name = "TOTAL_CUBIC_UOM_ID")
    private String totalCubicUomId;

    @Column(name = "TOTAL_WEIGHT")
    private BigDecimal totalWeight;

    @Column(name = "TOTAL_WEIGHT_UOM_ID")
    private String totalWeightUomId;

    @Column(name = "STATUS_ID")
    private String statusId;
}
