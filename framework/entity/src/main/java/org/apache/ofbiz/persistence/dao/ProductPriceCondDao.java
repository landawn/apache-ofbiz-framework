package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceCondEntity;

public interface ProductPriceCondDao extends CrudDao<ProductPriceCondEntity, ProductPriceCondEntity, SQLBuilder.PSC, ProductPriceCondDao> {
}
