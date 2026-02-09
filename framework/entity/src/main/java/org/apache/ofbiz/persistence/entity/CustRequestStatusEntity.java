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
@Entity(name = "CUST_REQUEST_STATUS")
@Table(name = "CUST_REQUEST_STATUS")
public class CustRequestStatusEntity {
    @Id
    @Column(name = "CUST_REQUEST_STATUS_ID")
    private String custRequestStatusId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CUST_REQUEST_ID")
    private String custRequestId;

    @Column(name = "CUST_REQUEST_ITEM_SEQ_ID")
    private String custRequestItemSeqId;

    @Column(name = "STATUS_DATE")
    private Timestamp statusDate;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;
}
