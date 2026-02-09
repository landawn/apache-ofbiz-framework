package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PARTY_RELATIONSHIP")
@Table(name = "PARTY_RELATIONSHIP")
public class PartyRelationshipEntity {
    @Id
    @Column(name = "PARTY_ID_FROM")
    private String partyIdFrom;

    @Id
    @Column(name = "PARTY_ID_TO")
    private String partyIdTo;

    @Id
    @Column(name = "ROLE_TYPE_ID_FROM")
    private String roleTypeIdFrom;

    @Id
    @Column(name = "ROLE_TYPE_ID_TO")
    private String roleTypeIdTo;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "RELATIONSHIP_NAME")
    private String relationshipName;

    @Column(name = "SECURITY_GROUP_ID")
    private String securityGroupId;

    @Column(name = "PRIORITY_TYPE_ID")
    private String priorityTypeId;

    @Column(name = "PARTY_RELATIONSHIP_TYPE_ID")
    private String partyRelationshipTypeId;

    @Column(name = "PERMISSIONS_ENUM_ID")
    private String permissionsEnumId;

    @Column(name = "POSITION_TITLE")
    private String positionTitle;

    @Column(name = "COMMENTS")
    private String comments;
}
