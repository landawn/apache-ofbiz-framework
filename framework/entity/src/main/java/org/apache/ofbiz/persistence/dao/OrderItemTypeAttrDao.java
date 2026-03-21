package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemTypeAttrEntity;

public interface OrderItemTypeAttrDao extends CrudDao<OrderItemTypeAttrEntity, OrderItemTypeAttrEntity, SqlBuilder.PSC, OrderItemTypeAttrDao> {
}
