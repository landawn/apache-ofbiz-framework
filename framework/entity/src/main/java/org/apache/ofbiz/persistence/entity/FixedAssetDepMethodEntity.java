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
@Entity(name = "FIXED_ASSET_DEP_METHOD")
@Table(name = "FIXED_ASSET_DEP_METHOD")
public class FixedAssetDepMethodEntity {
    @Id
    @Column(name = "DEPRECIATION_CUSTOM_METHOD_ID")
    private String depreciationCustomMethodId;

    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
