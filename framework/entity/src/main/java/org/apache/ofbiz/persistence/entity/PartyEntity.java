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
@Entity(name = "PARTY")
@Table(name = "PARTY")
public class PartyEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "PARTY_TYPE_ID")
    private String partyTypeId;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "PREFERRED_CURRENCY_UOM_ID")
    private String preferredCurrencyUomId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;

    @Column(name = "DATA_SOURCE_ID")
    private String dataSourceId;

    @Column(name = "IS_UNREAD")
    private String isUnread;
}
