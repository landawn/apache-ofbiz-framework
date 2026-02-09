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
@Entity(name = "PARTY_GL_ACCOUNT")
@Table(name = "PARTY_GL_ACCOUNT")
public class PartyGlAccountEntity {
    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Id
    @Column(name = "GL_ACCOUNT_TYPE_ID")
    private String glAccountTypeId;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;
}
