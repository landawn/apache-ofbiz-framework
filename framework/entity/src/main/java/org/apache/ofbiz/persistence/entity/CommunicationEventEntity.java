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
@Entity(name = "COMMUNICATION_EVENT")
@Table(name = "COMMUNICATION_EVENT")
public class CommunicationEventEntity {
    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Column(name = "COMMUNICATION_EVENT_TYPE_ID")
    private String communicationEventTypeId;

    @Column(name = "ORIG_COMM_EVENT_ID")
    private String origCommEventId;

    @Column(name = "PARENT_COMM_EVENT_ID")
    private String parentCommEventId;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CONTACT_MECH_TYPE_ID")
    private String contactMechTypeId;

    @Column(name = "CONTACT_MECH_ID_FROM")
    private String contactMechIdFrom;

    @Column(name = "CONTACT_MECH_ID_TO")
    private String contactMechIdTo;

    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Column(name = "ENTRY_DATE")
    private Timestamp entryDate;

    @Column(name = "DATETIME_STARTED")
    private Timestamp datetimeStarted;

    @Column(name = "DATETIME_ENDED")
    private Timestamp datetimeEnded;

    @Column(name = "SUBJECT")
    private String subject;

    @Column(name = "CONTENT_MIME_TYPE_ID")
    private String contentMimeTypeId;

    @Column(name = "CONTENT")
    private String content;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "REASON_ENUM_ID")
    private String reasonEnumId;

    @Column(name = "CONTACT_LIST_ID")
    private String contactListId;

    @Column(name = "HEADER_STRING")
    private String headerString;

    @Column(name = "FROM_STRING")
    private String fromString;

    @Column(name = "TO_STRING")
    private String toString;

    @Column(name = "CC_STRING")
    private String ccString;

    @Column(name = "BCC_STRING")
    private String bccString;

    @Column(name = "MESSAGE_ID")
    private String messageId;
}
