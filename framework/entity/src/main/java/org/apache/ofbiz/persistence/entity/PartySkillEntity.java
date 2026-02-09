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
@Entity(name = "PARTY_SKILL")
@Table(name = "PARTY_SKILL")
public class PartySkillEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "SKILL_TYPE_ID")
    private String skillTypeId;

    @Column(name = "YEARS_EXPERIENCE")
    private BigDecimal yearsExperience;

    @Column(name = "RATING")
    private BigDecimal rating;

    @Column(name = "SKILL_LEVEL")
    private BigDecimal skillLevel;

    @Column(name = "STARTED_USING_DATE")
    private Timestamp startedUsingDate;
}
