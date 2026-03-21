package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureGroupApplEntity;

public interface ProductFeatureGroupApplDao
        extends CrudDao<ProductFeatureGroupApplEntity, ProductFeatureGroupApplEntity, SqlBuilder.PSC, ProductFeatureGroupApplDao>,
        DelegatorQueryDao {
}
