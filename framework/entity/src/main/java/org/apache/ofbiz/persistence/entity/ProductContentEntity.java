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
@Entity(name = "PRODUCT_CONTENT")
@Table(name = "PRODUCT_CONTENT")
public class ProductContentEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "PRODUCT_CONTENT_TYPE_ID")
    private String productContentTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "PURCHASE_FROM_DATE")
    private Timestamp purchaseFromDate;

    @Column(name = "PURCHASE_THRU_DATE")
    private Timestamp purchaseThruDate;

    @Column(name = "USE_COUNT_LIMIT")
    private BigDecimal useCountLimit;

    @Column(name = "USE_TIME")
    private BigDecimal useTime;

    @Column(name = "USE_TIME_UOM_ID")
    private String useTimeUomId;

    @Column(name = "USE_ROLE_TYPE_ID")
    private String useRoleTypeId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
