package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CREDIT_CARD")
@Table(name = "CREDIT_CARD")
public class CreditCardEntity {
    @Id
    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "CARD_TYPE")
    private String cardType;

    @Column(name = "CARD_NUMBER")
    private String cardNumber;

    @Column(name = "VALID_FROM_DATE")
    private String validFromDate;

    @Column(name = "EXPIRE_DATE")
    private String expireDate;

    @Column(name = "ISSUE_NUMBER")
    private String issueNumber;

    @Column(name = "COMPANY_NAME_ON_CARD")
    private String companyNameOnCard;

    @Column(name = "TITLE_ON_CARD")
    private String titleOnCard;

    @Column(name = "FIRST_NAME_ON_CARD")
    private String firstNameOnCard;

    @Column(name = "MIDDLE_NAME_ON_CARD")
    private String middleNameOnCard;

    @Column(name = "LAST_NAME_ON_CARD")
    private String lastNameOnCard;

    @Column(name = "SUFFIX_ON_CARD")
    private String suffixOnCard;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "CONSECUTIVE_FAILED_AUTHS")
    private BigDecimal consecutiveFailedAuths;

    @Column(name = "LAST_FAILED_AUTH_DATE")
    private Timestamp lastFailedAuthDate;

    @Column(name = "CONSECUTIVE_FAILED_NSF")
    private BigDecimal consecutiveFailedNsf;

    @Column(name = "LAST_FAILED_NSF_DATE")
    private Timestamp lastFailedNsfDate;
}
