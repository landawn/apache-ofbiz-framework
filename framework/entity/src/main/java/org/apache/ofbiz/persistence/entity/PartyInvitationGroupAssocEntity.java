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
@Entity(name = "PARTY_INVITATION_GROUP_ASSOC")
@Table(name = "PARTY_INVITATION_GROUP_ASSOC")
public class PartyInvitationGroupAssocEntity {
    @Id
    @Column(name = "PARTY_INVITATION_ID")
    private String partyInvitationId;

    @Id
    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;
}
