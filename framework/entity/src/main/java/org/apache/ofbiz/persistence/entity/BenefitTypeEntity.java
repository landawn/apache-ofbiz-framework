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
@Entity(name = "BENEFIT_TYPE")
@Table(name = "BENEFIT_TYPE")
public class BenefitTypeEntity {
    @Id
    @Column(name = "BENEFIT_TYPE_ID")
    private String benefitTypeId;

    @Column(name = "BENEFIT_NAME")
    private String benefitName;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "EMPLOYER_PAID_PERCENTAGE")
    private Double employerPaidPercentage;
}
