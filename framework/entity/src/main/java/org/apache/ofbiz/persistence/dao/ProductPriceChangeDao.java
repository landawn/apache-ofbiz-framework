package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceChangeEntity;

public interface ProductPriceChangeDao extends CrudDao<ProductPriceChangeEntity, String, SQLBuilder.PSC, ProductPriceChangeDao> {
}
