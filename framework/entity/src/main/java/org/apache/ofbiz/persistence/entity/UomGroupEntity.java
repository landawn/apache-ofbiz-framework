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
@Entity(name = "UOM_GROUP")
@Table(name = "UOM_GROUP")
public class UomGroupEntity {
    @Id
    @Column(name = "UOM_GROUP_ID")
    private String uomGroupId;

    @Id
    @Column(name = "UOM_ID")
    private String uomId;
}
