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
@Entity(name = "CONTACT_LIST_COMM_STATUS")
@Table(name = "CONTACT_LIST_COMM_STATUS")
public class ContactListCommStatusEntity {
    @Id
    @Column(name = "CONTACT_LIST_ID")
    private String contactListId;

    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "MESSAGE_ID")
    private String messageId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CHANGE_BY_USER_LOGIN_ID")
    private String changeByUserLoginId;
}
