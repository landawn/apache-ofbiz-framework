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
@Entity(name = "PERF_REVIEW_ITEM")
@Table(name = "PERF_REVIEW_ITEM")
public class PerfReviewItemEntity {
    @Id
    @Column(name = "EMPLOYEE_PARTY_ID")
    private String employeePartyId;

    @Id
    @Column(name = "EMPLOYEE_ROLE_TYPE_ID")
    private String employeeRoleTypeId;

    @Id
    @Column(name = "PERF_REVIEW_ID")
    private String perfReviewId;

    @Id
    @Column(name = "PERF_REVIEW_ITEM_SEQ_ID")
    private String perfReviewItemSeqId;

    @Column(name = "PERF_REVIEW_ITEM_TYPE_ID")
    private String perfReviewItemTypeId;

    @Column(name = "PERF_RATING_TYPE_ID")
    private String perfRatingTypeId;

    @Column(name = "COMMENTS")
    private String comments;
}
