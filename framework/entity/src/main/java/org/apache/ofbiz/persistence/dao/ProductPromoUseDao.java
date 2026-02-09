package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoUseEntity;

public interface ProductPromoUseDao extends CrudDao<ProductPromoUseEntity, ProductPromoUseEntity, SQLBuilder.PSC, ProductPromoUseDao> {
}
