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
@Entity(name = "SUBSCRIPTION_ACTIVITY")
@Table(name = "SUBSCRIPTION_ACTIVITY")
public class SubscriptionActivityEntity {
    @Id
    @Column(name = "SUBSCRIPTION_ACTIVITY_ID")
    private String subscriptionActivityId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "DATE_SENT")
    private Timestamp dateSent;
}
