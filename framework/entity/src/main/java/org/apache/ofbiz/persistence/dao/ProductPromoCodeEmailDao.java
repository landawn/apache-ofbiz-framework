package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCodeEmailEntity;

public interface ProductPromoCodeEmailDao extends CrudDao<ProductPromoCodeEmailEntity, ProductPromoCodeEmailEntity, SqlBuilder.PSC, ProductPromoCodeEmailDao> {
}
