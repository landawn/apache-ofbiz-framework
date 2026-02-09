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
@Entity(name = "EFT_ACCOUNT")
@Table(name = "EFT_ACCOUNT")
public class EftAccountEntity {
    @Id
    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "BANK_NAME")
    private String bankName;

    @Column(name = "ROUTING_NUMBER")
    private String routingNumber;

    @Column(name = "ACCOUNT_TYPE")
    private String accountType;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "NAME_ON_ACCOUNT")
    private String nameOnAccount;

    @Column(name = "COMPANY_NAME_ON_ACCOUNT")
    private String companyNameOnAccount;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "YEARS_AT_BANK")
    private BigDecimal yearsAtBank;
}
