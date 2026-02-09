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
@Entity(name = "VENDOR_PRODUCT")
@Table(name = "VENDOR_PRODUCT")
public class VendorProductEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Id
    @Column(name = "VENDOR_PARTY_ID")
    private String vendorPartyId;

    @Id
    @Column(name = "PRODUCT_STORE_GROUP_ID")
    private String productStoreGroupId;
}
