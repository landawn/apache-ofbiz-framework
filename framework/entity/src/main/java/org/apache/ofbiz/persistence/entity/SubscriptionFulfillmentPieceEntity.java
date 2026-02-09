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
@Entity(name = "SUBSCRIPTION_FULFILLMENT_PIECE")
@Table(name = "SUBSCRIPTION_FULFILLMENT_PIECE")
public class SubscriptionFulfillmentPieceEntity {
    @Id
    @Column(name = "SUBSCRIPTION_ACTIVITY_ID")
    private String subscriptionActivityId;

    @Id
    @Column(name = "SUBSCRIPTION_ID")
    private String subscriptionId;
}
