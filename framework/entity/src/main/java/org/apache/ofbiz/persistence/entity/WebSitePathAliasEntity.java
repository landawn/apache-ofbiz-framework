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
@Entity(name = "WEB_SITE_PATH_ALIAS")
@Table(name = "WEB_SITE_PATH_ALIAS")
public class WebSitePathAliasEntity {
    @Id
    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Id
    @Column(name = "PATH_ALIAS")
    private String pathAlias;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;

    @Column(name = "ALIAS_TO")
    private String aliasTo;

    @Column(name = "CONTENT_ID")
    private String contentId;

    @Column(name = "MAP_KEY")
    private String mapKey;
}
