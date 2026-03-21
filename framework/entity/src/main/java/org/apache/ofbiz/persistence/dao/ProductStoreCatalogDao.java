package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreCatalogEntity;

public interface ProductStoreCatalogDao extends CrudDao<ProductStoreCatalogEntity, ProductStoreCatalogEntity, SqlBuilder.PSC, ProductStoreCatalogDao> {
}
