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
@Entity(name = "PAYMENT_GATEWAY_RESP_MSG")
@Table(name = "PAYMENT_GATEWAY_RESP_MSG")
public class PaymentGatewayRespMsgEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_RESP_MSG_ID")
    private String paymentGatewayRespMsgId;

    @Column(name = "PAYMENT_GATEWAY_RESPONSE_ID")
    private String paymentGatewayResponseId;

    @Column(name = "PGR_MESSAGE")
    private String pgrMessage;
}
