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
@Entity(name = "PROD_CATALOG")
@Table(name = "PROD_CATALOG")
public class ProdCatalogEntity {
    @Id
    @Column(name = "PROD_CATALOG_ID")
    private String prodCatalogId;

    @Column(name = "CATALOG_NAME")
    private String catalogName;

    @Column(name = "USE_QUICK_ADD")
    private String useQuickAdd;

    @Column(name = "STYLE_SHEET")
    private String styleSheet;

    @Column(name = "HEADER_LOGO")
    private String headerLogo;

    @Column(name = "CONTENT_PATH_PREFIX")
    private String contentPathPrefix;

    @Column(name = "TEMPLATE_PATH_PREFIX")
    private String templatePathPrefix;

    @Column(name = "VIEW_ALLOW_PERM_REQD")
    private String viewAllowPermReqd;

    @Column(name = "PURCHASE_ALLOW_PERM_REQD")
    private String purchaseAllowPermReqd;
}
