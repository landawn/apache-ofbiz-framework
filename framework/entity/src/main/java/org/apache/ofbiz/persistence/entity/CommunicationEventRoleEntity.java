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
@Entity(name = "COMMUNICATION_EVENT_ROLE")
@Table(name = "COMMUNICATION_EVENT_ROLE")
public class CommunicationEventRoleEntity {
    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "STATUS_ID")
    private String statusId;
}
