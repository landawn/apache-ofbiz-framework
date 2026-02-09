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
@Entity(name = "WEB_SITE_CONTACT_LIST")
@Table(name = "WEB_SITE_CONTACT_LIST")
public class WebSiteContactListEntity {
    @Id
    @Column(name = "WEB_SITE_ID")
    private String webSiteId;

    @Id
    @Column(name = "CONTACT_LIST_ID")
    private String contactListId;

    @Id
    @Column(name = "FROM_DATE")
    private Timestamp fromDate;

    @Column(name = "THRU_DATE")
    private Timestamp thruDate;
}
