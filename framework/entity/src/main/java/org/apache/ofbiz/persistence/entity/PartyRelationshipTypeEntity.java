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
@Entity(name = "PARTY_RELATIONSHIP_TYPE")
@Table(name = "PARTY_RELATIONSHIP_TYPE")
public class PartyRelationshipTypeEntity {
    @Id
    @Column(name = "PARTY_RELATIONSHIP_TYPE_ID")
    private String partyRelationshipTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "HAS_TABLE")
    private String hasTable;

    @Column(name = "PARTY_RELATIONSHIP_NAME")
    private String partyRelationshipName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ROLE_TYPE_ID_VALID_FROM")
    private String roleTypeIdValidFrom;

    @Column(name = "ROLE_TYPE_ID_VALID_TO")
    private String roleTypeIdValidTo;
}
