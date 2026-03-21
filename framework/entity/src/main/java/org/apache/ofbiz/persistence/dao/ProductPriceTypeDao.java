package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceTypeEntity;

public interface ProductPriceTypeDao extends CrudDao<ProductPriceTypeEntity, String, SqlBuilder.PSC, ProductPriceTypeDao> {
}
