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
@Entity(name = "PARTY_DATA_SOURCE")
@Table(name = "PARTY_DATA_SOURCE")
public class PartyDataSourceEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "VISIT_ID")
    private String visitId;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "IS_CREATE")
    private String isCreate;
}
