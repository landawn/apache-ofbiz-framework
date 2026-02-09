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
@Entity(name = "SUPPLIER_PRODUCT")
@Table(name = "SUPPLIER_PRODUCT")
public class SupplierProductEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "AVAILABLE_FROM_DATE")
    private Timestamp availableFromDate;

    @Column(name = "AVAILABLE_THRU_DATE")
    private Timestamp availableThruDate;

    @Column(name = "SUPPLIER_PREF_ORDER_ID")
    private String supplierPrefOrderId;

    @Column(name = "SUPPLIER_RATING_TYPE_ID")
    private String supplierRatingTypeId;

    @Column(name = "STANDARD_LEAD_TIME_DAYS")
    private BigDecimal standardLeadTimeDays;

    @Id
    @Column(name = "MINIMUM_ORDER_QUANTITY")
    private BigDecimal minimumOrderQuantity;

    @Column(name = "ORDER_QTY_INCREMENTS")
    private BigDecimal orderQtyIncrements;

    @Column(name = "UNITS_INCLUDED")
    private BigDecimal unitsIncluded;

    @Column(name = "QUANTITY_UOM_ID")
    private String quantityUomId;

    @Column(name = "AGREEMENT_ID")
    private String agreementId;

    @Column(name = "AGREEMENT_ITEM_SEQ_ID")
    private String agreementItemSeqId;

    @Column(name = "LAST_PRICE")
    private BigDecimal lastPrice;

    @Column(name = "SHIPPING_PRICE")
    private BigDecimal shippingPrice;

    @Id
    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "SUPPLIER_PRODUCT_NAME")
    private String supplierProductName;

    @Column(name = "SUPPLIER_PRODUCT_ID")
    private String supplierProductId;

    @Column(name = "CAN_DROP_SHIP")
    private String canDropShip;

    @Column(name = "COMMENTS")
    private String comments;
}
