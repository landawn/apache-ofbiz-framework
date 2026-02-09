package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PARTY_GROUP")
@Table(name = "PARTY_GROUP")
public class PartyGroupEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "GROUP_NAME")
    private String groupName;

    @Column(name = "GROUP_NAME_LOCAL")
    private String groupNameLocal;

    @Column(name = "OFFICE_SITE_NAME")
    private String officeSiteName;

    @Column(name = "ANNUAL_REVENUE")
    private BigDecimal annualRevenue;

    @Column(name = "NUM_EMPLOYEES")
    private BigDecimal numEmployees;

    @Column(name = "TICKER_SYMBOL")
    private String tickerSymbol;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "LOGO_IMAGE_URL")
    private String logoImageUrl;
}
