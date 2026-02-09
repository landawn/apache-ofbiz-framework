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
@Entity(name = "QUOTE")
@Table(name = "QUOTE")
public class QuoteEntity {
    @Id
    @Column(name = "QUOTE_ID")
    private String quoteId;

    @Column(name = "QUOTE_TYPE_ID")
    private String quoteTypeId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "ISSUE_DATE")
    private Timestamp issueDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "SALES_CHANNEL_ENUM_ID")
    private String salesChannelEnumId;

    @Column(name = "VALID_FROM_DATE")
    private Timestamp validFromDate;

    @Column(name = "VALID_THRU_DATE")
    private Timestamp validThruDate;

    @Column(name = "QUOTE_NAME")
    private String quoteName;

    @Column(name = "DESCRIPTION")
    private String description;
}
