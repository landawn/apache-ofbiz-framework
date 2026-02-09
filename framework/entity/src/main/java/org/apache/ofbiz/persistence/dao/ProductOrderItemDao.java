package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductOrderItemEntity;

public interface ProductOrderItemDao extends CrudDao<ProductOrderItemEntity, ProductOrderItemEntity, SQLBuilder.PSC, ProductOrderItemDao> {
}
