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
@Entity(name = "PORTAL_PORTLET")
@Table(name = "PORTAL_PORTLET")
public class PortalPortletEntity {
    @Id
    @Column(name = "PORTAL_PORTLET_ID")
    private String portalPortletId;

    @Column(name = "PORTLET_NAME")
    private String portletName;

    @Column(name = "SCREEN_NAME")
    private String screenName;

    @Column(name = "SCREEN_LOCATION")
    private String screenLocation;

    @Column(name = "EDIT_FORM_NAME")
    private String editFormName;

    @Column(name = "EDIT_FORM_LOCATION")
    private String editFormLocation;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SCREENSHOT")
    private String screenshot;

    @Column(name = "SECURITY_SERVICE_NAME")
    private String securityServiceName;

    @Column(name = "SECURITY_MAIN_ACTION")
    private String securityMainAction;
}
