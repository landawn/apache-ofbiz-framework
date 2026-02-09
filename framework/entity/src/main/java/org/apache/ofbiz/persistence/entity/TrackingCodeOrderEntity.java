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
@Entity(name = "TRACKING_CODE_ORDER")
@Table(name = "TRACKING_CODE_ORDER")
public class TrackingCodeOrderEntity {
    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "TRACKING_CODE_TYPE_ID")
    private String trackingCodeTypeId;

    @Column(name = "TRACKING_CODE_ID")
    private String trackingCodeId;

    @Column(name = "IS_BILLABLE")
    private String isBillable;

    @Column(name = "SITE_ID")
    private String siteId;

    @Column(name = "HAS_EXPORTED")
    private String hasExported;

    @Column(name = "AFFILIATE_REFERRED_TIME_STAMP")
    private Timestamp affiliateReferredTimeStamp;
}
