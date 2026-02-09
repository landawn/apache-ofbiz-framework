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
@Entity(name = "PARTY_IDENTIFICATION")
@Table(name = "PARTY_IDENTIFICATION")
public class PartyIdentificationEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "PARTY_IDENTIFICATION_TYPE_ID")
    private String partyIdentificationTypeId;

    @Column(name = "ID_VALUE")
    private String idValue;
}
