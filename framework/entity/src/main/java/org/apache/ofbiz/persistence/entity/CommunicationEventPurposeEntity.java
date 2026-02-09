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
@Entity(name = "COMMUNICATION_EVENT_PURPOSE")
@Table(name = "COMMUNICATION_EVENT_PURPOSE")
public class CommunicationEventPurposeEntity {
    @Id
    @Column(name = "COMMUNICATION_EVENT_PRP_TYP_ID")
    private String communicationEventPrpTypId;

    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Column(name = "DESCRIPTION")
    private String description;
}
