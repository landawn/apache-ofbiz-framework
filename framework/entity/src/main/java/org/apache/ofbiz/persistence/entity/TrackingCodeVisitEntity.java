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
@Entity(name = "TRACKING_CODE_VISIT")
@Table(name = "TRACKING_CODE_VISIT")
public class TrackingCodeVisitEntity {
    @Id
    @Column(name = "TRACKING_CODE_ID")
    private String trackingCodeId;

    @Id
    @Column(name = "VISIT_ID")
    private String visitId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "SOURCE_ENUM_ID")
    private String sourceEnumId;
}
