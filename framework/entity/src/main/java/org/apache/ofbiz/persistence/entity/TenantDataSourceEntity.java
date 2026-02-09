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
@Entity(name = "TENANT_DATA_SOURCE")
@Table(name = "TENANT_DATA_SOURCE")
public class TenantDataSourceEntity {
    @Id
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Id
    @Column(name = "ENTITY_GROUP_NAME")
    private String entityGroupName;

    @Column(name = "JDBC_URI")
    private String jdbcUri;

    @Column(name = "JDBC_USERNAME")
    private String jdbcUsername;

    @Column(name = "JDBC_PASSWORD")
    private String jdbcPassword;
}
