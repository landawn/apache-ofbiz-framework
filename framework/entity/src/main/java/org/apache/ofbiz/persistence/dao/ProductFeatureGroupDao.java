package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureGroupEntity;

public interface ProductFeatureGroupDao extends CrudDao<ProductFeatureGroupEntity, String, SQLBuilder.PSC, ProductFeatureGroupDao>,
        DelegatorQueryDao {
}
