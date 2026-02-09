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
@Entity(name = "ORDER_BLACKLIST_TYPE")
@Table(name = "ORDER_BLACKLIST_TYPE")
public class OrderBlacklistTypeEntity {
    @Id
    @Column(name = "ORDER_BLACKLIST_TYPE_ID")
    private String orderBlacklistTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
