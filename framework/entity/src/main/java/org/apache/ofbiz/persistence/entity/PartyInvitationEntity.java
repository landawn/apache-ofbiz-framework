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
@Entity(name = "PARTY_INVITATION")
@Table(name = "PARTY_INVITATION")
public class PartyInvitationEntity {
    @Id
    @Column(name = "PARTY_INVITATION_ID")
    private String partyInvitationId;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "TO_NAME")
    private String toName;

    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "LAST_INVITE_DATE")
    private Timestamp lastInviteDate;
}
