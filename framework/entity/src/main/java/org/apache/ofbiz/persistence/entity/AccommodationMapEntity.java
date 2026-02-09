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
@Entity(name = "ACCOMMODATION_MAP")
@Table(name = "ACCOMMODATION_MAP")
public class AccommodationMapEntity {
    @Id
    @Column(name = "ACCOMMODATION_MAP_ID")
    private String accommodationMapId;

    @Column(name = "ACCOMMODATION_CLASS_ID")
    private String accommodationClassId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "ACCOMMODATION_MAP_TYPE_ID")
    private String accommodationMapTypeId;

    @Column(name = "NUMBER_OF_SPACES")
    private BigDecimal numberOfSpaces;
}
