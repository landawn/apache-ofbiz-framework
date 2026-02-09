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
@Entity(name = "SYSTEM_PROPERTY")
@Table(name = "SYSTEM_PROPERTY")
public class SystemPropertyEntity {
    @Id
    @Column(name = "SYSTEM_RESOURCE_ID")
    private String systemResourceId;

    @Id
    @Column(name = "SYSTEM_PROPERTY_ID")
    private String systemPropertyId;

    @Column(name = "SYSTEM_PROPERTY_VALUE")
    private String systemPropertyValue;

    @Column(name = "DESCRIPTION")
    private String description;
}
