package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProdCatalogCategoryEntity;

public interface ProdCatalogCategoryDao extends CrudDao<ProdCatalogCategoryEntity, ProdCatalogCategoryEntity, SqlBuilder.PSC, ProdCatalogCategoryDao> {
}
