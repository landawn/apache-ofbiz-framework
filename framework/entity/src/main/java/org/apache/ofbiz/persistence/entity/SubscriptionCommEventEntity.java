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
@Entity(name = "SUBSCRIPTION_COMM_EVENT")
@Table(name = "SUBSCRIPTION_COMM_EVENT")
public class SubscriptionCommEventEntity {
    @Id
    @Column(name = "SUBSCRIPTION_ID")
    private String subscriptionId;

    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;
}
