package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupTypeEntity;

public interface ProductStoreGroupTypeDao extends CrudDao<ProductStoreGroupTypeEntity, String, SqlBuilder.PSC, ProductStoreGroupTypeDao> {
}
