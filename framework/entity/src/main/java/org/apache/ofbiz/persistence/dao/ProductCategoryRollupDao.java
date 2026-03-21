package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryRollupEntity;

public interface ProductCategoryRollupDao
        extends CrudDao<ProductCategoryRollupEntity, ProductCategoryRollupEntity, SqlBuilder.PSC, ProductCategoryRollupDao>, DelegatorQueryDao {
}
