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
@Entity(name = "VARIANCE_REASON")
@Table(name = "VARIANCE_REASON")
public class VarianceReasonEntity {
    @Id
    @Column(name = "VARIANCE_REASON_ID")
    private String varianceReasonId;

    @Column(name = "DESCRIPTION")
    private String description;
}
