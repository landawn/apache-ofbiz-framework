package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFeaturePriceEntity;

public interface ProductFeaturePriceDao extends CrudDao<ProductFeaturePriceEntity, ProductFeaturePriceEntity, SqlBuilder.PSC, ProductFeaturePriceDao> {
}
