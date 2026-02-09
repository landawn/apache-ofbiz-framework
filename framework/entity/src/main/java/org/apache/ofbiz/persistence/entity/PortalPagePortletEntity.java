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
@Entity(name = "PORTAL_PAGE_PORTLET")
@Table(name = "PORTAL_PAGE_PORTLET")
public class PortalPagePortletEntity {
    @Id
    @Column(name = "PORTAL_PAGE_ID")
    private String portalPageId;

    @Id
    @Column(name = "PORTAL_PORTLET_ID")
    private String portalPortletId;

    @Id
    @Column(name = "PORTLET_SEQ_ID")
    private String portletSeqId;

    @Column(name = "COLUMN_SEQ_ID")
    private String columnSeqId;

    @Column(name = "SEQUENCE_NUM")
    private BigDecimal sequenceNum;
}
