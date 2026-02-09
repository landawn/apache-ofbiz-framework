package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoActionEntity;

public interface ProductPromoActionDao extends CrudDao<ProductPromoActionEntity, ProductPromoActionEntity, SQLBuilder.PSC, ProductPromoActionDao> {
}
