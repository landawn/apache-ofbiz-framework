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
@Entity(name = "SUPPLIER_RATING_TYPE")
@Table(name = "SUPPLIER_RATING_TYPE")
public class SupplierRatingTypeEntity {
    @Id
    @Column(name = "SUPPLIER_RATING_TYPE_ID")
    private String supplierRatingTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
