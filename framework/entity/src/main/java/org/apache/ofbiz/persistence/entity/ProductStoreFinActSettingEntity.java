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
@Entity(name = "PRODUCT_STORE_FIN_ACT_SETTING")
@Table(name = "PRODUCT_STORE_FIN_ACT_SETTING")
public class ProductStoreFinActSettingEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Id
    @Column(name = "FIN_ACCOUNT_TYPE_ID")
    private String finAccountTypeId;

    @Column(name = "REQUIRE_PIN_CODE")
    private String requirePinCode;

    @Column(name = "VALIDATE_G_C_FIN_ACCT")
    private String validateGCFinAcct;

    @Column(name = "ACCOUNT_CODE_LENGTH")
    private BigDecimal accountCodeLength;

    @Column(name = "PIN_CODE_LENGTH")
    private BigDecimal pinCodeLength;

    @Column(name = "ACCOUNT_VALID_DAYS")
    private BigDecimal accountValidDays;

    @Column(name = "AUTH_VALID_DAYS")
    private BigDecimal authValidDays;

    @Column(name = "PURCHASE_SURVEY_ID")
    private String purchaseSurveyId;

    @Column(name = "PURCH_SURVEY_SEND_TO")
    private String purchSurveySendTo;

    @Column(name = "PURCH_SURVEY_COPY_ME")
    private String purchSurveyCopyMe;

    @Column(name = "ALLOW_AUTH_TO_NEGATIVE")
    private String allowAuthToNegative;

    @Column(name = "MIN_BALANCE")
    private BigDecimal minBalance;

    @Column(name = "REPLENISH_THRESHOLD")
    private BigDecimal replenishThreshold;

    @Column(name = "REPLENISH_METHOD_ENUM_ID")
    private String replenishMethodEnumId;
}
