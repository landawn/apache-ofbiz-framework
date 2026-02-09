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
@Entity(name = "WEB_ANALYTICS_CONFIG")
@Table(name = "WEB_ANALYTICS_CONFIG")
public class WebAnalyticsConfigEntity {
    @Id
    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Id
    @Column(name = "WEB_ANALYTICS_TYPE_ID")
    private String webAnalyticsTypeId;

    @Column(name = "WEB_ANALYTICS_CODE")
    private String webAnalyticsCode;
}
