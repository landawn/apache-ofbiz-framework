package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ACCTG_TRANS_ENTRY")
@Table(name = "ACCTG_TRANS_ENTRY")
public class AcctgTransEntryEntity {
    @Id
    @Column(name = "ACCTG_TRANS_ID")
    private String acctgTransId;

    @Id
    @Column(name = "ACCTG_TRANS_ENTRY_SEQ_ID")
    private String acctgTransEntrySeqId;

    @Column(name = "ACCTG_TRANS_ENTRY_TYPE_ID")
    private String acctgTransEntryTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "VOUCHER_REF")
    private String voucherRef;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "THEIR_PARTY_ID")
    private String theirPartyId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "THEIR_PRODUCT_ID")
    private String theirProductId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "GL_ACCOUNT_TYPE_ID")
    private String glAccountTypeId;

    @Column(name = "GL_ACCOUNT_ID")
    private String glAccountId;

    @Column(name = "ORGANIZATION_PARTY_ID")
    private String organizationPartyId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "ORIG_AMOUNT")
    private BigDecimal origAmount;

    @Column(name = "ORIG_CURRENCY_UOM_ID")
    private String origCurrencyUomId;

    @Column(name = "DEBIT_CREDIT_FLAG")
    private String debitCreditFlag;

    @Column(name = "DUE_DATE")
    private Date dueDate;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "TAX_ID")
    private String taxId;

    @Column(name = "RECONCILE_STATUS_ID")
    private String reconcileStatusId;

    @Column(name = "SETTLEMENT_TERM_ID")
    private String settlementTermId;

    @Column(name = "IS_SUMMARY")
    private String isSummary;
}
