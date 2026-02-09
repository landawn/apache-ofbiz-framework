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
@Entity(name = "VARIANCE_REASON_GL_ACCOUNT")
@Table(name = "VARIANCE_REASON_GL_ACCOUNT")
public class VarianceReasonGlAccountEntity {
    @Id
    @Column(name = "VARIANCE_REASON_ID")
    private String varianceReasonId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;
}
