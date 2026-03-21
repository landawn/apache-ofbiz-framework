package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureCategoryApplEntity;

public interface ProductFeatureCategoryApplDao extends CrudDao<ProductFeatureCategoryApplEntity, ProductFeatureCategoryApplEntity, SqlBuilder.PSC, ProductFeatureCategoryApplDao> {
}
