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
@Entity(name = "TESTING_NODE")
@Table(name = "TESTING_NODE")
public class TestingNodeEntity {
    @Id
    @Column(name = "TESTING_NODE_ID")
    private String testingNodeId;

    @Column(name = "PRIMARY_PARENT_NODE_ID")
    private String primaryParentNodeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
