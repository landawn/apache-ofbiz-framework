package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureEntity;

public interface ProductFeatureDao extends CrudDao<ProductFeatureEntity, String, SqlBuilder.PSC, ProductFeatureDao>, DelegatorQueryDao {
}
