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
@Entity(name = "SHIPMENT_PACKAGE_ROUTE_SEG")
@Table(name = "SHIPMENT_PACKAGE_ROUTE_SEG")
public class ShipmentPackageRouteSegEntity {
    @Id
    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Id
    @Column(name = "SHIPMENT_PACKAGE_SEQ_ID")
    private String shipmentPackageSeqId;

    @Id
    @Column(name = "SHIPMENT_ROUTE_SEGMENT_ID")
    private String shipmentRouteSegmentId;

    @Column(name = "TRACKING_CODE")
    private String trackingCode;

    @Column(name = "BOX_NUMBER")
    private String boxNumber;

    @Column(name = "LABEL_IMAGE")
    private byte[] labelImage;

    @Column(name = "LABEL_INTL_SIGN_IMAGE")
    private byte[] labelIntlSignImage;

    @Column(name = "LABEL_HTML")
    private String labelHtml;

    @Column(name = "LABEL_PRINTED")
    private String labelPrinted;

    @Column(name = "INTERNATIONAL_INVOICE")
    private byte[] internationalInvoice;

    @Column(name = "PACKAGE_TRANSPORT_COST")
    private BigDecimal packageTransportCost;

    @Column(name = "PACKAGE_SERVICE_COST")
    private BigDecimal packageServiceCost;

    @Column(name = "PACKAGE_OTHER_COST")
    private BigDecimal packageOtherCost;

    @Column(name = "COD_AMOUNT")
    private BigDecimal codAmount;

    @Column(name = "INSURED_AMOUNT")
    private BigDecimal insuredAmount;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;
}
