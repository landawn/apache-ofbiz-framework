package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCondEntity;

public interface ProductPromoCondDao extends CrudDao<ProductPromoCondEntity, ProductPromoCondEntity, SQLBuilder.PSC, ProductPromoCondDao> {
}
