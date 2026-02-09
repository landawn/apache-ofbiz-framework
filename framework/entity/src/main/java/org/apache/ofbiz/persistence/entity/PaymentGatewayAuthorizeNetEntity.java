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
@Entity(name = "PAYMENT_GATEWAY_AUTHORIZE_NET")
@Table(name = "PAYMENT_GATEWAY_AUTHORIZE_NET")
public class PaymentGatewayAuthorizeNetEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "TRANSACTION_URL")
    private String transactionUrl;

    @Column(name = "CERTIFICATE_ALIAS")
    private String certificateAlias;

    @Column(name = "API_VERSION")
    private String apiVersion;

    @Column(name = "DELIMITED_DATA")
    private String delimitedData;

    @Column(name = "DELIMITER_CHAR")
    private String delimiterChar;

    @Column(name = "CP_VERSION")
    private String cpVersion;

    @Column(name = "CP_MARKET_TYPE")
    private String cpMarketType;

    @Column(name = "CP_DEVICE_TYPE")
    private String cpDeviceType;

    @Column(name = "METHOD")
    private String method;

    @Column(name = "EMAIL_CUSTOMER")
    private String emailCustomer;

    @Column(name = "EMAIL_MERCHANT")
    private String emailMerchant;

    @Column(name = "TEST_MODE")
    private String testMode;

    @Column(name = "RELAY_RESPONSE")
    private String relayResponse;

    @Column(name = "TRAN_KEY")
    private String tranKey;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "PWD")
    private String pwd;

    @Column(name = "TRANS_DESCRIPTION")
    private String transDescription;

    @Column(name = "DUPLICATE_WINDOW")
    private BigDecimal duplicateWindow;
}
