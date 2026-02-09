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
@Entity(name = "WEB_SITE")
@Table(name = "WEB_SITE")
public class WebSiteEntity {
    @Id
    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Column(name = "SITE_NAME")
    private String siteName;

    @Column(name = "HTTP_HOST")
    private String httpHost;

    @Column(name = "HTTP_PORT")
    private String httpPort;

    @Column(name = "HTTPS_HOST")
    private String httpsHost;

    @Column(name = "HTTPS_PORT")
    private String httpsPort;

    @Column(name = "ENABLE_HTTPS")
    private String enableHttps;

    @Column(name = "WEBAPP_PATH")
    private String webappPath;

    @Column(name = "STANDARD_CONTENT_PREFIX")
    private String standardContentPrefix;

    @Column(name = "SECURE_CONTENT_PREFIX")
    private String secureContentPrefix;

    @Column(name = "COOKIE_DOMAIN")
    private String cookieDomain;

    @Column(name = "VISUAL_THEME_SET_ID")
    private String visualThemeSetId;

    @Column(name = "PRODUCT_STORE_ID")
    private String productStoreId;

    @Column(name = "ALLOW_PRODUCT_STORE_CHANGE")
    private String allowProductStoreChange;

    @Column(name = "HOSTED_PATH_ALIAS")
    private String hostedPathAlias;

    @Column(name = "IS_DEFAULT")
    private String isDefault;

    @Column(name = "DISPLAY_MAINTENANCE_PAGE")
    private String displayMaintenancePage;
}
