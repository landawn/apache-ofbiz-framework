package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "COMM_EVENT_CONTENT_ASSOC")
@Table(name = "COMM_EVENT_CONTENT_ASSOC")
public class CommEventContentAssocEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Id
    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Column(name = "COMM_CONTENT_ASSOC_TYPE_ID")
    private String commContentAssocTypeId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
