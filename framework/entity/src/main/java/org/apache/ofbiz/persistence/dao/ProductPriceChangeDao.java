package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceChangeEntity;

public interface ProductPriceChangeDao extends CrudDao<ProductPriceChangeEntity, String, SqlBuilder.PSC, ProductPriceChangeDao> {
}
