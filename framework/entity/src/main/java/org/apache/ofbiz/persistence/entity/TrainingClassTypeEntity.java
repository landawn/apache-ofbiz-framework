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
@Entity(name = "TRAINING_CLASS_TYPE")
@Table(name = "TRAINING_CLASS_TYPE")
public class TrainingClassTypeEntity {
    @Id
    @Column(name = "TRAINING_CLASS_TYPE_ID")
    private String trainingClassTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "DESCRIPTION")
    private String description;
}
