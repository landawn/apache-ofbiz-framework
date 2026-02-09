package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "INVOICE_ITEM")
@Table(name = "INVOICE_ITEM")
public class InvoiceItemEntity {
    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "INVOICE_ITEM_SEQ_ID")
    private String invoiceItemSeqId;

    @Column(name = "INVOICE_ITEM_TYPE_ID")
    private String invoiceItemTypeId;

    @Column(name = "OVERRIDE_GL_ACCOUNT_ID")
    private String overrideGlAccountId;

    @Column(name = "OVERRIDE_ORG_PARTY_ID")
    private String overrideOrgPartyId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "PARENT_INVOICE_ID")
    private String parentInvoiceId;

    @Column(name = "PARENT_INVOICE_ITEM_SEQ_ID")
    private String parentInvoiceItemSeqId;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "TAXABLE_FLAG")
    private String taxableFlag;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TAX_AUTH_PARTY_ID")
    private String taxAuthPartyId;

    @Column(name = "TAX_AUTH_GEO_ID")
    private String taxAuthGeoId;

    @Column(name = "TAX_AUTHORITY_RATE_SEQ_ID")
    private String taxAuthorityRateSeqId;

    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;
}
