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
@Entity(name = "DELIVERABLE")
@Table(name = "DELIVERABLE")
public class DeliverableEntity {
    @Id
    @Column(name = "DELIVERABLE_ID")
    private String deliverableId;

    @Column(name = "DELIVERABLE_TYPE_ID")
    private String deliverableTypeId;

    @Column(name = "DELIVERABLE_NAME")
    private String deliverableName;

    @Column(name = "DESCRIPTION")
    private String description;
}
