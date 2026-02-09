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
@Entity(name = "ORDER_STATUS")
@Table(name = "ORDER_STATUS")
public class OrderStatusEntity {
    @Id
    @Column(name = "ORDER_STATUS_ID")
    private String orderStatusId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;

    @Column(name = "ORDER_PAYMENT_PREFERENCE_ID")
    private String orderPaymentPreferenceId;

    @Column(name = "STATUS_DATETIME")
    private Timestamp statusDatetime;

    @Column(name = "STATUS_USER_LOGIN")
    private String statusUserLogin;

    @Column(name = "CHANGE_REASON")
    private String changeReason;
}
