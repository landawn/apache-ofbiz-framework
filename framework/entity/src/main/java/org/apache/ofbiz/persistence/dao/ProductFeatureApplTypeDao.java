package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplTypeEntity;

public interface ProductFeatureApplTypeDao extends CrudDao<ProductFeatureApplTypeEntity, String, SQLBuilder.PSC, ProductFeatureApplTypeDao> {
}
