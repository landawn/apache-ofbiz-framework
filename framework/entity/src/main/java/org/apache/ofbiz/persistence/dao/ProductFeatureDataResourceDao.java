package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeatureDataResourceEntity;

public interface ProductFeatureDataResourceDao extends CrudDao<ProductFeatureDataResourceEntity, ProductFeatureDataResourceEntity, SqlBuilder.PSC, ProductFeatureDataResourceDao> {
}
