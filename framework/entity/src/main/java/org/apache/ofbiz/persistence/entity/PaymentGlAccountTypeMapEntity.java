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
@Entity(name = "PAYMENT_GL_ACCOUNT_TYPE_MAP")
@Table(name = "PAYMENT_GL_ACCOUNT_TYPE_MAP")
public class PaymentGlAccountTypeMapEntity {
    @Id
    @Column(name = "PAYMENT_TYPE_ID")
    private String paymentTypeId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "GL_ACCOUNT_TYPE_ID")
    private String glAccountTypeId;
}
