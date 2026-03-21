package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreEntity;

public interface ProductStoreDao extends CrudDao<ProductStoreEntity, String, SqlBuilder.PSC, ProductStoreDao>, DelegatorQueryDao {
}
