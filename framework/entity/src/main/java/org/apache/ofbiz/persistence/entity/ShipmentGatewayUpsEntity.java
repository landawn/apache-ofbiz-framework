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
@Entity(name = "SHIPMENT_GATEWAY_UPS")
@Table(name = "SHIPMENT_GATEWAY_UPS")
public class ShipmentGatewayUpsEntity {
    @Id
    @Column(name = "SHIPMENT_GATEWAY_CONFIG_ID")
    private String shipmentGatewayConfigId;

    @Column(name = "CONNECT_URL")
    private String connectUrl;

    @Column(name = "CONNECT_TIMEOUT")
    private BigDecimal connectTimeout;

    @Column(name = "SHIPPER_NUMBER")
    private String shipperNumber;

    @Column(name = "BILL_SHIPPER_ACCOUNT_NUMBER")
    private String billShipperAccountNumber;

    @Column(name = "ACCESS_LICENSE_NUMBER")
    private String accessLicenseNumber;

    @Column(name = "ACCESS_USER_ID")
    private String accessUserId;

    @Column(name = "ACCESS_PASSWORD")
    private String accessPassword;

    @Column(name = "SAVE_CERT_INFO")
    private String saveCertInfo;

    @Column(name = "SAVE_CERT_PATH")
    private String saveCertPath;

    @Column(name = "SHIPPER_PICKUP_TYPE")
    private String shipperPickupType;

    @Column(name = "CUSTOMER_CLASSIFICATION")
    private String customerClassification;

    @Column(name = "MAX_ESTIMATE_WEIGHT")
    private BigDecimal maxEstimateWeight;

    @Column(name = "MIN_ESTIMATE_WEIGHT")
    private BigDecimal minEstimateWeight;

    @Column(name = "COD_ALLOW_COD")
    private String codAllowCod;

    @Column(name = "COD_SURCHARGE_AMOUNT")
    private BigDecimal codSurchargeAmount;

    @Column(name = "COD_SURCHARGE_CURRENCY_UOM_ID")
    private String codSurchargeCurrencyUomId;

    @Column(name = "COD_SURCHARGE_APPLY_TO_PACKAGE")
    private String codSurchargeApplyToPackage;

    @Column(name = "COD_FUNDS_CODE")
    private String codFundsCode;

    @Column(name = "DEFAULT_RETURN_LABEL_MEMO")
    private String defaultReturnLabelMemo;

    @Column(name = "DEFAULT_RETURN_LABEL_SUBJECT")
    private String defaultReturnLabelSubject;
}
