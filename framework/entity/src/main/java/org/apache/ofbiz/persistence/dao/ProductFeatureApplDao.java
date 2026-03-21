package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplEntity;

public interface ProductFeatureApplDao
        extends CrudDao<ProductFeatureApplEntity, ProductFeatureApplEntity, SqlBuilder.PSC, ProductFeatureApplDao>, DelegatorQueryDao {
}
