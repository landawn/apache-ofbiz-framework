package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceActionTypeEntity;

public interface ProductPriceActionTypeDao extends CrudDao<ProductPriceActionTypeEntity, String, SqlBuilder.PSC, ProductPriceActionTypeDao> {
}
