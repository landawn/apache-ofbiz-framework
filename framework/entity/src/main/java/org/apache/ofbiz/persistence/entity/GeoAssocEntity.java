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
@Entity(name = "GEO_ASSOC")
@Table(name = "GEO_ASSOC")
public class GeoAssocEntity {
    @Id
    @Column(name = "GEO_ID")
    private String geoId;

    @Id
    @Column(name = "GEO_ID_TO")
    private String geoIdTo;

    @Column(name = "GEO_ASSOC_TYPE_ID")
    private String geoAssocTypeId;
}
