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
@Entity(name = "PHYSICAL_INVENTORY")
@Table(name = "PHYSICAL_INVENTORY")
public class PhysicalInventoryEntity {
    @Id
    @Column(name = "PHYSICAL_INVENTORY_ID")
    private String physicalInventoryId;

    @Column(name = "PHYSICAL_INVENTORY_DATE")
    private Timestamp physicalInventoryDate;

    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "GENERAL_COMMENTS")
    private String generalComments;
}
