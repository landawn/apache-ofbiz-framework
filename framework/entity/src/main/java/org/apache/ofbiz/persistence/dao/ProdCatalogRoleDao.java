package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProdCatalogRoleEntity;

public interface ProdCatalogRoleDao extends CrudDao<ProdCatalogRoleEntity, ProdCatalogRoleEntity, SqlBuilder.PSC, ProdCatalogRoleDao> {
}
