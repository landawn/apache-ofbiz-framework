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
@Entity(name = "PRODUCT_REVIEW")
@Table(name = "PRODUCT_REVIEW")
public class ProductReviewEntity {
    @Id
    @Column(name = "PRODUCT_REVIEW_ID")
    private String productReviewId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "USER_LOGIN_ID")
    private String userLoginId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "POSTED_ANONYMOUS")
    private String postedAnonymous;

    @Column(name = "POSTED_DATE_TIME")
    private Timestamp postedDateTime;

    @Column(name = "PRODUCT_RATING")
    private BigDecimal productRating;

    @Column(name = "PRODUCT_REVIEW")
    private String productReview;
}
