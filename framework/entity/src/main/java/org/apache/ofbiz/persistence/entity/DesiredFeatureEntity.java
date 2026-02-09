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
@Entity(name = "DESIRED_FEATURE")
@Table(name = "DESIRED_FEATURE")
public class DesiredFeatureEntity {
    @Id
    @Column(name = "DESIRED_FEATURE_ID")
    private String desiredFeatureId;

    @Id
    @Column(name = "REQUIREMENT_ID")
    private String requirementId;

    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "OPTIONAL_IND")
    private String optionalInd;
}
