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
@Entity(name = "SALE_TYPE")
@Table(name = "SALE_TYPE")
public class SaleTypeEntity {
    @Id
    @Column(name = "SALE_TYPE_ID")
    private String saleTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
