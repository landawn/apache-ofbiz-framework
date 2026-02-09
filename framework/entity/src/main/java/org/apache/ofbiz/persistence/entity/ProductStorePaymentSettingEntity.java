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
@Entity(name = "PRODUCT_STORE_PAYMENT_SETTING")
@Table(name = "PRODUCT_STORE_PAYMENT_SETTING")
public class ProductStorePaymentSettingEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Id
    @Column(name = "PAYMENT_METHOD_TYPE_ID")
    private String paymentMethodTypeId;

    @Id
    @Column(name = "PAYMENT_SERVICE_TYPE_ENUM_ID")
    private String paymentServiceTypeEnumId;

    @Column(name = "PAYMENT_SERVICE")
    private String paymentService;

    @Column(name = "PAYMENT_CUSTOM_METHOD_ID")
    private String paymentCustomMethodId;

    @Column(name = "PAYMENT_GATEWAY_CONFIG_ID")
    private String paymentGatewayConfigId;

    @Column(name = "PAYMENT_PROPERTIES_PATH")
    private String paymentPropertiesPath;

    @Column(name = "APPLY_TO_ALL_PRODUCTS")
    private String applyToAllProducts;
}
