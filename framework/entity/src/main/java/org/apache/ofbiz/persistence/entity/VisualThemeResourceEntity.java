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
@Entity(name = "VISUAL_THEME_RESOURCE")
@Table(name = "VISUAL_THEME_RESOURCE")
public class VisualThemeResourceEntity {
    @Id
    @Column(name = "VISUAL_THEME_ID")
    private String visualThemeId;

    @Id
    @Column(name = "RESOURCE_TYPE_ENUM_ID")
    private String resourceTypeEnumId;

    @Id
    @Column(name = "SEQUENCE_ID")
    private String sequenceId;

    @Column(name = "RESOURCE_VALUE")
    private String resourceValue;
}
