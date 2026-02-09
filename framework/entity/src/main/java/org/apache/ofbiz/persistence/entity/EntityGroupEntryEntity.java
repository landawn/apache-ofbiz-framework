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
@Entity(name = "ENTITY_GROUP_ENTRY")
@Table(name = "ENTITY_GROUP_ENTRY")
public class EntityGroupEntryEntity {
    @Id
    @Column(name = "ENTITY_GROUP_ID")
    private String entityGroupId;

    @Id
    @Column(name = "ENTITY_OR_PACKAGE")
    private String entityOrPackage;

    @Column(name = "APPL_ENUM_ID")
    private String applEnumId;
}
