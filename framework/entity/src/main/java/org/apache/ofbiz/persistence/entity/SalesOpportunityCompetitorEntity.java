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
@Entity(name = "SALES_OPPORTUNITY_COMPETITOR")
@Table(name = "SALES_OPPORTUNITY_COMPETITOR")
public class SalesOpportunityCompetitorEntity {
    @Id
    @Column(name = "SALES_OPPORTUNITY_ID")
    private String salesOpportunityId;

    @Id
    @Column(name = "COMPETITOR_PARTY_ID")
    private String competitorPartyId;

    @Column(name = "POSITION_ENUM_ID")
    private String positionEnumId;

    @Column(name = "STRENGTHS")
    private String strengths;

    @Column(name = "WEAKNESSES")
    private String weaknesses;
}
