package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupRoleEntity;

public interface ProductStoreGroupRoleDao extends CrudDao<ProductStoreGroupRoleEntity, ProductStoreGroupRoleEntity, SqlBuilder.PSC, ProductStoreGroupRoleDao> {
}
