package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPricePurposeEntity;

public interface ProductPricePurposeDao extends CrudDao<ProductPricePurposeEntity, String, SqlBuilder.PSC, ProductPricePurposeDao> {
}
