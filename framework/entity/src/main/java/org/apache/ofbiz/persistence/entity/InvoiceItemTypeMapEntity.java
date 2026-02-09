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
@Entity(name = "INVOICE_ITEM_TYPE_MAP")
@Table(name = "INVOICE_ITEM_TYPE_MAP")
public class InvoiceItemTypeMapEntity {
    @Id
    @Column(name = "INVOICE_ITEM_MAP_KEY")
    private String invoiceItemMapKey;

    @Id
    @Column(name = "INVOICE_TYPE_ID")
    private String invoiceTypeId;

    @Column(name = "INVOICE_ITEM_TYPE_ID")
    private String invoiceItemTypeId;
}
