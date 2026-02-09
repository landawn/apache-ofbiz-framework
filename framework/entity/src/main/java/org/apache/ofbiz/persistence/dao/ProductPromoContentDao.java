package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoContentEntity;

public interface ProductPromoContentDao extends CrudDao<ProductPromoContentEntity, ProductPromoContentEntity, SQLBuilder.PSC, ProductPromoContentDao> {
}
