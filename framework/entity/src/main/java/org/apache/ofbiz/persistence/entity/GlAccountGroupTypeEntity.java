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
@Entity(name = "GL_ACCOUNT_GROUP_TYPE")
@Table(name = "GL_ACCOUNT_GROUP_TYPE")
public class GlAccountGroupTypeEntity {
    @Id
    @Column(name = "GL_ACCOUNT_GROUP_TYPE_ID")
    private String glAccountGroupTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
