package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoEntity;

public interface ProductPromoDao extends CrudDao<ProductPromoEntity, String, SqlBuilder.PSC, ProductPromoDao> {
}
