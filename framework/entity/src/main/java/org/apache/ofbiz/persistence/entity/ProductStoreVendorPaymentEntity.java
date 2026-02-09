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
@Entity(name = "PRODUCT_STORE_VENDOR_PAYMENT")
@Table(name = "PRODUCT_STORE_VENDOR_PAYMENT")
public class ProductStoreVendorPaymentEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Id
    @Column(name = "VENDOR_PARTY_ID")
    private String vendorPartyId;

    @Id
    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Id
    @Column(name = "CREDIT_CARD_ENUM_ID")
    private String creditCardEnumId;
}
