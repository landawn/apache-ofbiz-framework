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
@Entity(name = "WEB_PAGE")
@Table(name = "WEB_PAGE")
public class WebPageEntity {
    @Id
    @Column(name = "WEB_PAGE_ID")
    private String webPageId;

    @Column(name = "PAGE_NAME")
    private String pageName;

    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Column(name = "CONTENT_ID")
    private String contentId;
}
