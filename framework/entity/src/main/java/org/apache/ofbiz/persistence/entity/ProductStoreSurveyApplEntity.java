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
@Entity(name = "PRODUCT_STORE_SURVEY_APPL")
@Table(name = "PRODUCT_STORE_SURVEY_APPL")
public class ProductStoreSurveyApplEntity {
    @Id
    @Column(name = "PRODUCT_STORE_SURVEY_ID")
    private String productStoreSurveyId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "SURVEY_APPL_TYPE_ID")
    private String surveyApplTypeId;

    @Column(name = "GROUP_NAME")
    private String groupName;

    @Column(name = "SURVEY_ID")
    private String surveyId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SURVEY_TEMPLATE")
    private String surveyTemplate;

    @Column(name = "RESULT_TEMPLATE")
    private String resultTemplate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
