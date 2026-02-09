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
@Entity(name = "PAYMENT_GATEWAY_CONFIG")
@Table(name = "PAYMENT_GATEWAY_CONFIG")
public class PaymentGatewayConfigEntity {
    @Id
    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "PAYMENT_GATEWAY_CONFIG_TYPE_ID")
    private String paymentGatewayConfigTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
