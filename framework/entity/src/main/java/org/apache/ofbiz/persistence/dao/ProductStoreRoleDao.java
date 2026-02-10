package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreRoleEntity;

public interface ProductStoreRoleDao extends CrudDao<ProductStoreRoleEntity, ProductStoreRoleEntity, SQLBuilder.PSC, ProductStoreRoleDao>, DelegatorQueryDao {
}
