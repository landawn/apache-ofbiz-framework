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
@Entity(name = "GEO")
@Table(name = "GEO")
public class GeoEntity {
    @Id
    @Column(name = "GEO_ID")
    private String geoId;

    @Column(name = "GEO_TYPE_ID")
    private String geoTypeId;

    @Column(name = "GEO_NAME")
    private String geoName;

    @Column(name = "GEO_CODE")
    private String geoCode;

    @Column(name = "GEO_SEC_CODE")
    private String geoSecCode;

    @Column(name = "ABBREVIATION")
    private String abbreviation;

    @Column(name = "WELL_KNOWN_TEXT")
    private String wellKnownText;
}
