package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProdCatalogCategoryTypeEntity;

public interface ProdCatalogCategoryTypeDao extends CrudDao<ProdCatalogCategoryTypeEntity, String, SQLBuilder.PSC, ProdCatalogCategoryTypeDao> {
}
