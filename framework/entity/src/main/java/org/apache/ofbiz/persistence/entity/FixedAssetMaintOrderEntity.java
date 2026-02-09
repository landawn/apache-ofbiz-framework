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
@Entity(name = "FIXED_ASSET_MAINT_ORDER")
@Table(name = "FIXED_ASSET_MAINT_ORDER")
public class FixedAssetMaintOrderEntity {
    @Id
    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Id
    @Column(name = "MAINT_HIST_SEQ_ID")
    private String maintHistSeqId;

    @Id
    @Column(name = "ORDER_ID")
    private String orderId;

    @Id
    @Column(name = "ORDER_ITEM_SEQ_ID")
    private String orderItemSeqId;
}
