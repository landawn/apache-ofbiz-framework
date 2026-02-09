package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureCategoryApplEntity;

public interface ProductFeatureCategoryApplDao extends CrudDao<ProductFeatureCategoryApplEntity, ProductFeatureCategoryApplEntity, SQLBuilder.PSC, ProductFeatureCategoryApplDao> {
}
