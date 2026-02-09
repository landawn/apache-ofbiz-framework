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
@Entity(name = "SUBSCRIPTION_RESOURCE")
@Table(name = "SUBSCRIPTION_RESOURCE")
public class SubscriptionResourceEntity {
    @Id
    @Column(name = "SUBSCRIPTION_RESOURCE_ID")
    private String subscriptionResourceId;

    @Column(name = "PARENT_RESOURCE_ID")
    private String parentResourceId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Column(name = "SERVICE_NAME_ON_EXPIRY")
    private String serviceNameOnExpiry;
}
