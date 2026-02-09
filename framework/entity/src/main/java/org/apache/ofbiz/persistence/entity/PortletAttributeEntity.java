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
@Entity(name = "PORTLET_ATTRIBUTE")
@Table(name = "PORTLET_ATTRIBUTE")
public class PortletAttributeEntity {
    @Id
    @Column(name = "PORTAL_PAGE_ID")
    private String portalPageId;

    @Id
    @Column(name = "PORTAL_PORTLET_ID")
    private String portalPortletId;

    @Id
    @Column(name = "PORTLET_SEQ_ID")
    private String portletSeqId;

    @Id
    @Column(name = "ATTR_NAME")
    private String attrName;

    @Column(name = "ATTR_VALUE")
    private String attrValue;

    @Column(name = "ATTR_DESCRIPTION")
    private String attrDescription;

    @Column(name = "ATTR_TYPE")
    private String attrType;
}
