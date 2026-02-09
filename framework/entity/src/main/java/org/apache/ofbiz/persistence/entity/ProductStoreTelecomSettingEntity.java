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
@Entity(name = "PRODUCT_STORE_TELECOM_SETTING")
@Table(name = "PRODUCT_STORE_TELECOM_SETTING")
public class ProductStoreTelecomSettingEntity {
    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Id
    @Column(name = "TELECOM_METHOD_TYPE_ID")
    private String telecomMethodTypeId;

    @Id
    @Column(name = "TELECOM_MSG_TYPE_ENUM_ID")
    private String telecomMsgTypeEnumId;

    @Column(name = "TELECOM_CUSTOM_METHOD_ID")
    private String telecomCustomMethodId;

    @Column(name = "TELECOM_GATEWAY_CONFIG_ID")
    private String telecomGatewayConfigId;
}
