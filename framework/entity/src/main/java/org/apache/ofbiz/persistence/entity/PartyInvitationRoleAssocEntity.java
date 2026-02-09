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
@Entity(name = "PARTY_INVITATION_ROLE_ASSOC")
@Table(name = "PARTY_INVITATION_ROLE_ASSOC")
public class PartyInvitationRoleAssocEntity {
    @Id
    @Column(name = "PARTY_INVITATION_ID")
    private String partyInvitationId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;
}
