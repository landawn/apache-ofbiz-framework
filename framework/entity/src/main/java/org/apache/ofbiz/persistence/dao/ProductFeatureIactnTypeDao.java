package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureIactnTypeEntity;

public interface ProductFeatureIactnTypeDao extends CrudDao<ProductFeatureIactnTypeEntity, String, SqlBuilder.PSC, ProductFeatureIactnTypeDao> {
}
