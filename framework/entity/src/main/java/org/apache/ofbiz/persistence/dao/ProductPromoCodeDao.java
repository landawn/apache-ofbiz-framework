package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCodeEntity;

public interface ProductPromoCodeDao extends CrudDao<ProductPromoCodeEntity, String, SqlBuilder.PSC, ProductPromoCodeDao> {
}
