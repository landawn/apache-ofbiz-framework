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
@Entity(name = "GL_XBRL_CLASS")
@Table(name = "GL_XBRL_CLASS")
public class GlXbrlClassEntity {
    @Id
    @Column(name = "GL_XBRL_CLASS_ID")
    private String glXbrlClassId;

    @Column(name = "PARENT_GL_XBRL_CLASS_ID")
    private String parentGlXbrlClassId;

    @Column(name = "DESCRIPTION")
    private String description;
}
