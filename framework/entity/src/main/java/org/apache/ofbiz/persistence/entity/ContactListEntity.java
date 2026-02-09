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
@Entity(name = "CONTACT_LIST")
@Table(name = "CONTACT_LIST")
public class ContactListEntity {
    @Id
    @Column(name = "CONTACT_LIST_ID")
    private String contactListId;

    @Column(name = "CONTACT_LIST_TYPE_ID")
    private String contactListTypeId;

    @Column(name = "CONTACT_MECH_TYPE_ID")
    private String contactMechTypeId;

    @Column(name = "MARKETING_CAMPAIGN_ID")
    private String marketingCampaignId;

    @Column(name = "CONTACT_LIST_NAME")
    private String contactListName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "IS_PUBLIC")
    private String isPublic;

    @Column(name = "SINGLE_USE")
    private String singleUse;

    @Column(name = "OWNER_PARTY_ID")
    private String ownerPartyId;

    @Column(name = "VERIFY_EMAIL_FROM")
    private String verifyEmailFrom;

    @Column(name = "VERIFY_EMAIL_SCREEN")
    private String verifyEmailScreen;

    @Column(name = "VERIFY_EMAIL_SUBJECT")
    private String verifyEmailSubject;

    @Column(name = "VERIFY_EMAIL_WEB_SITE_ID")
    private String verifyEmailWebSiteId;

    @Column(name = "OPT_OUT_SCREEN")
    private String optOutScreen;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
