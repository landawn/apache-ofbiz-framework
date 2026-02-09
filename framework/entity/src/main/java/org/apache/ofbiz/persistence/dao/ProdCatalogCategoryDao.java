package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProdCatalogCategoryEntity;

public interface ProdCatalogCategoryDao extends CrudDao<ProdCatalogCategoryEntity, ProdCatalogCategoryEntity, SQLBuilder.PSC, ProdCatalogCategoryDao> {
}
