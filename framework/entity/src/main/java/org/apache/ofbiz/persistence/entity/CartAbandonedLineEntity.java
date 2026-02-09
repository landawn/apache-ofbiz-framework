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
@Entity(name = "CART_ABANDONED_LINE")
@Table(name = "CART_ABANDONED_LINE")
public class CartAbandonedLineEntity {
    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "CART_ABANDONED_LINE_SEQ_ID")
    private String cartAbandonedLineSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PROD_CATALOG_ID")
    private String prodCatalogId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "RESERV_START")
    private Timestamp reservStart;

    @Column(name = "RESERV_LENGTH")
    private BigDecimal reservLength;

    @Column(name = "RESERV_PERSONS")
    private BigDecimal reservPersons;

    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "RESERV2ND_P_P_PERC")
    private BigDecimal reserv2ndPPPerc;

    @Column(name = "RESERV_NTH_P_P_PERC")
    private BigDecimal reservNthPPPerc;

    @Column(name = "CONFIG_ID")
    private String configId;

    @Column(name = "TOTAL_WITH_ADJUSTMENTS")
    private BigDecimal totalWithAdjustments;

    @Column(name = "WAS_RESERVED")
    private String wasReserved;
}
