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
@Entity(name = "TESTING_TYPE")
@Table(name = "TESTING_TYPE")
public class TestingTypeEntity {
    @Id
    @Column(name = "TESTING_TYPE_ID")
    private String testingTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
