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
@Entity(name = "INVOICE")
@Table(name = "INVOICE")
public class InvoiceEntity {
    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Column(name = "INVOICE_TYPE_ID")
    private String invoiceTypeId;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "BILLING_ACCOUNT_ID")
    private String billingAccountId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "INVOICE_DATE")
    private Timestamp invoiceDate;

    @Column(name = "DUE_DATE")
    private Timestamp dueDate;

    @Column(name = "PAID_DATE")
    private Timestamp paidDate;

    @Column(name = "INVOICE_MESSAGE")
    private String invoiceMessage;

    @Column(name = "REFERENCE_NUMBER")
    private String referenceNumber;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;
}
