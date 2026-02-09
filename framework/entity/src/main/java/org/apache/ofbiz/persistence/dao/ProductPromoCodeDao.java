package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCodeEntity;

public interface ProductPromoCodeDao extends CrudDao<ProductPromoCodeEntity, String, SQLBuilder.PSC, ProductPromoCodeDao> {
}
