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
@Entity(name = "ORDER_NOTIFICATION")
@Table(name = "ORDER_NOTIFICATION")
public class OrderNotificationEntity {
    @Id
    @Column(name = "ORDER_NOTIFICATION_ID")
    private String orderNotificationId;

    @Column(name = "ORDER_ID")
    private String orderId;

    @Column(name = "EMAIL_TYPE")
    private String emailType;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "NOTIFICATION_DATE")
    private Timestamp notificationDate;
}
