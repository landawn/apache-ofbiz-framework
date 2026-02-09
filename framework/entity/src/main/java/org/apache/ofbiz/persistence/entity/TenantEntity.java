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
@Entity(name = "TENANT")
@Table(name = "TENANT")
public class TenantEntity {
    @Id
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "TENANT_NAME")
    private String tenantName;

    @Column(name = "INITIAL_PATH")
    private String initialPath;

    @Column(name = "DISABLED")
    private String disabled;
}
