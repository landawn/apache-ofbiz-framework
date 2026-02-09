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
@Entity(name = "QUOTE_ITEM")
@Table(name = "QUOTE_ITEM")
public class QuoteItemEntity {
    @Id
    @Column(name = "QUOTE_ID")
    private String quoteId;

    @Id
    @Column(name = "QUOTE_ITEM_SEQ_ID")
    private String quoteItemSeqId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "DELIVERABLE_TYPE_ID")
    private String deliverableTypeId;

    @Column(name = "SKILL_TYPE_ID")
    private String skillTypeId;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Column(name = "CUST_REQUEST_ITEM_SEQ_ID")
    private String custRequestItemSeqId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "SELECTED_AMOUNT")
    private BigDecimal selectedAmount;

    @Column(name = "QUOTE_UNIT_PRICE")
    private BigDecimal quoteUnitPrice;

    @Column(name = "RESERV_START")
    private Timestamp reservStart;

    @Column(name = "RESERV_LENGTH")
    private BigDecimal reservLength;

    @Column(name = "RESERV_PERSONS")
    private BigDecimal reservPersons;

    @Column(name = "CONFIG_ID")
    private String configId;

    @Column(name = "ESTIMATED_DELIVERY_DATE")
    private Timestamp estimatedDeliveryDate;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "IS_PROMO")
    private String isPromo;

    @Column(name = "LEAD_TIME_DAYS")
    private BigDecimal leadTimeDays;
}
