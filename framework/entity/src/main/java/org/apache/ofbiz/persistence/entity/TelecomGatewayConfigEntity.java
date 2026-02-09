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
@Entity(name = "TELECOM_GATEWAY_CONFIG")
@Table(name = "TELECOM_GATEWAY_CONFIG")
public class TelecomGatewayConfigEntity {
    @Id
    @Column(name = "TELECOM_GATEWAY_CONFIG_ID")
    private String telecomGatewayConfigId;

    @Column(name = "DESCRIPTION")
    private String description;
}
