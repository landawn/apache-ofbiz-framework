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
@Entity(name = "REQUIREMENT")
@Table(name = "REQUIREMENT")
public class RequirementEntity {
    @Id
    @Column(name = "REQUIREMENT_ID")
    private String requirementId;

    @Column(name = "REQUIREMENT_TYPE_ID")
    private String requirementTypeId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "DELIVERABLE_ID")
    private String deliverableId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REQUIREMENT_START_DATE")
    private Timestamp requirementStartDate;

    @Column(name = "REQUIRED_BY_DATE")
    private Timestamp requiredByDate;

    @Column(name = "ESTIMATED_BUDGET")
    private BigDecimal estimatedBudget;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "USE_CASE")
    private String useCase;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;

    @Column(name = "FACILITY_ID_TO")
    private String facilityIdTo;
}
