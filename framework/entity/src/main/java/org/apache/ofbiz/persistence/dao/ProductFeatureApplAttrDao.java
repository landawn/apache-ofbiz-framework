package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureApplAttrEntity;

public interface ProductFeatureApplAttrDao extends CrudDao<ProductFeatureApplAttrEntity, ProductFeatureApplAttrEntity, SqlBuilder.PSC, ProductFeatureApplAttrDao> {
}
