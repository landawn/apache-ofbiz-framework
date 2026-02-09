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
@Entity(name = "AFFILIATE")
@Table(name = "AFFILIATE")
public class AffiliateEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "AFFILIATE_NAME")
    private String affiliateName;

    @Column(name = "AFFILIATE_DESCRIPTION")
    private String affiliateDescription;

    @Column(name = "YEAR_ESTABLISHED")
    private String yearEstablished;

    @Column(name = "SITE_TYPE")
    private String siteType;

    @Column(name = "SITE_PAGE_VIEWS")
    private String sitePageViews;

    @Column(name = "SITE_VISITORS")
    private String siteVisitors;

    @Column(name = "DATE_TIME_CREATED")
    private Timestamp dateTimeCreated;

    @Column(name = "DATE_TIME_APPROVED")
    private Timestamp dateTimeApproved;
}
