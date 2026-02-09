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
@Entity(name = "COMPONENT")
@Table(name = "COMPONENT")
public class ComponentEntity {
    @Id
    @Column(name = "COMPONENT_NAME")
    private String componentName;

    @Column(name = "ROOT_LOCATION")
    private String rootLocation;
}
