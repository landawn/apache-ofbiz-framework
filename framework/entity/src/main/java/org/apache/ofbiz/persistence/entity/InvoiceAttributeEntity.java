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
@Entity(name = "INVOICE_ATTRIBUTE")
@Table(name = "INVOICE_ATTRIBUTE")
public class InvoiceAttributeEntity {
    @Id
    @Column(name = "INVOICE_ID")
    private String invoiceId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;

    @Column(name = "ATTR_DESCRIPTION")
    private String attrDescription;
}
