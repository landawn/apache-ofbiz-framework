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
@Entity(name = "TESTING_REMOVE_ALL")
@Table(name = "TESTING_REMOVE_ALL")
public class TestingRemoveAllEntity {
    @Id
    @Column(name = "TESTING_REMOVE_ALL_ID")
    private String testingRemoveAllId;

    @Column(name = "DESCRIPTION")
    private String description;
}
