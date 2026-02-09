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
@Entity(name = "ENTITY_SYNC_INCLUDE")
@Table(name = "ENTITY_SYNC_INCLUDE")
public class EntitySyncIncludeEntity {
    @Id
    @Column(name = "ENTITY_SYNC_ID")
    private String entitySyncId;

    @Id
    @Column(name = "ENTITY_OR_PACKAGE")
    private String entityOrPackage;

    @Column(name = "APPL_ENUM_ID")
    private String applEnumId;
}
