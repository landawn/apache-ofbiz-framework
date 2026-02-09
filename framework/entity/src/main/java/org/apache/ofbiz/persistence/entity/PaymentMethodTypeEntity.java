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
@Entity(name = "PAYMENT_METHOD_TYPE")
@Table(name = "PAYMENT_METHOD_TYPE")
public class PaymentMethodTypeEntity {
    @Id
    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_GL_ACCOUNT_ID")
    private String defaultGlAccountId;
}
