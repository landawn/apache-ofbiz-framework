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
@Entity(name = "GL_RESOURCE_TYPE")
@Table(name = "GL_RESOURCE_TYPE")
public class GlResourceTypeEntity {
    @Id
    @Column(name = "GL_RESOURCE_TYPE_ID")
    private String glResourceTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
