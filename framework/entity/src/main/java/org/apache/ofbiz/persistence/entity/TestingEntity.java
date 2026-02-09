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
@Entity(name = "TESTING")
@Table(name = "TESTING")
public class TestingEntity {
    @Id
    @Column(name = "TESTING_ID")
    private String testingId;

    @Column(name = "TESTING_TYPE_ID")
    private String testingTypeId;

    @Column(name = "TESTING_NAME")
    private String testingName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "TESTING_SIZE")
    private BigDecimal testingSize;

    @Column(name = "TESTING_DATE")
    private Timestamp testingDate;
}
