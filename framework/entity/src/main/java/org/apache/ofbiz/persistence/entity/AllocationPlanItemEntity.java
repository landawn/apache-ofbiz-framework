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
@Entity(name = "ALLOCATION_PLAN_ITEM")
@Table(name = "ALLOCATION_PLAN_ITEM")
public class AllocationPlanItemEntity {
    @Id
    @Column(name = "PLAN_ID")
    private String planId;

    @Id
    @Column(name = "PLAN_ITEM_SEQ_ID")
    private String planItemSeqId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PLAN_METHOD_ENUM_ID")
    private String planMethodEnumId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "ALLOCATED_QUANTITY")
    private BigDecimal allocatedQuantity;

    @Column(name = "PRIORITY_SEQ_ID")
    private String prioritySeqId;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
