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
@Entity(name = "GL_ACCOUNT_CATEGORY_TYPE")
@Table(name = "GL_ACCOUNT_CATEGORY_TYPE")
public class GlAccountCategoryTypeEntity {
    @Id
    @Column(name = "GL_ACCOUNT_CATEGORY_TYPE_ID")
    private String glAccountCategoryTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
