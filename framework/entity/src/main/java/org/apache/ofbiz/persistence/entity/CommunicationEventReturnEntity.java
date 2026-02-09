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
@Entity(name = "COMMUNICATION_EVENT_RETURN")
@Table(name = "COMMUNICATION_EVENT_RETURN")
public class CommunicationEventReturnEntity {
    @Id
    @Column(name = "RETURN_ID")
    private String returnId;

    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;
}
