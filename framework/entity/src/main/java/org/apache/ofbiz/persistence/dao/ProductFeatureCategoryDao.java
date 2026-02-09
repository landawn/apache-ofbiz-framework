package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureCategoryEntity;

public interface ProductFeatureCategoryDao extends CrudDao<ProductFeatureCategoryEntity, String, SQLBuilder.PSC, ProductFeatureCategoryDao> {
}
