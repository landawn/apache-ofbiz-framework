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
@Entity(name = "FIN_ACCOUNT_TRANS")
@Table(name = "FIN_ACCOUNT_TRANS")
public class FinAccountTransEntity {
    @Id
    @Column(name = "FIN_ACCOUNT_TRANS_ID")
    private String finAccountTransId;

    @Column(name = "FIN_ACCOUNT_TRANS_TYPE_ID")
    private String finAccountTransTypeId;

    @Column(name = "FIN_ACCOUNT_ID")
    private String finAccountId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "GL_RECONCILIATION_ID")
    private String glReconciliationId;

    @Column(name = "TRANSACTION_DATE")
    private Timestamp transactionDate;

    @Column(name = "ENTRY_DATE")
    private Timestamp entryDate;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PAYMENT_ID")
    private String paymentId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "PERFORMED_BY_PARTY_ID")
    private String performedByPartyId;

    @Column(name = "REASON_ENUM_ID")
    private String reasonEnumId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "STATUS_ID")
    private String statusId;
}
