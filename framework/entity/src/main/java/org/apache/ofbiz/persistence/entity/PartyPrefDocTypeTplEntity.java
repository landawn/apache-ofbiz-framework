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
@Entity(name = "PARTY_PREF_DOC_TYPE_TPL")
@Table(name = "PARTY_PREF_DOC_TYPE_TPL")
public class PartyPrefDocTypeTplEntity {
    @Id
    @Column(name = "PARTY_PREF_DOC_TYPE_TPL_ID")
    private String partyPrefDocTypeTplId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "INVOICE_TYPE_ID")
    private String invoiceTypeId;

    @Column(name = "ORDER_TYPE_ID")
    private String orderTypeId;

    @Column(name = "QUOTE_TYPE_ID")
    private String quoteTypeId;

    @Column(name = "CUSTOM_SCREEN_ID")
    private String customScreenId;
}
