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
@Entity(name = "CONTACT_LIST_PARTY_STATUS")
@Table(name = "CONTACT_LIST_PARTY_STATUS")
public class ContactListPartyStatusEntity {
    @Id
    @Column(name = "CONTACT_LIST_ID")
    private String contactListId;

    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Id
    @Column(name = "STATUS_DATE")
    private Timestamp statusDate;

    @Column(name = "STATUS_ID")
    private String statusId;

    @Column(name = "SET_BY_USER_LOGIN_ID")
    private String setByUserLoginId;

    @Column(name = "OPT_IN_VERIFY_CODE")
    private String optInVerifyCode;
}
