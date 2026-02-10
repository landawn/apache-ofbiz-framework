package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplEntity;

public interface ProductFeatureApplDao
        extends CrudDao<ProductFeatureApplEntity, ProductFeatureApplEntity, SQLBuilder.PSC, ProductFeatureApplDao>, DelegatorQueryDao {
}
