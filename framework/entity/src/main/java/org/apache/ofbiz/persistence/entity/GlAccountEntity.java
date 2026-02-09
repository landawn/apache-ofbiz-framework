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
@Entity(name = "GL_ACCOUNT")
@Table(name = "GL_ACCOUNT")
public class GlAccountEntity {
    @Id
    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Column(name = "GL_ACCOUNT_TYPE_ID")
    private String glAccountTypeId;

    @Column(name = "GL_ACCOUNT_CLASS_ID")
    private String glAccountClassId;

    @Column(name = "GL_RESOURCE_TYPE_ID")
    private String glResourceTypeId;

    @Column(name = "GL_XBRL_CLASS_ID")
    private String glXbrlClassId;

    @Column(name = "PARENT_GL_ACCOUNT_ID")
    private String parentGlAccountId;

    @Column(name = "ACCOUNT_CODE")
    private String accountCode;

    @Column(name = "ACCOUNT_NAME")
    private String accountName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "EXTERNAL_ID")
    private String externalId;
}
