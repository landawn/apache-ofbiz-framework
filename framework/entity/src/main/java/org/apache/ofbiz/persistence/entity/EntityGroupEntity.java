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
@Entity(name = "ENTITY_GROUP")
@Table(name = "ENTITY_GROUP")
public class EntityGroupEntity {
    @Id
    @Column(name = "ENTITY_GROUP_ID")
    private String entityGroupId;

    @Column(name = "ENTITY_GROUP_NAME")
    private String entityGroupName;
}
