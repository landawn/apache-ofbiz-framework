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
@Entity(name = "PROD_PROMO_CODE_CONTACT_MECH")
@Table(name = "PROD_PROMO_CODE_CONTACT_MECH")
public class ProdPromoCodeContactMechEntity {
    @Id
    @Column(name = "PRODUCT_PROMO_CODE_ID")
    private String productPromoCodeId;

    @Id
    @Column(name = "CONTACT_MECH_ID")
    private String contactMechId;
}
