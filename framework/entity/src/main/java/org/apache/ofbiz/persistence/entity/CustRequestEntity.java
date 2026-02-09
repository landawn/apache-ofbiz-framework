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
@Entity(name = "CUST_REQUEST")
@Table(name = "CUST_REQUEST")
public class CustRequestEntity {
    @Id
    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Column(name = "CUST_REQUEST_TYPE_ID")
    private String custRequestTypeId;

    @Column(name = "CUST_REQUEST_CATEGORY_ID")
    private String custRequestCategoryId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "FROM_PARTY_ID")
    private String fromPartyId;

    @Column(name = "PRIORITY")
    private BigDecimal priority;

    @Column(name = "CUST_REQUEST_DATE")
    private Timestamp custRequestDate;

    @Column(name = "RESPONSE_REQUIRED_DATE")
    private Timestamp responseRequiredDate;

    @Column(name = "CUST_REQUEST_NAME")
    private String custRequestName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "MAXIMUM_AMOUNT_UOM_ID")
    private String maximumAmountUomId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "SALES_CHANNEL_ENUM_ID")
    private String salesChannelEnumId;

    @Column(name = "FULFILL_CONTACT_MECH_ID")
    private String fulfillContactMechId;

    @Column(name = "CURRENCY_UOM_ID")
    private String currencyUomId;

    @Column(name = "OPEN_DATE_TIME")
    private Timestamp openDateTime;

    @Column(name = "CLOSED_DATE_TIME")
    private Timestamp closedDateTime;

    @Column(name = "INTERNAL_COMMENT")
    private String internalComment;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
