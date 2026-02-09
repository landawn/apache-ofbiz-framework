package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ACCTG_TRANS")
@Table(name = "ACCTG_TRANS")
public class AcctgTransEntity {
    @Id
    @Column(name = "ACCTG_TRANS_ID")
    private String acctgTransId;

    @Column(name = "ACCTG_TRANS_TYPE_ID")
    private String acctgTransTypeId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TRANSACTION_DATE")
    private Timestamp transactionDate;

    @Column(name = "IS_POSTED")
    private String isPosted;

    @Column(name = "POSTED_DATE")
    private Timestamp postedDate;

    @Column(name = "SCHEDULED_POSTING_DATE")
    private Timestamp scheduledPostingDate;

    @Column(name = "GL_JOURNAL_ID")
    private String glJournalId;

    @Column(name = "GL_FISCAL_TYPE_ID")
    private String glFiscalTypeId;

    @Column(name = "VOUCHER_REF")
    private String voucherRef;

    @Column(name = "VOUCHER_DATE")
    private Timestamp voucherDate;

    @Column(name = "GROUP_STATUS_ID")
    private String groupStatusId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "INVENTORY_ITEM_ID")
    private String inventoryItemId;

    @Column(name = "PHYSICAL_INVENTORY_ID")
    private String physicalInventoryId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "FIN_ACCOUNT_TRANS_ID")
    private String finAccountTransId;

    @Column(name = "SHIPMENT_ID")
    private String shipmentId;

    @Column(name = "RECEIPT_ID")
    private String receiptId;

    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "THEIR_ACCTG_TRANS_ID")
    private String theirAcctgTransId;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
