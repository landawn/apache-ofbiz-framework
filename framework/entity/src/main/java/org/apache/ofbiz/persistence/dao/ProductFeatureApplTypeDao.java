package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplTypeEntity;

public interface ProductFeatureApplTypeDao extends CrudDao<ProductFeatureApplTypeEntity, String, SqlBuilder.PSC, ProductFeatureApplTypeDao> {
}
