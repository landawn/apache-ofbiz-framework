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
@Entity(name = "PRODUCT_CATEGORY_LINK")
@Table(name = "PRODUCT_CATEGORY_LINK")
public class ProductCategoryLinkEntity {
    @Id
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Id
    @Column(name = "LINK_SEQ_ID")
    private String linkSeqId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "TITLE_TEXT")
    private String titleText;

    @Column(name = "DETAIL_TEXT")
    private String detailText;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Column(name = "IMAGE_TWO_URL")
    private String imageTwoUrl;

    @Column(name = "LINK_TYPE_ENUM_ID")
    private String linkTypeEnumId;

    @Column(name = "LINK_INFO")
    private String linkInfo;

    @Column(name = "DETAIL_SUB_SCREEN")
    private String detailSubScreen;
}
