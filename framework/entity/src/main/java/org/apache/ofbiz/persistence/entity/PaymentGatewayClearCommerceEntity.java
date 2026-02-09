package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PAYMENT_GATEWAY_CLEAR_COMMERCE")
@Table(name = "PAYMENT_GATEWAY_CLEAR_COMMERCE")
public class PaymentGatewayClearCommerceEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "SOURCE_ID")
    private String sourceId;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "CLIENT_ID")
    private String clientId;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PWD")
    private String pwd;

    @Column(name = "USER_ALIAS")
    private String userAlias;

    @Column(name = "EFFECTIVE_ALIAS")
    private String effectiveAlias;

    @Column(name = "PROCESS_MODE")
    private String processMode;

    @Column(name = "SERVER_U_R_L")
    private String serverURL;

    @Column(name = "ENABLE_C_V_M")
    private String enableCVM;
}
