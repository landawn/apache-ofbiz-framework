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
@Entity(name = "PARTY_NEED")
@Table(name = "PARTY_NEED")
public class PartyNeedEntity {
    @Id
    @Column(name = "PARTY_NEED_ID")
    private String partyNeedId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "ROLE_TYPE_ID")
    private String roleTypeId;

    @Column(name = "PARTY_TYPE_ID")
    private String partyTypeId;

    @Column(name = "NEED_TYPE_ID")
    private String needTypeId;

    @Column(name = "COMMUNICATION_EVENT_ID")
    private String communicationEventId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_CATEGORY_ID")
    private String productCategoryId;

    @Column(name = "VISIT_ID")
    private String visitId;

    @Column(name = "DATETIME_RECORDED")
    private Timestamp datetimeRecorded;

    @Column(name = "DESCRIPTION")
    private String description;
}
