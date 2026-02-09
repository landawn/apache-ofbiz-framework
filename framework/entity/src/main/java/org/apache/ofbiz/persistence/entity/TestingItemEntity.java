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
@Entity(name = "TESTING_ITEM")
@Table(name = "TESTING_ITEM")
public class TestingItemEntity {
    @Id
    @Column(name = "TESTING_ID")
    private String testingId;

    @Id
    @Column(name = "TESTING_SEQ_ID")
    private String testingSeqId;

    @Column(name = "TESTING_HISTORY")
    private String testingHistory;
}
