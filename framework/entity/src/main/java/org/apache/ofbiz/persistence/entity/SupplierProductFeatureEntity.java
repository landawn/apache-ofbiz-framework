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
@Entity(name = "SUPPLIER_PRODUCT_FEATURE")
@Table(name = "SUPPLIER_PRODUCT_FEATURE")
public class SupplierProductFeatureEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "PRODUCT_FEATURE_ID")
    private String productFeatureId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "UOM_ID")
    private String uomId;

    @Column(name = "ID_CODE")
    private String idCode;
}
