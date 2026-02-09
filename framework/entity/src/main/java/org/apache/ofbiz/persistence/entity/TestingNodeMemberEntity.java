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
@Entity(name = "TESTING_NODE_MEMBER")
@Table(name = "TESTING_NODE_MEMBER")
public class TestingNodeMemberEntity {
    @Id
    @Column(name = "TESTING_NODE_ID")
    private String testingNodeId;

    @Id
    @Column(name = "TESTING_ID")
    private String testingId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "EXTEND_FROM_DATE")
    private Timestamp extendFromDate;

    @Column(name = "EXTEND_THRU_DATE")
    private Timestamp extendThruDate;
}
