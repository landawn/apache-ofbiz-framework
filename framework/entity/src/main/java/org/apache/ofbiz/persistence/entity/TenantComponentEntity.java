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
@Entity(name = "TENANT_COMPONENT")
@Table(name = "TENANT_COMPONENT")
public class TenantComponentEntity {
    @Id
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Id
    @Column(name = "COMPONENT_NAME")
    private String componentName;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
