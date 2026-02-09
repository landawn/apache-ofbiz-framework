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
@Entity(name = "TRACKING_CODE_TYPE")
@Table(name = "TRACKING_CODE_TYPE")
public class TrackingCodeTypeEntity {
    @Id
    @Column(name = "TRACKING_CODE_TYPE_ID")
    private String trackingCodeTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
