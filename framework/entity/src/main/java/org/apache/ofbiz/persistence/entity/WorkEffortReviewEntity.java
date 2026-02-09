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
@Entity(name = "WORK_EFFORT_REVIEW")
@Table(name = "WORK_EFFORT_REVIEW")
public class WorkEffortReviewEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Id
    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Id
    @Column(name = "REVIEW_DATE")
    private Timestamp reviewDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "POSTED_ANONYMOUS")
    private String postedAnonymous;

    @Column(name = "RATING")
    private Double rating;

    @Column(name = "REVIEW_TEXT")
    private String reviewText;
}
