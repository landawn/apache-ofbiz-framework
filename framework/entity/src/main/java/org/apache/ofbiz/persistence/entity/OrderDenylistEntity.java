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
@Entity(name = "ORDER_DENYLIST")
@Table(name = "ORDER_DENYLIST")
public class OrderDenylistEntity {
    @Id
    @Column(name = "DENYLIST_STRING")
    private String denylistString;

    @Id
    @Column(name = "ORDER_DENYLIST_TYPE_ID")
    private String orderDenylistTypeId;
}
