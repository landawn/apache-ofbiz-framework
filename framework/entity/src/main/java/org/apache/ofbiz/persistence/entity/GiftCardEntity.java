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
@Entity(name = "GIFT_CARD")
@Table(name = "GIFT_CARD")
public class GiftCardEntity {
    @Id
    @Column(name = "PAYMENT_METHOD_ID")
    private String paymentMethodId;

    @Column(name = "CARD_NUMBER")
    private String cardNumber;

    @Column(name = "PIN_NUMBER")
    private String pinNumber;

    @Column(name = "EXPIRE_DATE")
    private String expireDate;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;
}
