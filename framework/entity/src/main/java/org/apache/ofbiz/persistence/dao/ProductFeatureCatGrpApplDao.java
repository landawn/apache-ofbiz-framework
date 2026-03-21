package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureCatGrpApplEntity;

public interface ProductFeatureCatGrpApplDao
        extends CrudDao<ProductFeatureCatGrpApplEntity, ProductFeatureCatGrpApplEntity, SqlBuilder.PSC, ProductFeatureCatGrpApplDao>,
        DelegatorQueryDao {
}
