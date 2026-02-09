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
@Entity(name = "COMMUNICATION_EVENT_TYPE")
@Table(name = "COMMUNICATION_EVENT_TYPE")
public class CommunicationEventTypeEntity {
    @Id
    @Column(name = "COMMUNICATION_EVENT_TYPE_ID")
    private String communicationEventTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CONTACT_MECH_TYPE_ID")
    private String contactMechTypeId;
}
