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
@Entity(name = "PORTLET_PORTLET_CATEGORY")
@Table(name = "PORTLET_PORTLET_CATEGORY")
public class PortletPortletCategoryEntity {
    @Id
    @Column(name = "PORTAL_PORTLET_ID")
    private String portalPortletId;

    @Id
    @Column(name = "PORTLET_CATEGORY_ID")
    private String portletCategoryId;
}
