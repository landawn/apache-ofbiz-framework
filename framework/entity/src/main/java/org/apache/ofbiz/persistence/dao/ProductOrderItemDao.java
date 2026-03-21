package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductOrderItemEntity;

public interface ProductOrderItemDao extends CrudDao<ProductOrderItemEntity, ProductOrderItemEntity, SqlBuilder.PSC, ProductOrderItemDao> {
}
