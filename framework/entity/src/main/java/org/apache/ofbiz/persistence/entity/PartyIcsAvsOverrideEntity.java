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
@Entity(name = "PARTY_ICS_AVS_OVERRIDE")
@Table(name = "PARTY_ICS_AVS_OVERRIDE")
public class PartyIcsAvsOverrideEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "AVS_DECLINE_STRING")
    private String avsDeclineString;
}
