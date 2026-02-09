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
@Entity(name = "PRODUCT_SEARCH_RESULT")
@Table(name = "PRODUCT_SEARCH_RESULT")
public class ProductSearchResultEntity {
    @Id
    @Column(name = "PRODUCT_SEARCH_RESULT_ID")
    private String productSearchResultId;

    @Column(name = "VISIT_ID")
    private String visitId;

    @Column(name = "ORDER_BY_NAME")
    private String orderByName;

    @Column(name = "IS_ASCENDING")
    private String isAscending;

    @Column(name = "NUM_RESULTS")
    private BigDecimal numResults;

    @Column(name = "SECONDS_TOTAL")
    private Double secondsTotal;

    @Column(name = "SEARCH_DATE")
    private Timestamp searchDate;
}
