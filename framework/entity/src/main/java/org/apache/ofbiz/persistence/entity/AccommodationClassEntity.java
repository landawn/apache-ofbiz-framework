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
@Entity(name = "ACCOMMODATION_CLASS")
@Table(name = "ACCOMMODATION_CLASS")
public class AccommodationClassEntity {
    @Id
    @Column(name = "ACCOMMODATION_CLASS_ID")
    private String accommodationClassId;

    @Column(name = "PARENT_CLASS_ID")
    private String parentClassId;

    @Column(name = "DESCRIPTION")
    private String description;
}
