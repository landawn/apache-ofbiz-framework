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
@Entity(name = "PARTY_CONTENT_TYPE")
@Table(name = "PARTY_CONTENT_TYPE")
public class PartyContentTypeEntity {
    @Id
    @Column(name = "PARTY_CONTENT_TYPE_ID")
    private String partyContentTypeId;

    @Column(name = "PARENT_TYPE_ID")
    private String parentTypeId;

    @Column(name = "DESCRIPTION")
    private String description;
}
