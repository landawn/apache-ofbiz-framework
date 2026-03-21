package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProdCatalogEntity;

public interface ProdCatalogDao extends CrudDao<ProdCatalogEntity, String, SqlBuilder.PSC, ProdCatalogDao> {
}
