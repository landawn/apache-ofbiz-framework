package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderItemTypeEntity;

public interface OrderItemTypeDao extends CrudDao<OrderItemTypeEntity, String, SQLBuilder.PSC, OrderItemTypeDao> {
}
