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
@Entity(name = "FIN_ACCOUNT_TYPE")
@Table(name = "FIN_ACCOUNT_TYPE")
public class FinAccountTypeEntity {
    @Id
    @Column(name = "FIN_ACCOUNT_TYPE_ID")
    private String finAccountTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "REPLENISH_ENUM_ID")
    private String replenishEnumId;

    @Column(name = "IS_REFUNDABLE")
    private String isRefundable;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "DESCRIPTION")
    private String description;
}
