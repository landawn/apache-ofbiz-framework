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
@Entity(name = "TENANT_DOMAIN_NAME")
@Table(name = "TENANT_DOMAIN_NAME")
public class TenantDomainNameEntity {
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Id
    @Column(name = "DOMAIN_NAME")
    private String domainName;
}
