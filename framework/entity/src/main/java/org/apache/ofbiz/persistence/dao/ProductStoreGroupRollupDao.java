package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupRollupEntity;

public interface ProductStoreGroupRollupDao extends CrudDao<ProductStoreGroupRollupEntity, ProductStoreGroupRollupEntity, SQLBuilder.PSC, ProductStoreGroupRollupDao> {
}
