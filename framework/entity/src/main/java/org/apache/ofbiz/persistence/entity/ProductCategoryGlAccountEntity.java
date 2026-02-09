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
@Entity(name = "PRODUCT_CATEGORY_GL_ACCOUNT")
@Table(name = "PRODUCT_CATEGORY_GL_ACCOUNT")
public class ProductCategoryGlAccountEntity {
    @Id
    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Id
    @Column(name = "GL_ACCOUNT_TYPE_ID")
    private String glAccountTypeId;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;
}
