package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureCategoryEntity;

public interface ProductFeatureCategoryDao extends CrudDao<ProductFeatureCategoryEntity, String, SqlBuilder.PSC, ProductFeatureCategoryDao> {
}
