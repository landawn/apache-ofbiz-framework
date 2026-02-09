package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ALLOCATION_PLAN_HEADER")
@Table(name = "ALLOCATION_PLAN_HEADER")
public class AllocationPlanHeaderEntity {
    @Id
    @Column(name = "PLAN_ID")
    private String planId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PLAN_TYPE_ID")
    private String planTypeId;

    @Column(name = "PLAN_NAME")
    private String planName;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
