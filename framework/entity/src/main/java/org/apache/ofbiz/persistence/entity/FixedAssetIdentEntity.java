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
@Entity(name = "FIXED_ASSET_IDENT")
@Table(name = "FIXED_ASSET_IDENT")
public class FixedAssetIdentEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "FIXED_ASSET_IDENT_TYPE_ID")
    private String fixedAssetIdentTypeId;

    @Column(name = "ID_VALUE")
    private String idValue;
}
