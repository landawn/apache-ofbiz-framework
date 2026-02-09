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
@Entity(name = "INVOICE_ITEM_TYPE_GL_ACCOUNT")
@Table(name = "INVOICE_ITEM_TYPE_GL_ACCOUNT")
public class InvoiceItemTypeGlAccountEntity {
    @Id
    @Column(name = "INVOICE_ITEM_TYPE_ID")
    private String invoiceItemTypeId;

    @Id
    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;
}
