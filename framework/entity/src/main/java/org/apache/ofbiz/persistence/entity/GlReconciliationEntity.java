package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "GL_RECONCILIATION")
@Table(name = "GL_RECONCILIATION")
public class GlReconciliationEntity {
    @Id
    @Column(name = "GL_RECONCILIATION_ID")
    private String glReconciliationId;

    @Column(name = "GL_RECONCILIATION_NAME")
    private String glReconciliationName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "RECONCILED_BALANCE")
    private BigDecimal reconciledBalance;

    @Column(name = "OPENING_BALANCE")
    private BigDecimal openingBalance;

    @Column(name = "RECONCILED_DATE")
    private Timestamp reconciledDate;
}
