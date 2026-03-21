package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupEntity;

public interface ProductStoreGroupDao extends CrudDao<ProductStoreGroupEntity, String, SqlBuilder.PSC, ProductStoreGroupDao> {
}
