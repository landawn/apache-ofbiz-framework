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
@Entity(name = "PARTY_TYPE_ATTR")
@Table(name = "PARTY_TYPE_ATTR")
public class PartyTypeAttrEntity {
    @Id
    @Column(name = "PARTY_TYPE_ID")
    private String partyTypeId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "DESCRIPTION")
    private String description;
}
