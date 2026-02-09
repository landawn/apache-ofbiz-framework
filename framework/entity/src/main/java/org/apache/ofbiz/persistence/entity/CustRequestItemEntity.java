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
@Entity(name = "CUST_REQUEST_ITEM")
@Table(name = "CUST_REQUEST_ITEM")
public class CustRequestItemEntity {
    @Id
    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Id
    @Column(name = "CUST_REQUEST_ITEM_SEQ_ID")
    private String custRequestItemSeqId;

    @Column(name = "CUST_REQUEST_RESOLUTION_ID")
    private String custRequestResolutionId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "PRIORITY")
    private BigDecimal priority;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "REQUIRED_BY_DATE")
    private Timestamp requiredByDate;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "SELECTED_AMOUNT")
    private BigDecimal selectedAmount;

    @Column(name = "MAXIMUM_AMOUNT")
    private BigDecimal maximumAmount;

    @Column(name = "RESERV_START")
    private Timestamp reservStart;

    @Column(name = "RESERV_LENGTH")
    private BigDecimal reservLength;

    @Column(name = "RESERV_PERSONS")
    private BigDecimal reservPersons;

    @Column(name = "CONFIG_ID")
    private String configId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STORY")
    private String story;
}
