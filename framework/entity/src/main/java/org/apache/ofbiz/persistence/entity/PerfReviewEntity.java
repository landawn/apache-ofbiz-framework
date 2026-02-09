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
@Entity(name = "PERF_REVIEW")
@Table(name = "PERF_REVIEW")
public class PerfReviewEntity {
    @Id
    @Column(name = "EMPLOYEE_PARTY_ID")
    private String employeePartyId;

    @Id
    @Column(name = "EMPLOYEE_ROLE_TYPE_ID")
    private String employeeRoleTypeId;

    @Id
    @Column(name = "PERF_REVIEW_ID")
    private String perfReviewId;

    @Column(name = "MANAGER_PARTY_ID")
    private String managerPartyId;

    @Column(name = "MANAGER_ROLE_TYPE_ID")
    private String managerRoleTypeId;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "EMPL_POSITION_ID")
    private String emplPositionId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "COMMENTS")
    private String comments;
}
