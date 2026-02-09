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
@Entity(name = "PAYMENT_GROUP")
@Table(name = "PAYMENT_GROUP")
public class PaymentGroupEntity {
    @Id
    @Column(name = "PAYMENT_GROUP_ID")
    private String paymentGroupId;

    @Column(name = "PAYMENT_GROUP_TYPE_ID")
    private String paymentGroupTypeId;

    @Column(name = "PAYMENT_GROUP_NAME")
    private String paymentGroupName;
}
