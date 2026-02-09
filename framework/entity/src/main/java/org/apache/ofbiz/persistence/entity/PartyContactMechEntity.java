package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PARTY_CONTACT_MECH")
@Table(name = "PARTY_CONTACT_MECH")
public class PartyContactMechEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "ALLOW_SOLICITATION")
    private String allowSolicitation;

    @Column(name = "EXTENSION")
    private String extension;

    @Column(name = "VERIFIED")
    private String verified;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "YEARS_WITH_CONTACT_MECH")
    private BigDecimal yearsWithContactMech;

    @Column(name = "MONTHS_WITH_CONTACT_MECH")
    private BigDecimal monthsWithContactMech;
}
