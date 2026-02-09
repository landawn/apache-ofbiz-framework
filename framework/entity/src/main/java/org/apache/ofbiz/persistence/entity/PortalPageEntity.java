package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PORTAL_PAGE")
@Table(name = "PORTAL_PAGE")
public class PortalPageEntity {
    @Id
    @Column(name = "PORTAL_PAGE_ID")
    private String portalPageId;

    @Column(name = "PORTAL_PAGE_NAME")
    private String portalPageName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "OWNER_USER_LOGIN_ID")
    private String ownerUserLoginId;

    @Column(name = "ORIGINAL_PORTAL_PAGE_ID")
    private String originalPortalPageId;

    @Column(name = "PARENT_PORTAL_PAGE_ID")
    private String parentPortalPageId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;

    @Column(name = "SECURITY_GROUP_ID")
    private String securityGroupId;

    @Column(name = "HELP_CONTENT_ID")
    private String helpContentId;
}
