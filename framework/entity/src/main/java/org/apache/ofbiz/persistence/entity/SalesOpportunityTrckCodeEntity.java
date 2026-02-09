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
@Entity(name = "SALES_OPPORTUNITY_TRCK_CODE")
@Table(name = "SALES_OPPORTUNITY_TRCK_CODE")
public class SalesOpportunityTrckCodeEntity {
    @Id
    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;

    @Id
    @Column(name = "TRACKING_CODE_ID")
    private String trackingCodeId;

    @Column(name = "RECEIVED_DATE")
    private Timestamp receivedDate;
}
