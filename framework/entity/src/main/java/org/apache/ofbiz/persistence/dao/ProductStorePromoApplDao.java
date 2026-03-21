package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStorePromoApplEntity;

public interface ProductStorePromoApplDao extends CrudDao<ProductStorePromoApplEntity, ProductStorePromoApplEntity, SqlBuilder.PSC, ProductStorePromoApplDao> {
}
