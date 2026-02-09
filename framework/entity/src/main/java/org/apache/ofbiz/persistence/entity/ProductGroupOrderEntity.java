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
@Entity(name = "PRODUCT_GROUP_ORDER")
@Table(name = "PRODUCT_GROUP_ORDER")
public class ProductGroupOrderEntity {
    @Id
    @Column(name = "GROUP_ORDER_ID")
    private String groupOrderId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "REQ_ORDER_QTY")
    private BigDecimal reqOrderQty;

    @Column(name = "SOLD_ORDER_QTY")
    private BigDecimal soldOrderQty;

    @Column(name = "JOB_ID")
    private String jobId;
}
