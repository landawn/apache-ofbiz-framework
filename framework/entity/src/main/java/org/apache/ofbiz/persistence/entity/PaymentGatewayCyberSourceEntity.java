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
@Entity(name = "PAYMENT_GATEWAY_CYBER_SOURCE")
@Table(name = "PAYMENT_GATEWAY_CYBER_SOURCE")
public class PaymentGatewayCyberSourceEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "API_VERSION")
    private String apiVersion;

    @Column(name = "PRODUCTION")
    private String production;

    @Column(name = "KEYS_DIR")
    private String keysDir;

    @Column(name = "KEYS_FILE")
    private String keysFile;

    @Column(name = "LOG_ENABLED")
    private String logEnabled;

    @Column(name = "LOG_DIR")
    private String logDir;

    @Column(name = "LOG_FILE")
    private String logFile;

    @Column(name = "LOG_SIZE")
    private BigDecimal logSize;

    @Column(name = "MERCHANT_DESCR")
    private String merchantDescr;

    @Column(name = "MERCHANT_CONTACT")
    private String merchantContact;

    @Column(name = "AUTO_BILL")
    private String autoBill;

    @Column(name = "ENABLE_DAV")
    private String enableDav;

    @Column(name = "FRAUD_SCORE")
    private String fraudScore;

    @Column(name = "IGNORE_AVS")
    private String ignoreAvs;

    @Column(name = "DISABLE_BILL_AVS")
    private String disableBillAvs;

    @Column(name = "AVS_DECLINE_CODES")
    private String avsDeclineCodes;
}
