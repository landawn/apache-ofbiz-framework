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
@Entity(name = "PARTY_PROFILE_DEFAULT")
@Table(name = "PARTY_PROFILE_DEFAULT")
public class PartyProfileDefaultEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "DEFAULT_SHIP_ADDR")
    private String defaultShipAddr;

    @Column(name = "DEFAULT_BILL_ADDR")
    private String defaultBillAddr;

    @Column(name = "DEFAULT_PAY_METH")
    private String defaultPayMeth;

    @Column(name = "DEFAULT_SHIP_METH")
    private String defaultShipMeth;
}
