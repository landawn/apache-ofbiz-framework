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
@Entity(name = "FIXED_ASSET_TYPE_GL_ACCOUNT")
@Table(name = "FIXED_ASSET_TYPE_GL_ACCOUNT")
public class FixedAssetTypeGlAccountEntity {
    @Id
    @Column(name = "FIXED_ASSET_TYPE_ID")
    private String fixedAssetTypeId;

    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "ASSET_GL_ACCOUNT_ID")
    private String assetGlAccountId;

    @Column(name = "ACC_DEP_GL_ACCOUNT_ID")
    private String accDepGlAccountId;

    @Column(name = "DEP_GL_ACCOUNT_ID")
    private String depGlAccountId;

    @Column(name = "PROFIT_GL_ACCOUNT_ID")
    private String profitGlAccountId;

    @Column(name = "LOSS_GL_ACCOUNT_ID")
    private String lossGlAccountId;
}
