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
@Entity(name = "PARTY_ACCTG_PREFERENCE")
@Table(name = "PARTY_ACCTG_PREFERENCE")
public class PartyAcctgPreferenceEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "FISCAL_YEAR_START_MONTH")
    private BigDecimal fiscalYearStartMonth;

    @Column(name = "FISCAL_YEAR_START_DAY")
    private BigDecimal fiscalYearStartDay;

    @Column(name = "TAX_FORM_ID")
    private String taxFormId;

    @Column(name = "COGS_METHOD_ID")
    private String cogsMethodId;

    @Column(name = "BASE_CURRENCY_UOM_ID")
    private String baseCurrencyUomId;

    @Column(name = "INVOICE_SEQ_CUST_METH_ID")
    private String invoiceSeqCustMethId;

    @Column(name = "INVOICE_ID_PREFIX")
    private String invoiceIdPrefix;

    @Column(name = "LAST_INVOICE_NUMBER")
    private BigDecimal lastInvoiceNumber;

    @Column(name = "LAST_INVOICE_RESTART_DATE")
    private Timestamp lastInvoiceRestartDate;

    @Column(name = "USE_INVOICE_ID_FOR_RETURNS")
    private String useInvoiceIdForReturns;

    @Column(name = "QUOTE_SEQ_CUST_METH_ID")
    private String quoteSeqCustMethId;

    @Column(name = "QUOTE_ID_PREFIX")
    private String quoteIdPrefix;

    @Column(name = "LAST_QUOTE_NUMBER")
    private BigDecimal lastQuoteNumber;

    @Column(name = "ORDER_SEQ_CUST_METH_ID")
    private String orderSeqCustMethId;

    @Column(name = "ORDER_ID_PREFIX")
    private String orderIdPrefix;

    @Column(name = "LAST_ORDER_NUMBER")
    private BigDecimal lastOrderNumber;

    @Column(name = "REFUND_PAYMENT_METHOD_ID")
    private String refundPaymentMethodId;

    @Column(name = "ERROR_GL_JOURNAL_ID")
    private String errorGlJournalId;

    @Column(name = "ENABLE_ACCOUNTING")
    private String enableAccounting;
}
