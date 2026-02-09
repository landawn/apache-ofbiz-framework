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
@Entity(name = "AGREEMENT_ITEM_TYPE_ATTR")
@Table(name = "AGREEMENT_ITEM_TYPE_ATTR")
public class AgreementItemTypeAttrEntity {
    @Id
    @Column(name = "AGREEMENT_ITEM_TYPE_ID")
    private String agreementItemTypeId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "DESCRIPTION")
    private String description;
}
