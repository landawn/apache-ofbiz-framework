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
@Entity(name = "WEB_SITE_PUBLISH_POINT")
@Table(name = "WEB_SITE_PUBLISH_POINT")
public class WebSitePublishPointEntity {
    @Id
    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "TEMPLATE_TITLE")
    private String templateTitle;

    @Column(name = "STYLE_SHEET_FILE")
    private String styleSheetFile;

    @Column(name = "LOGO")
    private String logo;

    @Column(name = "MEDALLION_LOGO")
    private String medallionLogo;

    @Column(name = "LINE_LOGO")
    private String lineLogo;

    @Column(name = "LEFT_BAR_ID")
    private String leftBarId;

    @Column(name = "RIGHT_BAR_ID")
    private String rightBarId;

    @Column(name = "CONTENT_DEPT")
    private String contentDept;

    @Column(name = "ABOUT_CONTENT_ID")
    private String aboutContentId;
}
