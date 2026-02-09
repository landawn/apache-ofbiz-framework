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
@Entity(name = "SECURITY_PERMISSION")
@Table(name = "SECURITY_PERMISSION")
public class SecurityPermissionEntity {
    @Id
    @Column(name = "PERMISSION_ID")
    private String permissionId;

    @Column(name = "DESCRIPTION")
    private String description;
}
