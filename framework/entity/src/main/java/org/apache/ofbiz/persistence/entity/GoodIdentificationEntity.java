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
@Entity(name = "GOOD_IDENTIFICATION")
@Table(name = "GOOD_IDENTIFICATION")
public class GoodIdentificationEntity {
    @Id
    @Column(name = "GOOD_IDENTIFICATION_TYPE_ID")
    private String goodIdentificationTypeId;

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "ID_VALUE")
    private String idValue;
}
