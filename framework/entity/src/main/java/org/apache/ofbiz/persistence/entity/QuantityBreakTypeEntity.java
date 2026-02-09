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
@Entity(name = "QUANTITY_BREAK_TYPE")
@Table(name = "QUANTITY_BREAK_TYPE")
public class QuantityBreakTypeEntity {
    @Id
    @Column(name = "QUANTITY_BREAK_TYPE_ID")
    private String quantityBreakTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
