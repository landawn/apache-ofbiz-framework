package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ACCOMMODATION_SPOT")
@Table(name = "ACCOMMODATION_SPOT")
public class AccommodationSpotEntity {
    @Id
    @Column(name = "ACCOMMODATION_SPOT_ID")
    private String accommodationSpotId;

    @Column(name = "ACCOMMODATION_CLASS_ID")
    private String accommodationClassId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "NUMBER_OF_SPACES")
    private BigDecimal numberOfSpaces;

    @Column(name = "DESCRIPTION")
    private String description;
}
