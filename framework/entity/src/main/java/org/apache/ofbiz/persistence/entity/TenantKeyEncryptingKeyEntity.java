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
@Entity(name = "TENANT_KEY_ENCRYPTING_KEY")
@Table(name = "TENANT_KEY_ENCRYPTING_KEY")
public class TenantKeyEncryptingKeyEntity {
    @Id
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "KEK_TEXT")
    private String kekText;
}
