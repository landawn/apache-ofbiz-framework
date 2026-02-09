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
@Entity(name = "PAYMENT_GATEWAY_PAYFLOW_PRO")
@Table(name = "PAYMENT_GATEWAY_PAYFLOW_PRO")
public class PaymentGatewayPayflowProEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "CERTS_PATH")
    private String certsPath;

    @Column(name = "HOST_ADDRESS")
    private String hostAddress;

    @Column(name = "HOST_PORT")
    private BigDecimal hostPort;

    @Column(name = "TIMEOUT")
    private BigDecimal timeout;

    @Column(name = "PROXY_ADDRESS")
    private String proxyAddress;

    @Column(name = "PROXY_PORT")
    private BigDecimal proxyPort;

    @Column(name = "PROXY_LOGON")
    private String proxyLogon;

    @Column(name = "PROXY_PASSWORD")
    private String proxyPassword;

    @Column(name = "VENDOR")
    private String vendor;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "PWD")
    private String pwd;

    @Column(name = "PARTNER")
    private String partner;

    @Column(name = "CHECK_AVS")
    private String checkAvs;

    @Column(name = "CHECK_CVV2")
    private String checkCvv2;

    @Column(name = "PRE_AUTH")
    private String preAuth;

    @Column(name = "ENABLE_TRANSMIT")
    private String enableTransmit;

    @Column(name = "LOG_FILE_NAME")
    private String logFileName;

    @Column(name = "LOGGING_LEVEL")
    private BigDecimal loggingLevel;

    @Column(name = "MAX_LOG_FILE_SIZE")
    private BigDecimal maxLogFileSize;

    @Column(name = "STACK_TRACE_ON")
    private String stackTraceOn;

    @Column(name = "REDIRECT_URL")
    private String redirectUrl;

    @Column(name = "RETURN_URL")
    private String returnUrl;

    @Column(name = "CANCEL_RETURN_URL")
    private String cancelReturnUrl;
}
