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
@Entity(name = "INVOICE_CONTACT_MECH")
@Table(name = "INVOICE_CONTACT_MECH")
public class InvoiceContactMechEntity {
    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "CONTACT_MECH_PURPOSE_TYPE_ID")
    private String contactMechPurposeTypeId;

    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;
}
