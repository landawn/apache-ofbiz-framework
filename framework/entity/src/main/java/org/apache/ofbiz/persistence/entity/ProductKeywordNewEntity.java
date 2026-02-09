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
@Entity(name = "PRODUCT_KEYWORD_NEW")
@Table(name = "PRODUCT_KEYWORD_NEW")
public class ProductKeywordNewEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "KEYWORD")
    private String keyword;

    @Id
    @Column(name = "KEYWORD_TYPE_ID")
    private String keywordTypeId;

    @Column(name = "RELEVANCY_WEIGHT")
    private BigDecimal relevancyWeight;

    @Column(name = "STATUS_ID")
    private String statusId;
}
